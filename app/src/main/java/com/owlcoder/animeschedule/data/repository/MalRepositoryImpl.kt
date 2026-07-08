package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.mal.MalApiService
import com.owlcoder.animeschedule.data.api.mal.auth.MalAuthManager
import com.owlcoder.animeschedule.data.api.mal.dto.MalListStatus
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryEntity
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateDao
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateEntity
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity
import com.owlcoder.animeschedule.data.work.PendingUpdateScheduler
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.repository.MalRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val MAL_LIST_CACHE_TTL_MS = 60 * 60 * 1000L // 1h

@Singleton
class MalRepositoryImpl @Inject constructor(
    private val malApiService: MalApiService,
    private val malListEntryDao: MalListEntryDao,
    private val pendingListUpdateDao: PendingListUpdateDao,
    private val animeDetailDao: AnimeDetailDao,
    private val malAuthManager: MalAuthManager,
    private val prefsDataStore: UserPreferencesDataStore,
    private val pendingUpdateScheduler: PendingUpdateScheduler
) : MalRepository {

    override fun getUserList(): Flow<AppResult<List<MalListEntry>>> =
        malListEntryDao.getAll().map { entities ->
            AppResult.Success(entities.map { it.toDomain() })
        }

    override suspend fun updateListEntry(animeId: Int, update: MalListUpdate): AppResult<Unit> {
        val result = executeWithRefresh {
            val patched = malApiService.updateListStatus(
                animeId = animeId,
                status = update.status?.malValue,
                numWatchedEpisodes = update.episodesWatched,
                score = update.score
            )
            applyPatchedLocally(animeId, patched)
            AppResult.Success(Unit)
        }
        // Couldn't reach MAL (offline / server down): apply the edit locally and queue it for
        // a background flush — the change is not lost, so report success to the UI.
        if (result is AppResult.Error && result.error is AppError.Network) {
            queueUpdate(animeId, update)
            return AppResult.Success(Unit)
        }
        return result
    }

    override suspend fun incrementEpisode(animeId: Int): AppResult<Unit> {
        val existing = malListEntryDao.getByAnimeId(animeId) ?: return AppResult.Error(AppError.NoCache)
        val newEpisodes = existing.numEpisodesWatched + 1
        return updateListEntry(animeId, MalListUpdate(episodesWatched = newEpisodes))
    }

    override suspend fun removeListEntry(animeId: Int): AppResult<Unit> {
        val result = executeWithRefresh {
            deleteOnMal(animeId)
            malListEntryDao.deleteByAnimeId(animeId)
            pendingListUpdateDao.deleteByAnimeId(animeId)
            AppResult.Success(Unit)
        }
        if (result is AppResult.Error && result.error is AppError.Network) {
            queueRemoval(animeId)
            return AppResult.Success(Unit)
        }
        return result
    }

    override suspend fun refreshUserList(force: Boolean): Boolean {
        val now = System.currentTimeMillis()
        val lastSync = prefsDataStore.getLastMalListSyncEpochMs()
        if (!force && (now - lastSync) < MAL_LIST_CACHE_TTL_MS) return true
        // Push local queued edits first so the server state we're about to mirror includes them.
        flushPendingUpdates()
        malAuthManager.ensureFreshToken()
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
        prefsDataStore.setLastMalListSyncEpochMs(now)
        return true
    }

    override suspend fun flushPendingUpdates(): Boolean {
        val pending = pendingListUpdateDao.getAll()
        if (pending.isEmpty()) return true
        var allFlushed = true
        for (p in pending) {
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
                AppResult.Success(Unit)
            }
            if (result is AppResult.Success) {
                pendingListUpdateDao.deleteByAnimeId(p.animeId)
            } else {
                allFlushed = false
                // Session is dead — no point hammering the remaining rows; keep the queue
                // so a future re-login can still deliver the edits.
                if ((result as AppResult.Error).error is AppError.Unauthorized) break
            }
        }
        return allFlushed
    }

    /** DELETE returns Response (no HttpException on error) — normalize: 404 = already gone
     *  = fine; 401 and others are rethrown so [executeWithRefresh] can handle them. */
    private suspend fun deleteOnMal(animeId: Int) {
        val response = malApiService.deleteListStatus(animeId)
        if (!response.isSuccessful && response.code() != 404) {
            throw retrofit2.HttpException(response)
        }
    }

    /** Mirrors a successful PATCH into the local cache; inserts a new row (with metadata from
     *  the MAL node or the AniList detail cache) when the anime wasn't on the list yet. */
    private suspend fun applyPatchedLocally(animeId: Int, patched: MalListStatus) {
        // Stamp the local edit time immediately so the "recently changed" home
        // section reflects this update right away, instead of waiting for the
        // next full list sync to pull MAL's own updated_at back down.
        val editedNow = java.time.Instant.now().toString()
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
            val node = runCatching { malApiService.getAnimeDetail(animeId) }.getOrNull()
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

    /** Applies an offline edit optimistically to the local cache and queues it for flush. */
    private suspend fun queueUpdate(animeId: Int, update: MalListUpdate) {
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
        val editedNow = java.time.Instant.now().toString()
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
        pendingUpdateScheduler.scheduleFlush()
    }

    private suspend fun queueRemoval(animeId: Int) {
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
        pendingUpdateScheduler.scheduleFlush()
    }

    private suspend fun cachedDetailTitle(malId: Int): String? {
        val detail = animeDetailDao.getByMalId(malId) ?: return null
        return detail.titleEnglish ?: detail.titleRomaji ?: detail.titleNative
    }

    private suspend fun fetchAnimeListPage(offset: Int) =
        try {
            malApiService.getUserAnimeList(offset = offset)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                when (malAuthManager.refreshAccessToken()) {
                    MalAuthManager.RefreshResult.REFRESHED ->
                        runCatching { malApiService.getUserAnimeList(offset = offset) }.getOrNull()
                    MalAuthManager.RefreshResult.INVALID -> {
                        prefsDataStore.setMalLoggedIn(false)
                        null
                    }
                    MalAuthManager.RefreshResult.TRANSIENT -> null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

    // Error mapping matters for the offline queue: AppError.Network (connectivity — safe to
    // queue and retry later) vs AppError.Unknown (HTTP 4xx/5xx — the server rejected the
    // request; retrying the same payload forever would be wrong).
    private suspend fun <T> executeWithRefresh(block: suspend () -> AppResult<T>): AppResult<T> {
        malAuthManager.ensureFreshToken()
        return try {
            block()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                when (malAuthManager.refreshAccessToken()) {
                    MalAuthManager.RefreshResult.REFRESHED -> try {
                        block()
                    } catch (e2: retrofit2.HttpException) {
                        // Only a second 401 means the session is really dead; a transient
                        // network/server error on the retry must not log the user out.
                        if (e2.code() == 401) {
                            prefsDataStore.setMalLoggedIn(false)
                            AppResult.Error(AppError.Unauthorized)
                        } else {
                            AppResult.Error(AppError.Unknown(e2.message()))
                        }
                    } catch (e2: java.io.IOException) {
                        AppResult.Error(AppError.Network(e2.message))
                    } catch (e2: kotlinx.coroutines.CancellationException) {
                        throw e2
                    } catch (e2: Exception) {
                        AppResult.Error(AppError.Unknown(e2.message))
                    }
                    MalAuthManager.RefreshResult.INVALID -> {
                        prefsDataStore.setMalLoggedIn(false)
                        AppResult.Error(AppError.Unauthorized)
                    }
                    MalAuthManager.RefreshResult.TRANSIENT ->
                        AppResult.Error(AppError.Network(e.message()))
                }
            } else {
                AppResult.Error(AppError.Unknown(e.message()))
            }
        } catch (e: java.io.IOException) {
            AppResult.Error(AppError.Network(e.message))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message))
        }
    }
}
