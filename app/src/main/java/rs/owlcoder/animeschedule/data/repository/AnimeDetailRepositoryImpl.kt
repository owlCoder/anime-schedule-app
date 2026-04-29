package rs.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import rs.owlcoder.animeschedule.data.local.db.AnimeDetailDao
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

    override fun getAnimeDetail(animeId: Int): Flow<AppResult<AnimeDetail>> =
        combine(
            animeDetailDao.getById(animeId),
            malListEntryDao.observeByAnimeId(animeId)
        ) { entity, malEntity ->
            val malEntry = malEntity?.toDomain()
            if (entity != null) {
                val nowEpoch = Instant.now().epochSecond
                if (nowEpoch - entity.cachedAtEpochSeconds > DETAIL_CACHE_TTL) {
                    refreshDetail(animeId)
                }
                AppResult.Success(entity.toDomain(malEntry))
            } else {
                refreshDetail(animeId)
                val fresh = animeDetailDao.getById(animeId)
                AppResult.Success(
                    animeDetailDao.getCacheTime(animeId)?.let {
                        entity?.toDomain(malEntry)
                    } ?: return@combine AppResult.Error(rs.owlcoder.animeschedule.core.result.AppError.NoCache)
                )
            }
        }

    private suspend fun refreshDetail(animeId: Int) {
        val result = aniListDataSource.getAnimeDetail(animeId)
        if (result is AppResult.Success) {
            animeDetailDao.upsert(result.data.toEntity(Instant.now().epochSecond))
        }
    }
}
