package com.owlcoder.animeschedule.data.repository

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.mal.MalApiService
import com.owlcoder.animeschedule.data.api.mal.auth.MalAuthManager
import com.owlcoder.animeschedule.data.api.mal.dto.MalAnimeListResponse
import com.owlcoder.animeschedule.data.api.mal.dto.MalAnimeNode
import com.owlcoder.animeschedule.data.api.mal.dto.MalListStatus
import com.owlcoder.animeschedule.data.api.mal.dto.MalPaging
import com.owlcoder.animeschedule.data.api.mal.dto.MalUserResponse
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryEntity
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateDao
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateEntity
import com.owlcoder.animeschedule.domain.model.UserPreferences
import com.owlcoder.animeschedule.domain.repository.WorkScheduler
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchStatus
import java.io.IOException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaTypeOrNull())))

private fun node(id: Int, title: String = "Anime $id") = MalAnimeNode(id = id, title = title)

private fun listResponse(vararg ids: Int, hasNext: Boolean = false) = MalAnimeListResponse(
    data = ids.map { com.owlcoder.animeschedule.data.api.mal.dto.MalAnimeListItem(node(it)) },
    paging = if (hasNext) MalPaging(next = "next") else null
)

private fun entity(animeId: Int, episodes: Int = 3, status: String = "watching") = MalListEntryEntity(
    animeId = animeId,
    malId = animeId,
    title = "Anime $animeId",
    coverImageUrl = null,
    totalEpisodes = 12,
    status = status,
    numEpisodesWatched = episodes,
    score = 7,
    updatedAt = null
)

private class FakeMalApiService : MalApiService {
    var onGetUserAnimeList: suspend (offset: Int) -> MalAnimeListResponse = { listResponse() }
    var onUpdateListStatus: suspend (animeId: Int) -> MalListStatus =
        { MalListStatus(status = "watching", numEpisodesWatched = 0, score = 0) }
    var onDeleteListStatus: suspend (animeId: Int) -> Response<Unit> = { Response.success(Unit) }
    var onGetAnimeDetail: suspend (malId: Int) -> MalAnimeNode = { node(it) }

    val updateCalls = mutableListOf<Int>()
    val payloads = mutableListOf<MalListUpdate>()
    val deleteCalls = mutableListOf<Int>()

    override suspend fun getUserAnimeList(fields: String, limit: Int, offset: Int, nsfw: Boolean) =
        onGetUserAnimeList(offset)

    override suspend fun updateListStatus(animeId: Int, status: String?, numWatchedEpisodes: Int?, score: Int?): MalListStatus {
        updateCalls += animeId
        payloads += MalListUpdate(WatchStatus.entries.firstOrNull { it.malValue == status }, numWatchedEpisodes, score)
        return onUpdateListStatus(animeId)
    }

    override suspend fun deleteListStatus(animeId: Int): Response<Unit> {
        deleteCalls += animeId
        return onDeleteListStatus(animeId)
    }

    override suspend fun getAnimeDetail(malId: Int, fields: String) = onGetAnimeDetail(malId)

    override suspend fun getMe(fields: String) = MalUserResponse(1, "tester", null)
}

private class FakeMalListEntryDao : MalListEntryDao {
    val rows = linkedMapOf<Int, MalListEntryEntity>()
    private val flow = MutableStateFlow<List<MalListEntryEntity>>(emptyList())
    private fun emit() { flow.value = rows.values.toList() }

    override fun getAll(): Flow<List<MalListEntryEntity>> = flow
    override suspend fun getByAnimeId(animeId: Int) = rows[animeId]
    override fun observeByAnimeId(animeId: Int) = flow.map { rows[animeId] }
    override fun observeByMalId(malId: Int) = flow.map { list -> list.find { it.malId == malId } }
    override suspend fun upsert(entity: MalListEntryEntity) { rows[entity.animeId] = entity; emit() }
    override suspend fun upsertAll(entities: List<MalListEntryEntity>) {
        entities.forEach { rows[it.animeId] = it }; emit()
    }
    override suspend fun deleteByAnimeId(animeId: Int) { rows.remove(animeId); emit() }
    override suspend fun deleteAll() { rows.clear(); emit() }
}

