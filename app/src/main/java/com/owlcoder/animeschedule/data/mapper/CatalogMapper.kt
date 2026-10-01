package com.owlcoder.animeschedule.data.mapper

import com.owlcoder.animeschedule.data.api.alternative.CatalogAnime
import com.owlcoder.animeschedule.data.api.alternative.toInternalId
import com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem

fun CatalogAnime.toDetailEntity(internalId: Int, nowEpoch: Long): AnimeDetailEntity =
    AnimeDetailEntity(
        animeId = internalId,
        malId = malId,
        titleRomaji = titleRomaji,
        titleEnglish = titleEnglish,
        titleNative = null,
        coverImageUrl = coverImageUrl,
        coverColor = null,
        bannerImageUrl = bannerImageUrl,
        description = description,
        genres = genres,
        averageScore = averageScore,
        meanScore = averageScore,
        episodes = episodes,
        duration = duration,
        status = status,
        format = format,
        season = season,
        seasonYear = seasonYear,
        nextAiringEpisode = null,
        nextAiringAt = nextAiringAt,
        studiosJson = null,
        charactersJson = null,
        relationsJson = null,
        trailerSite = null,
        trailerId = null,
        siteUrl = siteUrl,
        cachedAtEpochSeconds = nowEpoch
    )

fun CatalogAnime.toSearchResult(userListEntry: MalListEntry?): AnimeSearchResult = AnimeSearchResult(
    anilistId = toInternalId(),
    malId = malId,
    title = title,
    titleEnglish = titleEnglish,
    coverImageUrl = coverImageUrl,
    type = format,
    year = seasonYear?.toString(),
    meanScore = averageScore?.toDouble(),
    totalEpisodes = episodes,
    userListEntry = userListEntry
)

fun CatalogAnime.toSeasonalItem(): SeasonalAnimeItem = SeasonalAnimeItem(
    anilistId = toInternalId(),
    malId = malId,
    title = title,
    coverImageUrl = coverImageUrl,
    coverColor = null,
    genres = genres,
    format = format,
    status = status,
    episodes = episodes,
    season = season,
    seasonYear = seasonYear,
    averageScore = averageScore,
    meanScore = averageScore
)
