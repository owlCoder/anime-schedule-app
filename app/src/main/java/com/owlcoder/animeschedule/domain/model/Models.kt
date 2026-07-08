package com.owlcoder.animeschedule.domain.model

import java.time.LocalDate

data class MalListEntry(
    val animeId: Int,
    val title: String = "",
    val coverImageUrl: String? = null,
    val status: WatchStatus,
    val episodesWatched: Int,
    val score: Int,
    val totalEpisodes: Int?,
    /** ISO-8601 timestamp of the entry's last update on MAL (episode progress, status, score
     *  change, etc.) — drives the Schedule home "Recently changed" section. Null for entries
     *  synthesized outside the MAL list sync (e.g. search results before the user adds them). */
    val updatedAt: String? = null
)

data class AiringEpisode(
    val airingId: Int,
    val animeId: Int,
    val malId: Int?,
    val episode: Int,
    val airingAtEpochSeconds: Long,
    val title: String,
    val titleRomaji: String?,
    val coverImageUrl: String?,
    val coverColor: String?,
    val genres: List<String>,
    val averageScore: Int?,
    val totalEpisodes: Int?,
    val status: String?,
    val format: String?,
    val malListEntry: MalListEntry?
)

data class ScheduleDay(
    val date: LocalDate,
    val episodes: List<AiringEpisode>
)

data class RelatedAnime(
    val animeId: Int,
    val title: String,
    val coverImageUrl: String?,
    val format: String?,
    val status: String?,
    val relationType: String?
)

data class Studio(val id: Int, val name: String, val isMain: Boolean)

data class AnimeDetail(
    val animeId: Int,
    val malId: Int?,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val coverImageUrl: String?,
    val coverColor: String?,
    val bannerImageUrl: String?,
    val description: String?,
    val genres: List<String>,
    val averageScore: Int?,
    val meanScore: Int?,
    val episodes: Int?,
    val duration: Int?,
    val status: String?,
    val format: String?,
    val season: String?,
    val seasonYear: Int?,
    val nextAiringEpisode: Int?,
    val nextAiringAt: Long?,
    val studios: List<Studio>,
    val relations: List<RelatedAnime>,
    val trailerSite: String?,
    val trailerId: String?,
    val siteUrl: String?,
    val malListEntry: MalListEntry?
)

data class MalListUpdate(
    val status: WatchStatus? = null,
    val episodesWatched: Int? = null,
    val score: Int? = null
)

/** One page of search results plus whether more pages exist — drives infinite scroll. */
data class SearchPage(
    val results: List<AnimeSearchResult>,
    val hasNextPage: Boolean
)

data class AnimeSearchResult(
    val anilistId: Int,
    val malId: Int?,
    val title: String,
    val titleEnglish: String?,
    val coverImageUrl: String?,
    val type: String?,
    val year: String?,
    val meanScore: Double?,
    val totalEpisodes: Int?,
    val userListEntry: MalListEntry?
)

enum class AnimeSeason {
    WINTER, SPRING, SUMMER, FALL
}

data class SeasonalAnimeItem(
    val anilistId: Int,
    val malId: Int?,
    val title: String,
    val coverImageUrl: String?,
    val coverColor: String?,
    val genres: List<String>,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val season: String?,
    val seasonYear: Int?,
    val averageScore: Int?,
    val meanScore: Int?
)

data class AppNotification(
    val id: Int,
    val animeId: Int,
    val title: String,
    val episode: Int,
    val coverImageUrl: String?,
    val airingAtEpochSeconds: Long,
    val isRead: Boolean,
    val createdAtEpochSeconds: Long
)