private class FakePendingListUpdateDao : PendingListUpdateDao {
    val rows = linkedMapOf<Int, PendingListUpdateEntity>()
    private val flow = MutableStateFlow<List<PendingListUpdateEntity>>(emptyList())
    override fun observeAll() = flow
    private fun emit() { flow.value = rows.values.toList() }
    override suspend fun getAll() = rows.values.toList()
    override suspend fun getByAnimeId(animeId: Int) = rows[animeId]
    override suspend fun upsert(entity: PendingListUpdateEntity) { rows[entity.animeId] = entity; emit() }
    override suspend fun deleteByAnimeId(animeId: Int) { rows.remove(animeId); emit() }
    override suspend fun deleteAll() { rows.clear(); emit() }
}

private class FakeScheduler : WorkScheduler {
    var flushRequests = 0
    override fun scheduleFlushPendingUpdates() { flushRequests++ }
    override fun checkAiringNotifications() = Unit
}

class MalRepositoryImplTest {

    private lateinit var api: FakeMalApiService
    private lateinit var listDao: FakeMalListEntryDao
    private lateinit var pendingDao: FakePendingListUpdateDao
    private lateinit var scheduler: FakeScheduler
    private lateinit var authManager: MalAuthManager
    private lateinit var prefs: UserPreferencesDataStore
    private lateinit var detailDao: AnimeDetailDao
    private lateinit var repository: MalRepositoryImpl

    @Before
    fun setUp() {
        api = FakeMalApiService()
        listDao = FakeMalListEntryDao()
        pendingDao = FakePendingListUpdateDao()
        scheduler = FakeScheduler()
        authManager = mockk {
            coEvery { ensureFreshToken() } just Runs
            coEvery { refreshAccessToken(any()) } returns MalAuthManager.RefreshResult.REFRESHED
        }
        prefs = mockk(relaxed = true) {
            coEvery { getLastMalListSyncEpochMs() } returns 0L
            every { userPreferencesFlow } returns flowOf(UserPreferences(malLoggedIn = true))
            every { lastMalSyncSuccess } returns flowOf(123L)
        }
        detailDao = mockk {
            coEvery { getByMalId(any()) } returns null
        }
        repository = MalRepositoryImpl(
            malApiService = api,
            malListEntryDao = listDao,
            pendingListUpdateDao = pendingDao,
            animeDetailDao = detailDao,
            malAuthManager = authManager,
            prefsDataStore = prefs,
            workScheduler = scheduler
        )
    }

    // --- updateListEntry ---

    @Test
    fun `update of existing entry mirrors the PATCH response locally`() = runTest {
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { MalListStatus(status = "completed", numEpisodesWatched = 12, score = 9) }

        val result = repository.updateListEntry(10, MalListUpdate(status = WatchStatus.COMPLETED))

        assertTrue(result is AppResult.Success)
        val row = listDao.rows[10]!!
        assertEquals("completed", row.status)
        assertEquals(12, row.numEpisodesWatched)
        assertEquals(9, row.score)
        assertNotNull(row.updatedAt)
    }

    @Test
    fun `first add inserts a local row with fetched metadata`() = runTest {
        api.onUpdateListStatus = { MalListStatus(status = "plan_to_watch", numEpisodesWatched = 0, score = 0) }
        api.onGetAnimeDetail = { MalAnimeNode(id = it, title = "Fetched Title", numEpisodes = 24) }

        val result = repository.updateListEntry(55, MalListUpdate(status = WatchStatus.PLAN_TO_WATCH))

        assertTrue(result is AppResult.Success)
        val row = listDao.rows[55]!!
        assertEquals("Fetched Title", row.title)
        assertEquals(24, row.totalEpisodes)
        assertEquals("plan_to_watch", row.status)
    }

    // --- 401 handling ---

    @Test
    fun `401 with successful token refresh retries and succeeds`() = runTest {
        listDao.rows[10] = entity(10)
        var calls = 0
        api.onUpdateListStatus = {
            calls++
            if (calls == 1) throw httpException(401)
            MalListStatus(status = "watching", numEpisodesWatched = 4, score = 7)
        }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        assertTrue(result is AppResult.Success)
        assertEquals(2, calls)
        assertEquals(4, listDao.rows[10]!!.numEpisodesWatched)
    }

    @Test
    fun `invalid refresh token logs the user out`() = runTest {
        listDao.rows[10] = entity(10)
        coEvery { authManager.refreshAccessToken(any()) } returns MalAuthManager.RefreshResult.INVALID
        api.onUpdateListStatus = { throw httpException(401) }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        assertTrue(result is AppResult.Error && (result as AppResult.Error).error is AppError.Unauthorized)
        coVerify { prefs.expireMalSession() }
    }

