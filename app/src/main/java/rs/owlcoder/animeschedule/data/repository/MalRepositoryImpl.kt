package rs.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rs.owlcoder.animeschedule.core.result.AppError
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.api.mal.MalApiService
import rs.owlcoder.animeschedule.data.api.mal.auth.MalAuthManager
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import rs.owlcoder.animeschedule.data.local.db.MalListEntryDao
import rs.owlcoder.animeschedule.data.mapper.toDomain
import rs.owlcoder.animeschedule.data.mapper.toEntity
import rs.owlcoder.animeschedule.domain.model.MalListEntry
import rs.owlcoder.animeschedule.domain.model.MalListUpdate
import rs.owlcoder.animeschedule.domain.repository.MalRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val MAL_LIST_CACHE_TTL_MS = 60 * 60 * 1000L // 1h

@Singleton
class MalRepositoryImpl @Inject constructor(
    private val malApiService: MalApiService,
    private val malListEntryDao: MalListEntryDao,
    private val malAuthManager: MalAuthManager,
    private val prefsDataStore: UserPreferencesDataStore
) : MalRepository {

    private var lastRefreshMs: Long = 0L

    override fun getUserList(): Flow<AppResult<List<MalListEntry>>> =
        malListEntryDao.getAll().map { entities ->
            AppResult.Success(entities.map { it.toDomain() })
        }

    override suspend fun updateListEntry(animeId: Int, update: MalListUpdate): AppResult<Unit> {
        return executeWithRefresh {
            malApiService.updateListStatus(
                animeId = animeId,
                status = update.status?.malValue,
                numWatchedEpisodes = update.episodesWatched,
                score = update.score
            )
            val existing = malListEntryDao.getByAnimeId(animeId)
            if (existing != null) {
                malListEntryDao.upsert(
                    existing.copy(
                        status = update.status?.malValue ?: existing.status,
                        numEpisodesWatched = update.episodesWatched ?: existing.numEpisodesWatched,
                        score = update.score ?: existing.score
                    )
                )
            }
            AppResult.Success(Unit)
        }
    }

    override suspend fun incrementEpisode(animeId: Int): AppResult<Unit> {
        val existing = malListEntryDao.getByAnimeId(animeId) ?: return AppResult.Error(AppError.NoCache)
        val newEpisodes = existing.numEpisodesWatched + 1
        return updateListEntry(animeId, MalListUpdate(episodesWatched = newEpisodes))
    }

    override suspend fun refreshUserList(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastRefreshMs) < MAL_LIST_CACHE_TTL_MS) return
        var offset = 0
        val allEntities = mutableListOf<rs.owlcoder.animeschedule.data.local.db.MalListEntryEntity>()
        while (true) {
            val response = runCatching { malApiService.getUserAnimeList(offset = offset) }.getOrNull() ?: break
            val entities = response.data.map { it.node.toEntity() }
            allEntities.addAll(entities)
            if (response.paging?.next == null) break
            offset += 100
        }
        if (allEntities.isNotEmpty()) {
            malListEntryDao.deleteAll()
            malListEntryDao.upsertAll(allEntities)
            lastRefreshMs = now
        }
    }

    private suspend fun <T> executeWithRefresh(block: suspend () -> AppResult<T>): AppResult<T> {
        return try {
            block()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                val refreshed = malAuthManager.refreshAccessToken()
                if (refreshed) {
                    try { block() } catch (e2: Exception) {
                        prefsDataStore.setMalLoggedIn(false)
                        AppResult.Error(AppError.Unauthorized)
                    }
                } else {
                    prefsDataStore.setMalLoggedIn(false)
                    AppResult.Error(AppError.Unauthorized)
                }
            } else {
                AppResult.Error(AppError.Network(e.message()))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.Network(e.message))
        }
    }
}
