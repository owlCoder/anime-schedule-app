package com.owlcoder.animeschedule.data.repository

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
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
import com.owlcoder.animeschedule.data.work.PendingUpdateScheduler
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchStatus
import java.io.IOException

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
    val deleteCalls = mutableListOf<Int>()

    override suspend fun getUserAnimeList(fields: String, limit: Int, offset: Int, nsfw: Boolean) =
        onGetUserAnimeList(offset)

    override suspend fun updateListStatus(animeId: Int, status: String?, numWatchedEpisodes: Int?, score: Int?): MalListStatus {
        updateCalls += animeId
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
    override suspend fun getAll() = rows.values.toList()
    override suspend fun getByAnimeId(animeId: Int) = rows[animeId]
    override suspend fun upsert(entity: PendingListUpdateEntity) { rows[entity.animeId] = entity }
    override suspend fun deleteByAnimeId(animeId: Int) { rows.remove(animeId) }
    override suspend fun deleteAll() { rows.clear() }
}

private class FakeScheduler : PendingUpdateScheduler {
    var flushRequests = 0
    override fun scheduleFlush() { flushRequests++ }
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
            coEvery { refreshAccessToken() } returns MalAuthManager.RefreshResult.REFRESHED
        }
        prefs = mockk(relaxed = true) {
            coEvery { getLastMalListSyncEpochMs() } returns 0L
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
            pendingUpdateScheduler = scheduler
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
        coEvery { authManager.refreshAccessToken() } returns MalAuthManager.RefreshResult.INVALID
        api.onUpdateListStatus = { throw httpException(401) }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        assertTrue(result is AppResult.Error && (result as AppResult.Error).error is AppError.Unauthorized)
        coVerify { prefs.setMalLoggedIn(false, any(), any()) }
    }

    @Test
    fun `transient refresh failure does not log the user out and queues the edit`() = runTest {
        listDao.rows[10] = entity(10)
        coEvery { authManager.refreshAccessToken() } returns MalAuthManager.RefreshResult.TRANSIENT
        api.onUpdateListStatus = { throw httpException(401) }

        val result = repository.updateListEntry(10, MalListUpdate(episodesWatched = 4))

        // Treated like connectivity trouble: the edit lands in the offline queue (flushed
        // later with a fresh token) instead of being lost — and the session survives.
        assertTrue(result is AppResult.Success)
        assertEquals(4, pendingDao.rows[10]!!.numWatchedEpisodes)
        coVerify(exactly = 0) { prefs.setMalLoggedIn(false, any(), any()) }
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
}
