package com.owlcoder.animeschedule.presentation.screens.discovery

import androidx.annotation.StringRes
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import java.util.Locale

/** Filters operate on loaded catalog pages; unknown metadata never satisfies a numeric limit. */
enum class TrackingFilter(@StringRes val labelRes: Int) {
    ALL(R.string.discovery_all_titles), TRACKED(R.string.discovery_tracked), UNTRACKED(R.string.discovery_untracked)
}
enum class SearchSort(@StringRes val labelRes: Int) {
    RELEVANCE(R.string.discovery_relevance), TITLE(R.string.seasonal_sort_title),
    SCORE(R.string.seasonal_sort_score), EPISODES(R.string.discovery_shortest)
}
data class SearchFilter(
    val tracking: TrackingFilter = TrackingFilter.ALL,
    val formats: Set<String> = emptySet(),
    val sort: SearchSort = SearchSort.RELEVANCE,
) {
    val isActive get() = tracking != TrackingFilter.ALL || formats.isNotEmpty() || sort != SearchSort.RELEVANCE
}
internal fun AnimeSearchResult.communityScore(): Double? = meanScore?.takeIf { it > 0.0 }?.let { if (it > 10) it / 10 else it }
internal fun List<AnimeSearchResult>.discover(filter: SearchFilter): List<AnimeSearchResult> {
    val result = filter { item ->
        (filter.formats.isEmpty() || item.type?.uppercase(Locale.ROOT) in filter.formats) &&
            when (filter.tracking) {
                TrackingFilter.ALL -> true
                TrackingFilter.TRACKED -> item.userListEntry != null
                TrackingFilter.UNTRACKED -> item.userListEntry == null
            }
    }
    return when (filter.sort) {
        SearchSort.RELEVANCE -> result
        SearchSort.TITLE -> result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        SearchSort.SCORE -> result.sortedByDescending { it.communityScore() ?: -1.0 }
        SearchSort.EPISODES -> result.sortedBy { it.totalEpisodes?.takeIf { n -> n > 0 } ?: Int.MAX_VALUE }
    }
}
enum class ReleaseFilter(@StringRes val labelRes: Int, val status: String?) {
    ALL(R.string.discovery_any_status, null), AIRING(R.string.discovery_airing, "RELEASING"),
    FINISHED(R.string.discovery_finished, "FINISHED"), UPCOMING(R.string.discovery_upcoming, "NOT_YET_RELEASED");
    fun matches(value: String?): Boolean {
        val normalized = when (val raw = value?.trim()?.uppercase(Locale.ROOT)?.replace(' ', '_')) {
            "CURRENT", "AIRING", "ONGOING", "CURRENTLY_AIRING" -> "RELEASING"
            "UPCOMING", "TBA", "UNRELEASED" -> "NOT_YET_RELEASED"
            "COMPLETED" -> "FINISHED"
            else -> raw
        }
        return status == null || status == normalized
    }
}
enum class EpisodeLength(@StringRes val labelRes: Int) {
    ANY(R.string.discovery_any_length), SHORT(R.string.discovery_short_length),
    STANDARD(R.string.discovery_standard_length), LONG(R.string.discovery_long_length)
    ;
    fun matches(episodes: Int?): Boolean = when (this) {
        ANY -> true
        SHORT -> episodes != null && episodes in 1..13
        STANDARD -> episodes != null && episodes in 14..26
        LONG -> episodes != null && episodes >= 27
    }
}
internal fun adjacentSeason(season: AnimeSeason, year: Int, direction: Int): Pair<AnimeSeason, Int> {
    val index = year * 4 + season.ordinal + direction.coerceIn(-1, 1)
    return AnimeSeason.entries[Math.floorMod(index, 4)] to Math.floorDiv(index, 4)
}
