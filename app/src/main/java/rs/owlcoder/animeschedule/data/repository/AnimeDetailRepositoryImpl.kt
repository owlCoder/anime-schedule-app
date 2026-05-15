package rs.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import rs.owlcoder.animeschedule.core.result.AppError
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import rs.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import rs.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import rs.owlcoder.animeschedule.data.local.db.MalListEntryDao
import rs.owlcoder.animeschedule.data.mapper.toDomain
import rs.owlcoder.animeschedule.data.mapper.toEntity
import rs.owlcoder.animeschedule.domain.model.AnimeDetail
import rs.owlcoder.animeschedule.domain.repository.AnimeDetailRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val DETAIL_CACHE_TTL = 60 * 60L

@Singleton
class AnimeDetailRepositoryImpl @Inject constructor(
    private val animeDetailDao: AnimeDetailDao,
    private val malListEntryDao: MalListEntryDao,
    private val aniListDataSource: AniListRemoteDataSource
) : AnimeDetailRepository {

    override fun getAnimeDetail(animeId: Int): Flow<AppResult<AnimeDetail>> = flow {
        // Ensure data is in cache (fetch if missing/stale)
        ensureDetailCached(animeId)

        // Now combine reactive Room flows so edits to MAL list refresh UI instantly
        combine(
            animeDetailDao.getById(animeId),
            malListEntryDao.observeByAnimeId(animeId)
        ) { entity, malEntity ->
            val resolved = entity ?: animeDetailDao.getByMalId(animeId)
            val malEntry = malEntity?.toDomain()
            if (resolved != null) AppResult.Success(resolved.toDomain(malEntry))
            else AppResult.Error(AppError.NoCache) as AppResult<AnimeDetail>
        }.collect { emit(it) }
    }

    private suspend fun ensureDetailCached(animeId: Int) {
        val nowEpoch = Instant.now().epochSecond
        val entity: AnimeDetailEntity? = animeDetailDao.getByIdOnce(animeId)
            ?: animeDetailDao.getByMalId(animeId)

        val stale = entity == null || (nowEpoch - entity.cachedAtEpochSeconds) > DETAIL_CACHE_TTL
        if (!stale) return

        if (entity != null) {
            val r = aniListDataSource.getAnimeDetail(entity.animeId)
            if (r is AppResult.Success) {
                animeDetailDao.upsert(r.data.toEntity(nowEpoch).copy(malId = entity.malId))
            }
        } else {
            // Try as AniList ID first (schedule/search); fall back to MAL ID (my-list)
            val byAniList = aniListDataSource.getAnimeDetail(animeId)
            if (byAniList is AppResult.Success) {
                animeDetailDao.upsert(byAniList.data.toEntity(nowEpoch))
            } else {
                val byMal = aniListDataSource.getAnimeDetailByMalId(animeId)
                if (byMal is AppResult.Success) {
                    animeDetailDao.upsert(byMal.data.toEntity(nowEpoch, malId = animeId))
                }
            }
        }
    }
}
