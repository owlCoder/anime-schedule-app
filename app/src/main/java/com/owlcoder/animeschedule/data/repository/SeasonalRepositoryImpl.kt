package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.alternative.AlternativeAnimeDataSource
import com.owlcoder.animeschedule.data.api.alternative.CatalogAnime
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.api.anilist.generated.type.MediaSeason
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.offline.OfflineCatalogDataSource
import com.owlcoder.animeschedule.data.provider.ProviderCall
import com.owlcoder.animeschedule.data.provider.ProviderOperation
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import com.owlcoder.animeschedule.data.provider.ProviderResult
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import com.owlcoder.animeschedule.domain.repository.SeasonalRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import com.owlcoder.animeschedule.data.api.alternative.toInternalId
import com.owlcoder.animeschedule.data.provider.requireProviderData
import com.owlcoder.animeschedule.data.mapper.toDetailEntity
import com.owlcoder.animeschedule.data.mapper.toSeasonalItem

@Singleton
class SeasonalRepositoryImpl @Inject constructor(
    private val remoteDataSource: AniListRemoteDataSource,
    private val alternativeDataSource: AlternativeAnimeDataSource,
    private val animeDetailDao: AnimeDetailDao,
    private val providerOrchestrator: ProviderOrchestrator,
    private val offlineCatalogDataSource: OfflineCatalogDataSource
) : SeasonalRepository {

    override suspend fun getSeasonalAnime(
        season: AnimeSeason,
        year: Int
    ): AppResult<List<SeasonalAnimeItem>> {
        val result = providerOrchestrator.firstSuccessful(
            operation = ProviderOperation.SEASON,
            calls = listOf(
                ProviderCall("AniList", isUsable = { it.isNotEmpty() }) {
                    remoteDataSource.getSeasonalAnime(season.toMediaSeason(), year)
                        .requireProviderData("AniList")
                        // Popularity-sorted pages can repeat a title; ids are used as list keys.
                        .distinctBy { it.id }
                        .map { media ->
                            SeasonalAnimeItem(
                                anilistId = media.id,
                                malId = media.idMal,
                                title = media.title?.english ?: media.title?.romaji ?: "Unknown",
                                coverImageUrl = media.coverImage?.large,
                                coverColor = media.coverImage?.color,
                                genres = media.genres?.filterNotNull() ?: emptyList(),
                                format = media.format?.rawValue,
                                status = media.status?.rawValue,
                                episodes = media.episodes,
                                season = media.season?.rawValue,
                                seasonYear = media.seasonYear,
                                averageScore = media.averageScore,
                                meanScore = media.meanScore
                            )
                        }
                },
                ProviderCall("Kitsu", isUsable = { it.isNotEmpty() }) {
                    alternativeDataSource.getKitsuSeason(season.name, year).toSeasonalItemsAndCache()
                },
                ProviderCall("AnimeSchedule", isUsable = { it.isNotEmpty() }) {
                    alternativeDataSource.getAnimeScheduleSeason(season.name, year).toSeasonalItemsAndCache()
                }
            )
        )

        return when (result) {
            is ProviderResult.Success -> AppResult.Success(result.value)
            is ProviderResult.Exhausted -> when (val offline = offlineCatalogDataSource.getSeason(season, year)) {
                is AppResult.Success -> AppResult.Success(offline.data)
                is AppResult.Error -> AppResult.Error(
                    result.failures.lastOrNull()?.message?.let(AppError::Network) ?: offline.error
                )
            }
        }
    }

    private fun AnimeSeason.toMediaSeason() = when (this) {
        AnimeSeason.WINTER -> MediaSeason.WINTER
        AnimeSeason.SPRING -> MediaSeason.SPRING
        AnimeSeason.SUMMER -> MediaSeason.SUMMER
        AnimeSeason.FALL -> MediaSeason.FALL
    }

    private suspend fun List<CatalogAnime>.toSeasonalItemsAndCache(): List<SeasonalAnimeItem> {
        val now = Instant.now().epochSecond
        val unique = distinctBy { it.toInternalId() }
        animeDetailDao.insertIfAbsent(unique.map { it.toDetailEntity(it.toInternalId(), now) })
        return unique.map { it.toSeasonalItem() }
    }
}