    @Test
    fun `transient refresh failure does not log the user out and queues the edit`() = runTest {
        listDao.rows[10] = entity(10)
        coEvery { authManager.refreshAccessToken(any()) } returns MalAuthManager.RefreshResult.TRANSIENT
        api.onUpdateListStatus = { throw httpException(401) }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        // Treated like connectivity trouble: the edit lands in the offline queue (flushed
        // later with a fresh token) instead of being lost — and the session survives.
        assertTrue(result is AppResult.Success)
        assertEquals(4, pendingDao.rows[10]!!.numWatchedEpisodes)
        coVerify(exactly = 0) { prefs.expireMalSession() }
    }

    // --- offline queue ---

    @Test
    fun `network failure queues the update, applies it locally and reports success`() = runTest {
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { throw IOException("offline") }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        assertTrue(result is AppResult.Success)
        assertEquals(4, listDao.rows[10]!!.numEpisodesWatched)
        assertEquals(4, pendingDao.rows[10]!!.numWatchedEpisodes)
        assertEquals(1, scheduler.flushRequests)
    }

    @Test
    fun `http 4xx is reported as an error and is not queued`() = runTest {
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { throw httpException(400) }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        assertTrue(result is AppResult.Error)
        assertTrue(pendingDao.rows.isEmpty())
        assertEquals(3, listDao.rows[10]!!.numEpisodesWatched)
    }

    @Test
    fun `flushPendingUpdates pushes queued edits and empties the queue`() = runTest {
        pendingDao.rows[10] = PendingListUpdateEntity(10, "watching", 5, null, isRemoval = false, queuedAtEpochMs = 1L)
        pendingDao.rows[20] = PendingListUpdateEntity(20, null, null, null, isRemoval = true, queuedAtEpochMs = 2L)
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { MalListStatus(status = "watching", numEpisodesWatched = 5, score = 7) }

        val flushed = repository.flushPendingUpdates()

        assertTrue(flushed)
        assertTrue(pendingDao.rows.isEmpty())
        assertEquals(listOf(10), api.updateCalls)
        assertEquals(listOf(20), api.deleteCalls)
        assertEquals(5, listDao.rows[10]!!.numEpisodesWatched)
    }

    // --- removeListEntry ---

    @Test
    fun `remove deletes remotely and locally`() = runTest {
        listDao.rows[10] = entity(10)

        val result = repository.removeListEntry(10)

        assertTrue(result is AppResult.Success)
        assertNull(listDao.rows[10])
        assertEquals(listOf(10), api.deleteCalls)
    }

