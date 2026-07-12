package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.CharacterDetail
import com.owlcoder.animeschedule.domain.repository.AnimeDetailRepository
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
        ensureDetailCached(animeId)

        // Resolve the canonical AnimeDetailEntity (may be stored under AniList or MAL ID)
        val resolved = animeDetailDao.getByIdOnce(animeId) ?: animeDetailDao.getByMalId(animeId)

        if (resolved == null) {
            emit(AppResult.Error(AppError.NoCache) as AppResult<AnimeDetail>)
            return@flow
        }

        // Use malId from entity to look up MAL list entry — mal_list_entries stores MAL IDs
        val malObservable = resolved.malId?.let { malListEntryDao.observeByMalId(it) }
            ?: malListEntryDao.observeByAnimeId(animeId)

        combine(
            animeDetailDao.getById(resolved.animeId),
            malObservable
        ) { entity, malEntity ->
            val detail = entity ?: resolved
            val malEntry = malEntity?.toDomain()
            AppResult.Success(detail.toDomain(malEntry)) as AppResult<AnimeDetail>
        }.collect { emit(it) }
    }

    override suspend fun getCharacterDetail(characterId: Int): AppResult<CharacterDetail> {
        val result = aniListDataSource.getCharacterDetail(characterId)
        return when (result) {
            is AppResult.Success -> AppResult.Success(
                CharacterDetail(
                    id = result.data.id,
                    name = result.data.name?.full ?: "",
                    nativeName = result.data.name?.native,
                    imageUrl = result.data.image?.large,
                    description = result.data.description
                )
            )
            is AppResult.Error -> AppResult.Error(result.error)
        }
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
