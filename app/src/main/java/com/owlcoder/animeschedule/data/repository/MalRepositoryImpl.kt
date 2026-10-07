package com.owlcoder.animeschedule.data.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.data.api.mal.auth.MalSession
import com.owlcoder.animeschedule.data.local.db.AnimeScheduleDatabase
import com.owlcoder.animeschedule.data.local.db.NotificationActionDao
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.mal.MalApiService
import com.owlcoder.animeschedule.data.api.mal.auth.MalAuthManager
import com.owlcoder.animeschedule.data.api.mal.dto.MalAnimeListResponse
import com.owlcoder.animeschedule.data.api.mal.dto.MalListStatus
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryEntity
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateDao
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateEntity
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.domain.repository.WorkScheduler
import javax.inject.Inject
import javax.inject.Singleton
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity

private const val MAL_LIST_CACHE_TTL_MS = 60 * 60 * 1000L // 1h

@Singleton
class MalRepositoryImpl @Inject constructor(
    private val malApiService: MalApiService,
    private val malListEntryDao: MalListEntryDao,
    private val pendingListUpdateDao: PendingListUpdateDao,
    private val animeDetailDao: AnimeDetailDao,
    private val malAuthManager: MalAuthManager,
    private val prefsDataStore: UserPreferencesDataStore,
    private val workScheduler: WorkScheduler,
    private val watchToolsStore: com.owlcoder.animeschedule.data.local.datastore.WatchToolsStore? = null,
    private val session: MalSession = MalSession(),
    private val database: AnimeScheduleDatabase? = null,
    private val notificationActions: NotificationActionDao? = null,
    private val network: com.owlcoder.animeschedule.data.network.NetworkMonitor? = null,
) : MalRepository {
    private val syncing = MutableStateFlow(false)
    private val failed = MutableStateFlow(false)
    private val undo = MutableStateFlow<UndoListChange?>(null)
    private var undoId = 0L
    override val undoChange = undo
    override val syncState = combine(
        pendingListUpdateDao.observeAll(), prefsDataStore.userPreferencesFlow,
        prefsDataStore.lastMalSyncSuccess, syncing, failed,
    ) { pending, prefs, lastSuccess, busy, error ->
        MalSyncState(prefs.malLoggedIn, pending.size, pending.count { it.rejected }, lastSuccess, busy, error)
    }

    private suspend fun <T> transaction(block: suspend () -> T): T =
        if (database == null) block() else database.withTransaction { block() }

    override fun getUserList(): Flow<List<MalListEntry>> =
        malListEntryDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateListEntry(animeId: Int, update: MalListUpdate): AppResult<Unit> =
        session.mutex.withLock { updateLocked(animeId, update) }

    private suspend fun updateLocked(animeId: Int, update: MalListUpdate, offerUndo: Boolean = true): AppResult<Unit> {
        if (!prefsDataStore.userPreferencesFlow.first().malLoggedIn) return AppResult.Error(AppError.Unauthorized)
        if (animeId <= 0 || update.episodesWatched?.let { it < 0 } == true || update.score?.let { it !in 0..10 } == true) return AppResult.Error(AppError.Unknown("Invalid list values"))
        val pending = pendingListUpdateDao.getByAnimeId(animeId)?.takeUnless { it.isRemoval }
        val merged = MalListUpdate(
            update.status ?: WatchStatus.entries.firstOrNull { it.malValue == pending?.status },
            update.episodesWatched ?: pending?.numWatchedEpisodes,
            update.score ?: pending?.score,
        )
        val previousEntry = malListEntryDao.getByAnimeId(animeId)
        val result = if (network?.isOnline == false) AppResult.Error(AppError.Network("offline")) else executeWithRefresh {
            val patched = malApiService.updateListStatus(
                animeId = animeId,
                status = merged.status?.malValue,
                numWatchedEpisodes = merged.episodesWatched,
                score = merged.score
            )
            transaction {
                applyPatchedLocally(animeId, patched)
                pendingListUpdateDao.deleteByAnimeId(animeId)
            }
            prefsDataStore.setLastMalSyncSuccess(System.currentTimeMillis())
        }
        // Couldn't reach MAL (offline / server down): apply the edit locally and queue it for
        // a background flush — the change is not lost, so report success to the UI.
        if (result is AppResult.Error && result.error is AppError.Network) {
            queueUpdate(animeId, update)
            recordActivity(animeId, previousEntry)
            if (offerUndo) offerUndo(animeId, previousEntry)
            return AppResult.Success(Unit)
        }
        if (result is AppResult.Success) {
            failed.value = false
            recordActivity(animeId, previousEntry)
            if (offerUndo) offerUndo(animeId, previousEntry)
        }
        return result
    }

    private fun MalListEntryEntity.values() = MalListUpdate(
        WatchStatus.entries.firstOrNull { it.malValue == status }, numEpisodesWatched, score,
    )

    private suspend fun offerUndo(animeId: Int, before: MalListEntryEntity?) {
        val after = malListEntryDao.getByAnimeId(animeId)
        undo.value = if (before != null && after != null && before.values() != after.values())
            UndoListChange(++undoId, animeId, after.title, before.values(), after.values(), session.epoch) else null
    }

    override fun dismissUndo(id: Long) { undo.update { if (it?.id == id) null else it } }

    override suspend fun undoListChange(id: Long): AppResult<Unit> = session.mutex.withLock {
        val change = undo.value?.takeIf { it.id == id } ?: return@withLock AppResult.Error(AppError.NoCache)
        val current = malListEntryDao.getByAnimeId(change.animeId)
        if (session.epoch != change.sessionEpoch || current?.values() != change.after) {
            dismissUndo(id)
            return@withLock AppResult.Error(AppError.Unknown("The entry has changed"))
        }
        updateLocked(change.animeId, change.before, offerUndo = false).also {
            if (it is AppResult.Success) dismissUndo(id)
        }
    }

    /** Receipt + optimistic progress + queue are committed together before touching the network.
     * A process dying after commit can only resend the absolute progress, never apply a second +1. */
    suspend fun incrementFromNotification(notificationId: Int, token: String): AppResult<Unit> = session.mutex.withLock {
        val actions = notificationActions ?: return@withLock AppResult.Error(AppError.NoCache)
        val receipt = actions.get(notificationId)?.takeIf { it.actionToken == token && token.isNotEmpty() } ?: return@withLock AppResult.Error(AppError.NoCache)
        if (receipt.progressHandled) return@withLock AppResult.Success(Unit)
        if (!prefsDataStore.userPreferencesFlow.first().malLoggedIn) return@withLock AppResult.Error(AppError.Unauthorized)
        val before = malListEntryDao.getByAnimeId(receipt.malId) ?: return@withLock AppResult.Error(AppError.NoCache)
        if (before.status != WatchStatus.WATCHING.malValue) return@withLock AppResult.Error(AppError.NoCache)
        val next = (before.numEpisodesWatched + 1).let { n -> before.totalEpisodes?.takeIf { it > 0 }?.let { minOf(n, it) } ?: n }
        transaction {
            if (next != before.numEpisodesWatched) queueUpdate(receipt.malId, MalListUpdate(episodesWatched = next), schedule = false)
            actions.consumeProgress(notificationId)
        }
        undo.value = null
        recordActivity(receipt.malId, before)
        // Offline is already a durable success. The constrained worker completes delivery later.
        workScheduler.scheduleFlushPendingUpdates()
        AppResult.Success(Unit)
    }

    private suspend fun recordActivity(animeId: Int, before: MalListEntryEntity?) {
        if (watchToolsStore == null) return
        val after = malListEntryDao.getByAnimeId(animeId) ?: return
        // Metadata failure must not turn a successful MAL edit into an apparent sync failure.
        try {
            watchToolsStore.recordProgress(animeId, after.title, before?.numEpisodesWatched ?: 0, after.numEpisodesWatched)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            Log.w(TAG, "Watch history could not be saved", error)
        }
    }

    override suspend fun incrementEpisode(animeId: Int): AppResult<Unit> = session.mutex.withLock {
        val existing = malListEntryDao.getByAnimeId(animeId) ?: return@withLock AppResult.Error(AppError.NoCache)
        // MAL rejects progress beyond the episode count, so stop at the known total.
        val total = existing.totalEpisodes?.takeIf { it > 0 }
        val newEpisodes = (existing.numEpisodesWatched + 1).let { next -> total?.let { minOf(next, it) } ?: next }
        updateLocked(animeId, MalListUpdate(episodesWatched = newEpisodes))
    }

    override suspend fun removeListEntry(animeId: Int): AppResult<Unit> = session.mutex.withLock {
        if (!prefsDataStore.userPreferencesFlow.first().malLoggedIn) return@withLock AppResult.Error(AppError.Unauthorized)
        undo.value = null
        val result = if (network?.isOnline == false) AppResult.Error(AppError.Network("offline")) else executeWithRefresh {
            deleteOnMal(animeId)
            transaction {
                malListEntryDao.deleteByAnimeId(animeId)
                pendingListUpdateDao.deleteByAnimeId(animeId)
            }
        }
        if (result is AppResult.Error && result.error is AppError.Network) {
            queueRemoval(animeId)
            return@withLock AppResult.Success(Unit)
        }
        result
    }

    override suspend fun refreshUserList(force: Boolean): Boolean = session.mutex.withLock { runSync(force) }

    override suspend fun retrySync(): Boolean = session.mutex.withLock {
        val complete = runSync(force = true, retryRejected = true) && pendingListUpdateDao.getAll().isEmpty()
        failed.value = !complete
        complete
    }

    private suspend fun runSync(force: Boolean, retryRejected: Boolean = false): Boolean {
        syncing.value = true
        failed.value = false
        return try {
            syncUserList(force, retryRejected).also { failed.value = !it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Log.w(TAG, "MAL list sync failed", e)
            failed.value = true
            false
        } finally { syncing.value = false }
    }

    private suspend fun syncUserList(force: Boolean, retryRejected: Boolean): Boolean {
        // Nothing to mirror (and a guaranteed 401) without a signed-in account.
        if (!prefsDataStore.userPreferencesFlow.first().malLoggedIn) return true
        val now = System.currentTimeMillis()
        val lastSync = prefsDataStore.getLastMalListSyncEpochMs()
        if (!force && pendingListUpdateDao.getAll().isEmpty() && (now - lastSync) < MAL_LIST_CACHE_TTL_MS) return true
        // Push local queued edits first so the server state we're about to mirror includes them.
        val flushed = flushLocked(retryRejected)
        var offset = 0
        val allEntities = mutableListOf<MalListEntryEntity>()
        while (true) {
            // A failed page aborts the whole sync (keep the stale-but-complete local copy):
            // replacing the table with a partial page set would silently drop entries.
            val response = fetchAnimeListPage(offset) ?: return false
            allEntities.addAll(response.data.map { it.node.toEntity() })
            if (response.paging?.next == null) break
            offset += 100
        }
        // Anything still pending (flush above failed) must survive the replace: keep the
        // optimistic local rows for queued adds and re-apply queued edits/removals on top
        // of the server state.
        val pending = pendingListUpdateDao.getAll().associateBy { it.animeId }
        val serverIds = allEntities.mapTo(mutableSetOf()) { it.animeId }
        val queuedAddRows = pending.values
            .filter { !it.isRemoval && it.animeId !in serverIds }
            .mapNotNull { malListEntryDao.getByAnimeId(it.animeId) }
        val merged = allEntities.mapNotNull { entity ->
            val p = pending[entity.animeId] ?: return@mapNotNull entity
            if (p.isRemoval) null else entity.copy(
                status = p.status ?: entity.status,
                numEpisodesWatched = p.numWatchedEpisodes ?: entity.numEpisodesWatched,
                score = p.score ?: entity.score
            )
        } + queuedAddRows
        // Full fetch succeeded — replace even when empty, so remote deletions propagate.
        malListEntryDao.replaceAll(merged)
        prefsDataStore.setLastMalListSyncEpochMs(System.currentTimeMillis())
        if (flushed) prefsDataStore.setLastMalSyncSuccess(System.currentTimeMillis())
        return true
    }

    override suspend fun flushPendingUpdates(): Boolean = session.mutex.withLock {
        syncing.value = true
        try { flushLocked().also { failed.value = !it } } finally { syncing.value = false }
    }

    private suspend fun flushLocked(retryRejected: Boolean = false): Boolean {
        val pending = pendingListUpdateDao.getAll()
        if (pending.isEmpty()) return true
        if (!prefsDataStore.userPreferencesFlow.first().malLoggedIn) return false
        var allFlushed = true
        for (p in pending) {
            if (p.rejected && !retryRejected) { allFlushed = false; continue }
            val result = executeWithRefresh {
                if (p.isRemoval) {
                    deleteOnMal(p.animeId)
                } else {
                    val patched = malApiService.updateListStatus(
                        animeId = p.animeId,
                        status = p.status,
                        numWatchedEpisodes = p.numWatchedEpisodes,
                        score = p.score
                    )
                    applyPatchedLocally(p.animeId, patched)
                }
            }
            when ((result as? AppResult.Error)?.error) {
                null -> pendingListUpdateDao.deleteByAnimeId(p.animeId)
                // Keep rejected edits visible in the sync center; only an explicit retry/edit
                // submits them again. A server rejection must never look like "all saved".
                is AppError.Unknown -> {
                    pendingListUpdateDao.upsert(p.copy(rejected = true))
                    allFlushed = false
                }
                // Session is dead: no point hammering the remaining rows, but keep the queue so
                // a future re-login can still deliver the edits.
                AppError.Unauthorized -> return false
                else -> allFlushed = false
            }
        }
        if (allFlushed) prefsDataStore.setLastMalSyncSuccess(System.currentTimeMillis())
        return allFlushed
    }

    /** DELETE returns Response (no HttpException on error) — normalize: 404 = already gone
     *  = fine; 401 and others are rethrown so [executeWithRefresh] can handle them. */
    private suspend fun deleteOnMal(animeId: Int) {
        val response = malApiService.deleteListStatus(animeId)
        if (!response.isSuccessful && response.code() != 404) {
            throw HttpException(response)
        }
    }

    /** Mirrors a successful PATCH into the local cache; inserts a new row (with metadata from
     *  the MAL node or the AniList detail cache) when the anime wasn't on the list yet. */
    private suspend fun applyPatchedLocally(animeId: Int, patched: MalListStatus) {
        // Stamp the local edit time immediately so the "recently changed" home
        // section reflects this update right away, instead of waiting for the
        // next full list sync to pull MAL's own updated_at back down.
        val editedNow = Instant.now().toString()
        val existing = malListEntryDao.getByAnimeId(animeId)
        if (existing != null) {
            malListEntryDao.upsert(
                existing.copy(
                    status = patched.status,
                    numEpisodesWatched = patched.numEpisodesWatched,
                    score = patched.score,
                    updatedAt = editedNow
                )
            )
        } else {
            val node = fetchAnimeNodeOrNull(animeId)
            malListEntryDao.upsert(
                MalListEntryEntity(
                    animeId = animeId,
                    malId = animeId,
                    title = node?.title ?: cachedDetailTitle(animeId) ?: "",
                    coverImageUrl = node?.mainPicture?.large ?: node?.mainPicture?.medium
                        ?: animeDetailDao.getByMalId(animeId)?.coverImageUrl,
                    totalEpisodes = node?.numEpisodes ?: animeDetailDao.getByMalId(animeId)?.episodes,
                    status = patched.status,
                    numEpisodesWatched = patched.numEpisodesWatched,
                    score = patched.score,
                    updatedAt = editedNow
                )
            )
        }
    }

    /** Best-effort metadata for a first-time add; the local row falls back to cached details. */
    private suspend fun fetchAnimeNodeOrNull(animeId: Int) = try {
        malApiService.getAnimeDetail(animeId)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    /** Applies an offline edit optimistically to the local cache and queues it for flush. */
    private suspend fun queueUpdate(animeId: Int, update: MalListUpdate, schedule: Boolean = true) {
        transaction {
            val previous = pendingListUpdateDao.getByAnimeId(animeId)
            pendingListUpdateDao.upsert(
                PendingListUpdateEntity(
                    animeId = animeId,
                    status = update.status?.malValue ?: previous?.takeIf { !it.isRemoval }?.status,
                    numWatchedEpisodes = update.episodesWatched
                        ?: previous?.takeIf { !it.isRemoval }?.numWatchedEpisodes,
                    score = update.score ?: previous?.takeIf { !it.isRemoval }?.score,
                    isRemoval = false,
                    queuedAtEpochMs = System.currentTimeMillis()
                )
            )
            val editedNow = Instant.now().toString()
            val existing = malListEntryDao.getByAnimeId(animeId)
            if (existing != null) {
                malListEntryDao.upsert(
                    existing.copy(
                        status = update.status?.malValue ?: existing.status,
                        numEpisodesWatched = update.episodesWatched ?: existing.numEpisodesWatched,
                        score = update.score ?: existing.score,
                        updatedAt = editedNow
                    )
                )
            } else {
                val detail = animeDetailDao.getByMalId(animeId)
                malListEntryDao.upsert(
                    MalListEntryEntity(
                        animeId = animeId,
                        malId = animeId,
                        title = cachedDetailTitle(animeId) ?: "",
                        coverImageUrl = detail?.coverImageUrl,
                        totalEpisodes = detail?.episodes,
                        status = update.status?.malValue ?: "plan_to_watch",
                        numEpisodesWatched = update.episodesWatched ?: 0,
                        score = update.score ?: 0,
                        updatedAt = editedNow
                    )
                )
            }
        }
        if (schedule) workScheduler.scheduleFlushPendingUpdates()
    }

    private suspend fun queueRemoval(animeId: Int) {
        transaction {
            pendingListUpdateDao.upsert(
                PendingListUpdateEntity(
                    animeId = animeId,
                    status = null,
                    numWatchedEpisodes = null,
                    score = null,
                    isRemoval = true,
                    queuedAtEpochMs = System.currentTimeMillis()
                )
            )
            malListEntryDao.deleteByAnimeId(animeId)
        }
        workScheduler.scheduleFlushPendingUpdates()
    }

    private suspend fun cachedDetailTitle(malId: Int): String? {
        val detail = animeDetailDao.getByMalId(malId) ?: return null
        return detail.titleEnglish ?: detail.titleRomaji ?: detail.titleNative
    }

    private suspend fun fetchAnimeListPage(offset: Int): MalAnimeListResponse? =
        (executeWithRefresh { malApiService.getUserAnimeList(offset = offset) } as? AppResult.Success)?.data

    /**
     * Runs [block] with a fresh access token and retries once after a 401 refresh.
     *
     * Failures map onto what the offline queue needs: [AppError.Network] means "worth retrying
     * later" (connectivity, timeouts, 5xx, rate limiting), [AppError.Unknown] means the server
     * rejected the request itself (other 4xx) so retrying the same payload would be pointless,
     * and [AppError.Unauthorized] means the session is really gone.
     */
    private suspend fun <T> executeWithRefresh(block: suspend () -> T): AppResult<T> {
        malAuthManager.ensureFreshToken()
        val first = attempt(block)
        if (first !is AppResult.Error || first.error != AppError.Unauthorized) return first

        return when (malAuthManager.refreshAccessToken(force = true)) {
            MalAuthManager.RefreshResult.REFRESHED -> attempt(block).also { retry ->
                // Only a second 401 means the session is really dead; a transient failure on the
                // retry must not log the user out.
                if (retry is AppResult.Error && retry.error == AppError.Unauthorized) {
                    prefsDataStore.expireMalSession()
                }
            }
            MalAuthManager.RefreshResult.INVALID -> {
                prefsDataStore.expireMalSession()
                AppResult.Error(AppError.Unauthorized)
            }
            MalAuthManager.RefreshResult.TRANSIENT ->
                AppResult.Error(AppError.Network("token refresh unavailable"))
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: HttpException) {
        when (e.code()) {
            HTTP_UNAUTHORIZED -> AppResult.Error(AppError.Unauthorized)
            HTTP_REQUEST_TIMEOUT, HTTP_TOO_MANY_REQUESTS, in 500..599 ->
                AppResult.Error(AppError.Network(e.message(), e.code()))
            else -> AppResult.Error(AppError.Unknown(e.message()))
        }
    } catch (e: IOException) {
        AppResult.Error(AppError.Network(e.message))
    } catch (e: Exception) {
        AppResult.Error(AppError.Unknown(e.message))
    }

    private companion object {
        const val TAG = "MalRepository"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
