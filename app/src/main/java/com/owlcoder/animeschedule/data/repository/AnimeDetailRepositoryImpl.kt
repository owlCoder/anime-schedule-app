package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.alternative.AlternativeAnimeDataSource
import com.owlcoder.animeschedule.data.api.alternative.toInternalId
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.offline.OfflineCatalogDataSource
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity
import com.owlcoder.animeschedule.data.provider.ProviderCall
import com.owlcoder.animeschedule.data.provider.ProviderCallException
import com.owlcoder.animeschedule.data.provider.ProviderOperation
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import com.owlcoder.animeschedule.data.provider.ProviderResult
import com.owlcoder.animeschedule.data.provider.requireProviderData
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
    private val aniListDataSource: AniListRemoteDataSource,
    private val alternativeDataSource: AlternativeAnimeDataSource,
    private val providerOrchestrator: ProviderOrchestrator,
    private val offlineCatalogDataSource: OfflineCatalogDataSource
) : AnimeDetailRepository {

    override fun getAnimeDetail(animeId: Int): Flow<AppResult<AnimeDetail>> = flow {
        ensureDetailCached(animeId)

        // Resolve the canonical AnimeDetailEntity (may be stored under AniList or MAL ID)
        val resolved = animeDetailDao.getByIdOnce(animeId) ?: animeDetailDao.getByMalId(animeId)

        if (resolved == null) {
            when (val offline = offlineCatalogDataSource.getDetail(animeId)) {
                is AppResult.Success -> emit(offline)
                is AppResult.Error -> emit(AppResult.Error(offline.error))
            }
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

        val canonicalAniListId = entity?.animeId ?: animeId
        val malId = entity?.malId ?: animeId.takeIf { entity == null && it > 0 }
        val result = providerOrchestrator.firstSuccessful(
            operation = ProviderOperation.DETAIL,
            calls = listOf(
                ProviderCall("AniList") {
                    if (entity != null) {
                        val media = aniListDataSource.getAnimeDetail(canonicalAniListId)
                            .requireProviderData("AniList")
                        DetailPayload(media.toEntity(nowEpoch).copy(malId = entity.malId))
                    } else {
                        val byAniList = aniListDataSource.getAnimeDetail(canonicalAniListId)
                        if (byAniList is AppResult.Success) {
                            DetailPayload(byAniList.data.toEntity(nowEpoch))
                        } else {
                            val byMal = aniListDataSource.getAnimeDetailByMalId(animeId)
                                .requireProviderData("AniList")
                            DetailPayload(byMal.toEntity(nowEpoch, malId = animeId))
                        }
                    }
                },
                ProviderCall("AnimeSchedule") {
                    val item = alternativeDataSource.getByAnimeScheduleAniListId(canonicalAniListId)
                        ?: malId?.let { alternativeDataSource.getByAnimeScheduleMalId(it) }
                        ?: throw ProviderCallException("AnimeSchedule", message = "anime not found")
                    DetailPayload(
                        alternativeDataSource.run {
                            item.toDetailEntity(entity?.animeId ?: item.toInternalId(), nowEpoch)
                        }
                    )
                }
            )
        )
        if (result is ProviderResult.Success) {
            animeDetailDao.upsert(result.value.entity)
        }
    }

    private data class DetailPayload(val entity: AnimeDetailEntity)
}