    @Test
    fun `remove tolerates 404 as already gone`() = runTest {
        listDao.rows[10] = entity(10)
        api.onDeleteListStatus = {
            Response.error(404, "{}".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val result = repository.removeListEntry(10)

        assertTrue(result is AppResult.Success)
        assertNull(listDao.rows[10])
    }

    @Test
    fun `offline remove is queued and hidden locally`() = runTest {
        listDao.rows[10] = entity(10)
        api.onDeleteListStatus = { throw IOException("offline") }

        val result = repository.removeListEntry(10)

        assertTrue(result is AppResult.Success)
        assertNull(listDao.rows[10])
        assertTrue(pendingDao.rows[10]!!.isRemoval)
        assertEquals(1, scheduler.flushRequests)
    }

    // --- refreshUserList ---

    @Test
    fun `failed page aborts the sync and keeps the local list intact`() = runTest {
        listDao.rows[1] = entity(1)
        listDao.rows[2] = entity(2)
        api.onGetUserAnimeList = { offset ->
            if (offset == 0) listResponse(1, hasNext = true) else throw IOException("offline")
        }

        val synced = repository.refreshUserList(force = true)

        assertFalse(synced)
        assertEquals(setOf(1, 2), listDao.rows.keys)
    }

    @Test
    fun `successful empty sync clears local rows so remote deletions propagate`() = runTest {
        listDao.rows[1] = entity(1)
        api.onGetUserAnimeList = { listResponse() }

        val synced = repository.refreshUserList(force = true)

        assertTrue(synced)
        assertTrue(listDao.rows.isEmpty())
        coVerify { prefs.setLastMalListSyncEpochMs(any()) }
    }

    @Test
    fun `sync preserves rows queued as offline adds`() = runTest {
        // 99 was added while offline: local row + pending update, unknown to the server.
        listDao.rows[99] = entity(99, status = "plan_to_watch")
        pendingDao.rows[99] = PendingListUpdateEntity(99, "plan_to_watch", 0, null, isRemoval = false, queuedAtEpochMs = 1L)
        // Flushing inside refresh fails (still offline for the PATCH), pages succeed.
        api.onUpdateListStatus = { throw IOException("offline") }
        api.onGetUserAnimeList = { listResponse(1) }

        val synced = repository.refreshUserList(force = true)

        assertTrue(synced)
        assertEquals(setOf(1, 99), listDao.rows.keys)
    }

    @Test
    fun `fresh cache skips the sync entirely`() = runTest {
        coEvery { prefs.getLastMalListSyncEpochMs() } returns System.currentTimeMillis()
        var apiCalls = 0
        api.onGetUserAnimeList = { apiCalls++; listResponse(1) }

        val synced = repository.refreshUserList(force = false)

        assertTrue(synced)
        assertEquals(0, apiCalls)
    }

    // --- error classification / offline queue ---

    @Test
    fun `server errors are transient so the edit is queued and the session survives`() = runTest {
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { throw httpException(503) }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        assertTrue(result is AppResult.Success)
        assertEquals(4, pendingDao.rows[10]!!.numWatchedEpisodes)
        assertEquals(1, scheduler.flushRequests)
        coVerify(exactly = 0) { prefs.expireMalSession() }
    }

    @Test
    fun `rate limiting is transient and queued`() = runTest {
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { throw httpException(429) }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        assertTrue(result is AppResult.Success)
        assertTrue(pendingDao.rows.containsKey(10))
    }

    @Test
    fun `flush retains rejected edits visibly and does not retry them automatically`() = runTest {
        pendingDao.rows[10] = PendingListUpdateEntity(10, "watching", 99, null, isRemoval = false, queuedAtEpochMs = 1L)
        pendingDao.rows[20] = PendingListUpdateEntity(20, "watching", 5, null, isRemoval = false, queuedAtEpochMs = 2L)
        api.onUpdateListStatus = { id ->
            if (id == 10) throw httpException(400)
            MalListStatus(status = "watching", numEpisodesWatched = 5, score = 0)
        }

        val flushed = repository.flushPendingUpdates()

        assertFalse(flushed)
        assertEquals(setOf(10), pendingDao.rows.keys)
        assertTrue(pendingDao.rows[10]!!.rejected)
        assertEquals(listOf(10, 20), api.updateCalls)
        repository.flushPendingUpdates()
        assertEquals("rejected payload is not sent again automatically", listOf(10, 20), api.updateCalls)
    }

    @Test
    fun `flush keeps edits queued while the network is down`() = runTest {
        pendingDao.rows[10] = PendingListUpdateEntity(10, "watching", 5, null, isRemoval = false, queuedAtEpochMs = 1L)
        api.onUpdateListStatus = { throw IOException("offline") }

        val flushed = repository.flushPendingUpdates()

        assertFalse(flushed)
        assertTrue(pendingDao.rows.containsKey(10))
    }

    @Test
    fun `flush stops and keeps the queue when the session is dead`() = runTest {
        pendingDao.rows[10] = PendingListUpdateEntity(10, "watching", 5, null, isRemoval = false, queuedAtEpochMs = 1L)
        pendingDao.rows[20] = PendingListUpdateEntity(20, "watching", 6, null, isRemoval = false, queuedAtEpochMs = 2L)
        coEvery { authManager.refreshAccessToken(any()) } returns MalAuthManager.RefreshResult.INVALID
        api.onUpdateListStatus = { throw httpException(401) }

        val flushed = repository.flushPendingUpdates()

        assertFalse(flushed)
        assertEquals(setOf(10, 20), pendingDao.rows.keys)
        assertEquals("remaining rows are not hammered", listOf(10), api.updateCalls)
    }

    // --- incrementEpisode ---

    @Test
    fun `increment never goes past the known episode total`() = runTest {
        listDao.rows[10] = entity(10, episodes = 12).copy(totalEpisodes = 12)
        api.onUpdateListStatus = { MalListStatus(status = "completed", numEpisodesWatched = 12, score = 7) }
        var sentEpisodes: Int? = null
        val capturing = object : MalApiService by api {
            override suspend fun updateListStatus(animeId: Int, status: String?, numWatchedEpisodes: Int?, score: Int?): MalListStatus {
                sentEpisodes = numWatchedEpisodes
                return api.updateListStatus(animeId, status, numWatchedEpisodes, score)
            }
        }
        val capped = MalRepositoryImpl(capturing, listDao, pendingDao, detailDao, authManager, prefs, scheduler)

        capped.incrementEpisode(10)

        assertEquals(12, sentEpisodes)
    }

    @Test
    fun `increment on an unknown entry reports no cache`() = runTest {
        val result = repository.incrementEpisode(404)

        assertTrue(result is AppResult.Error && (result as AppResult.Error).error == AppError.NoCache)
    }

    // --- refreshUserList ---

    @Test
    fun `signed out users never hit the network for a list sync`() = runTest {
        every { prefs.userPreferencesFlow } returns flowOf(UserPreferences(malLoggedIn = false))
        var apiCalls = 0
        api.onGetUserAnimeList = { apiCalls++; listResponse(1) }

        val synced = repository.refreshUserList(force = true)

        assertTrue(synced)
        assertEquals(0, apiCalls)
        coVerify(exactly = 0) { authManager.ensureFreshToken() }
    }

    @Test
    fun `cancellation during a sync propagates instead of being reported as a failed sync`() = runTest {
        api.onGetUserAnimeList = { throw kotlinx.coroutines.CancellationException("screen closed") }

        var propagated = false
        try {
            repository.refreshUserList(force = true)
        } catch (_: kotlinx.coroutines.CancellationException) {
            propagated = true
        }

        assertTrue(propagated)
    }
    @Test fun `online progress is logged after local mirror and rejected edits are not logged`() = runTest {
        val tools = mockk<com.owlcoder.animeschedule.data.local.datastore.WatchToolsStore>(relaxed = true)
        val tracked = MalRepositoryImpl(api, listDao, pendingDao, detailDao, authManager, prefs, scheduler, tools)
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { MalListStatus(status = "watching", numEpisodesWatched = 8, score = 7) }
        tracked.updateListEntry(10, MalListUpdate(episodesWatched = 8))
        coVerify(exactly = 1) { tools.recordProgress(10, "Anime 10", 3, 8) }
        api.onUpdateListStatus = { throw httpException(400) }
        tracked.updateListEntry(10, MalListUpdate(episodesWatched = 9))
        coVerify(exactly = 1) { tools.recordProgress(any(), any(), any(), any()) }
    }

    @Test fun `offline progress is logged once and a later queue flush does not duplicate activity`() = runTest {
        val tools = mockk<com.owlcoder.animeschedule.data.local.datastore.WatchToolsStore>(relaxed = true)
        val tracked = MalRepositoryImpl(api, listDao, pendingDao, detailDao, authManager, prefs, scheduler, tools)
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { throw IOException("offline") }
        tracked.updateListEntry(10, MalListUpdate(episodesWatched = 4))
        coVerify(exactly = 1) { tools.recordProgress(10, "Anime 10", 3, 4) }
        api.onUpdateListStatus = { MalListStatus(status = "watching", numEpisodesWatched = 4, score = 7) }
        tracked.flushPendingUpdates()
        coVerify(exactly = 1) { tools.recordProgress(any(), any(), any(), any()) }
    }


    private fun echoCurrentValues() {
        api.onUpdateListStatus = { id ->
            val values = api.payloads.last()
            val old = listDao.rows[id] ?: entity(id, episodes = 0, status = "plan_to_watch")
            MalListStatus(values.status?.malValue ?: old.status, values.episodesWatched ?: old.numEpisodesWatched, values.score ?: old.score)
        }
    }

    @Test fun `an online edit delivers queued fields too and cannot leave a stale replay`() = runTest {
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { throw IOException("offline") }
        repository.updateListEntry(10, MalListUpdate(episodesWatched = 5, status = WatchStatus.ON_HOLD))
        echoCurrentValues()
        repository.updateListEntry(10, MalListUpdate(score = 9))
        assertEquals(MalListUpdate(WatchStatus.ON_HOLD, 5, 9), api.payloads.last())
        assertTrue(pendingDao.rows.isEmpty())
        repository.flushPendingUpdates()
        assertEquals(2, api.updateCalls.size)
        assertEquals(5, listDao.rows[10]!!.numEpisodesWatched)
    }

    @Test fun `undo restores progress status and score together and is consumed once`() = runTest {
        listDao.rows[10] = entity(10, episodes = 3)
        echoCurrentValues()
        repository.updateListEntry(10, MalListUpdate(WatchStatus.COMPLETED, 12, 9))
        val change = repository.undoChange.value!!
        assertTrue(repository.undoListChange(change.id) is AppResult.Success)
        assertEquals(MalListUpdate(WatchStatus.WATCHING, 3, 7), api.payloads.last())
        assertNull(repository.undoChange.value)
        assertTrue(repository.undoListChange(change.id) is AppResult.Error)
        assertEquals(2, api.updateCalls.size)
    }

    @Test fun `offline undo replaces the queued desired state rather than replaying plus one`() = runTest {
        listDao.rows[10] = entity(10, episodes = 3)
        api.onUpdateListStatus = { throw IOException("offline") }
        repository.incrementEpisode(10)
        val id = repository.undoChange.value!!.id
        assertTrue(repository.undoListChange(id) is AppResult.Success)
        assertEquals(3, listDao.rows[10]!!.numEpisodesWatched)
        assertEquals(3, pendingDao.rows[10]!!.numWatchedEpisodes)
        echoCurrentValues()
        assertTrue(repository.flushPendingUpdates())
        assertEquals(3, api.payloads.last().episodesWatched)
        assertTrue(pendingDao.rows.isEmpty())
    }

    @Test fun `an old undo cannot overwrite a newer edit or a remote change`() = runTest {
        listDao.rows[10] = entity(10)
        echoCurrentValues()
        repository.incrementEpisode(10)
        val old = repository.undoChange.value!!.id
        repository.updateListEntry(10, MalListUpdate(score = 9))
        assertTrue(repository.undoListChange(old) is AppResult.Error)
        val latest = repository.undoChange.value!!.id
        listDao.rows[10] = listDao.rows[10]!!.copy(numEpisodesWatched = 8)
        assertTrue(repository.undoListChange(latest) is AppResult.Error)
        assertEquals(8, listDao.rows[10]!!.numEpisodesWatched)
        assertEquals(9, listDao.rows[10]!!.score)
        assertEquals(2, api.updateCalls.size)
    }

    @Test fun `simultaneous increments preserve both intended steps`() = runTest {
        listDao.rows[10] = entity(10)
        echoCurrentValues()
        val jobs = (1..2).map { async { repository.incrementEpisode(10) } }
        jobs.forEach { assertTrue(it.await() is AppResult.Success) }
        assertEquals(listOf(4, 5), api.payloads.map { it.episodesWatched })
        assertEquals(5, listDao.rows[10]!!.numEpisodesWatched)
    }

    @Test fun `sync center counts coalesced edits and preserves rejected edits for manual retry`() = runTest {
        api.onUpdateListStatus = { throw IOException("offline") }
        listDao.rows[10] = entity(10); listDao.rows[20] = entity(20)
        repository.incrementEpisode(10); repository.updateListEntry(10, MalListUpdate(score = 9)); repository.incrementEpisode(20)
        assertEquals(2, repository.syncState.first().pendingCount)
        api.onUpdateListStatus = { throw httpException(400) }
        assertFalse(repository.flushPendingUpdates())
        assertEquals(2, repository.syncState.first().rejectedCount)
        val previousCalls = api.updateCalls.size
        repository.flushPendingUpdates()
        assertEquals(previousCalls, api.updateCalls.size)
        echoCurrentValues()
        api.onGetUserAnimeList = { listResponse(10, 20) }
        assertTrue(repository.retrySync())
        assertEquals(0, repository.syncState.first().pendingCount)
        assertEquals(0, repository.syncState.first().rejectedCount)
    }

    @Test fun `signed out updates cannot resurrect the list or send a patch`() = runTest {
        every { prefs.userPreferencesFlow } returns flowOf(UserPreferences(malLoggedIn = false))
        assertTrue(repository.updateListEntry(10, MalListUpdate(episodesWatched = 1)) is AppResult.Error)
        assertTrue(api.updateCalls.isEmpty())
        assertTrue(listDao.rows.isEmpty())
    }

}
