package com.owlcoder.animeschedule.data.repository

import android.util.Log
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.alternative.AlternativeAnimeDataSource
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.offline.OfflineCatalogDataSource
import com.owlcoder.animeschedule.data.provider.ProviderCall
import com.owlcoder.animeschedule.data.provider.ProviderCallException
import com.owlcoder.animeschedule.data.provider.ProviderOperation
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import com.owlcoder.animeschedule.data.provider.ProviderResult
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.CharacterDetail
import com.owlcoder.animeschedule.domain.repository.AnimeDetailRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import com.owlcoder.animeschedule.data.api.alternative.toInternalId
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity
import com.owlcoder.animeschedule.data.provider.requireProviderData
import com.owlcoder.animeschedule.data.mapper.toDetailEntity

private const val DETAIL_CACHE_TTL_SECONDS = 60 * 60L
private const val TAG = "AnimeDetailRepository"

@Singleton
class AnimeDetailRepositoryImpl @Inject constructor(
    private val animeDetailDao: AnimeDetailDao,
    private val malListEntryDao: MalListEntryDao,
    private val aniListDataSource: AniListRemoteDataSource,
    private val alternativeDataSource: AlternativeAnimeDataSource,
    private val providerOrchestrator: ProviderOrchestrator,
    private val offlineCatalogDataSource: OfflineCatalogDataSource
) : AnimeDetailRepository {

    /**
     * Emits the cached detail row and keeps following it. A missing row is fetched before the
     * first emission; a stale row is shown immediately and refreshed in the background, so the
     * screen never waits on the network while usable data is already on disk.
     */
    override fun getAnimeDetail(animeId: Int): Flow<AppResult<AnimeDetail>> = channelFlow {
        val cached = findCached(animeId)
        when {
            cached == null -> refreshDetail(animeId, null)
            cached.isStale() -> launch { refreshDetail(animeId, cached) }
        }

        // The row may be stored under the AniList id or the MAL id the caller navigated with.
        val resolved = findCached(animeId)
        if (resolved == null) {
            send(offlineCatalogDataSource.getDetail(animeId))
            return@channelFlow
        }

        // mal_list_entries is keyed by MAL id, so prefer the id stored on the detail row.
        val malEntry = resolved.malId?.let { malListEntryDao.observeByMalId(it) }
            ?: malListEntryDao.observeByAnimeId(animeId)

        combine(animeDetailDao.getById(resolved.animeId), malEntry) { entity, malEntity ->
            AppResult.Success((entity ?: resolved).toDomain(malEntity?.toDomain())) as AppResult<AnimeDetail>
        }.collect { send(it) }
    }

    override suspend fun getCharacterDetail(characterId: Int): AppResult<CharacterDetail> =
        when (val result = aniListDataSource.getCharacterDetail(characterId)) {
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

    private suspend fun findCached(animeId: Int): AnimeDetailEntity? =
        animeDetailDao.getByIdOnce(animeId) ?: animeDetailDao.getByMalId(animeId)

    private fun AnimeDetailEntity.isStale(): Boolean =
        Instant.now().epochSecond - cachedAtEpochSeconds > DETAIL_CACHE_TTL_SECONDS

    /** Best-effort refresh: a failure leaves whatever is cached untouched. */
    private suspend fun refreshDetail(animeId: Int, cached: AnimeDetailEntity?) {
        try {
            val fresh = fetchDetail(animeId, cached) ?: return
            animeDetailDao.upsert(fresh)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Detail refresh failed for $animeId", error)
        }
    }

    private suspend fun fetchDetail(animeId: Int, cached: AnimeDetailEntity?): AnimeDetailEntity? {
        val nowEpoch = Instant.now().epochSecond
        val storedId = cached?.animeId ?: animeId
        // With no cached row the caller's id may be either an AniList id or a MAL id.
        val malId = cached?.malId ?: animeId.takeIf { cached == null && it > 0 }

        val result = providerOrchestrator.firstSuccessful(
            operation = ProviderOperation.DETAIL,
            calls = listOf(
                ProviderCall("AniList") {
                    fetchFromAniList(storedId, malId, nowEpoch, canonicalMissing = cached == null)
                },
                ProviderCall("AnimeSchedule") {
                    val item = alternativeDataSource.getByAniListId(storedId)
                        ?: malId?.let { alternativeDataSource.getByMalId(it) }
                        ?: throw ProviderCallException("AnimeSchedule", message = "anime not found")
                    item.toDetailEntity(cached?.animeId ?: item.toInternalId(), nowEpoch)
                }
            )
        )
        return (result as? ProviderResult.Success)?.value
    }

    private suspend fun fetchFromAniList(
        storedId: Int,
        malId: Int?,
        nowEpoch: Long,
        canonicalMissing: Boolean
    ): AnimeDetailEntity {
        // Kitsu/AnimeSchedule records use negative internal ids that AniList cannot resolve.
        val byId = if (storedId > 0) aniListDataSource.getAnimeDetail(storedId) else null
        // Only a definitive "not found" justifies a second lookup; after a network failure it
        // would just fail again.
        val notFound = byId == null || (byId is AppResult.Error && byId.error == AppError.NoCache)
        val media = when {
            byId is AppResult.Success -> byId.data
            notFound && malId != null ->
                aniListDataSource.getAnimeDetailByMalId(malId).requireProviderData("AniList")
            byId != null -> byId.requireProviderData("AniList")
            else -> throw ProviderCallException("AniList", message = "no AniList or MAL id")
        }
        val entity = media.toEntity(nowEpoch)
        return if (canonicalMissing) {
            entity.copy(malId = media.idMal ?: malId)
        } else {
            // Keep the primary key the caller already navigates with.
            entity.copy(animeId = storedId, malId = malId ?: media.idMal)
        }
    }
}
