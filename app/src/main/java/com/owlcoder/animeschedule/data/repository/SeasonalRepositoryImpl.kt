package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.api.anilist.generated.type.MediaSeason
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import com.owlcoder.animeschedule.domain.repository.SeasonalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeasonalRepositoryImpl @Inject constructor(
    private val remoteDataSource: AniListRemoteDataSource
) : SeasonalRepository {

    override suspend fun getSeasonalAnime(
        season: AnimeSeason,
        year: Int
    ): AppResult<List<SeasonalAnimeItem>> {
        val mediaSeason = when (season) {
            AnimeSeason.WINTER -> MediaSeason.WINTER
            AnimeSeason.SPRING -> MediaSeason.SPRING
            AnimeSeason.SUMMER -> MediaSeason.SUMMER
            AnimeSeason.FALL   -> MediaSeason.FALL
        }
        return when (val result = remoteDataSource.getSeasonalAnime(mediaSeason, year)) {
            is AppResult.Success -> AppResult.Success(result.data.map { m ->
                SeasonalAnimeItem(
                    anilistId = m.id,
                    malId = m.idMal,
                    title = m.title?.english ?: m.title?.romaji ?: "Unknown",
                    coverImageUrl = m.coverImage?.large,
                    coverColor = m.coverImage?.color,
                    genres = m.genres?.filterNotNull() ?: emptyList(),
                    format = m.format?.rawValue,
                    status = m.status?.rawValue,
                    episodes = m.episodes,
                    season = m.season?.rawValue,
                    seasonYear = m.seasonYear,
                    averageScore = m.averageScore,
                    meanScore = m.meanScore
                )
            })
            is AppResult.Error -> result
        }
    }
}
