package com.owlcoder.animeschedule.presentation.screens.mylist

import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.domain.model.WatchTools
import com.owlcoder.animeschedule.domain.model.remainingMinutes
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

enum class MyListSortOrder(val labelRes: Int) {
    RECENT(R.string.mylist_sort_recent),
    TITLE(R.string.mylist_sort_title),
    SCORE(R.string.mylist_sort_score),
    PROGRESS(R.string.mylist_sort_progress),
    REMAINING(R.string.mylist_sort_remaining),
    WATCH_TIME(R.string.sort_watch_time),
    OLDEST(R.string.sort_oldest_edit),
    LOWEST_SCORE(R.string.sort_lowest_score),
}

data class MyListInsights(
    val totalAnime: Int = 0,
    val completedAnime: Int = 0,
    val watchedEpisodes: Long = 0,
    val remainingEpisodes: Long = 0,
    val ratedAnime: Int = 0,
    val averageScore: Double? = null,
)

internal fun List<MalListEntry>.insights(): MyListInsights {
    val scores = map { it.score }.filter { it in 1..10 }
    return MyListInsights(
        totalAnime = size,
        completedAnime = count { it.status == WatchStatus.COMPLETED },
        watchedEpisodes = sumOf { it.episodesWatched.coerceAtLeast(0).toLong() },
        // Only known totals on Watching count toward the current backlog.
        remainingEpisodes = filter { it.status == WatchStatus.WATCHING }.sumOf {
            it.totalEpisodes?.takeIf { total -> total > 0 }
                ?.let { total -> (total - it.episodesWatched.coerceAtLeast(0)).coerceAtLeast(0).toLong() } ?: 0L
        },
        ratedAnime = scores.size,
        averageScore = scores.takeIf { it.isNotEmpty() }?.average(),
    )
}

internal fun List<MalListEntry>.sortedFor(order: MyListSortOrder, tools: WatchTools = WatchTools(), pinsFirst: Boolean = false): List<MalListEntry> {
    val titleOrder = compareBy<MalListEntry> { it.title.lowercase(Locale.ROOT) }.thenBy { it.animeId }
    val timestamps = if (order == MyListSortOrder.RECENT || order == MyListSortOrder.OLDEST) associate { it.animeId to it.updatedAt.epochOrZero() } else emptyMap()
    val comparator = when (order) {
        MyListSortOrder.TITLE -> titleOrder
        MyListSortOrder.RECENT -> compareByDescending<MalListEntry> { timestamps[it.animeId] ?: 0 }.then(titleOrder)
        MyListSortOrder.OLDEST -> compareBy<MalListEntry> { timestamps[it.animeId]?.takeIf { epoch -> epoch > 0 } ?: Long.MAX_VALUE }.then(titleOrder)
        MyListSortOrder.SCORE -> compareByDescending<MalListEntry> { it.score }.then(titleOrder)
        MyListSortOrder.LOWEST_SCORE -> compareBy<MalListEntry> { it.score.takeIf { score -> score in 1..10 } ?: Int.MAX_VALUE }.then(titleOrder)
        MyListSortOrder.PROGRESS -> compareByDescending<MalListEntry> {
            it.totalEpisodes?.takeIf { total -> total > 0 }
                ?.let { total -> it.episodesWatched.coerceIn(0, total).toDouble() / total } ?: -1.0
        }.then(titleOrder)
        MyListSortOrder.WATCH_TIME -> compareBy<MalListEntry> { it.remainingMinutes(tools) ?: Long.MAX_VALUE }.then(titleOrder)
        MyListSortOrder.REMAINING -> compareBy<MalListEntry> {
            it.totalEpisodes?.takeIf { total -> total > 0 }
                ?.let { total -> (total - it.episodesWatched.coerceAtLeast(0)).coerceAtLeast(0) } ?: Int.MAX_VALUE
        }.then(titleOrder)
    }
    return sortedWith(if (pinsFirst) compareByDescending<MalListEntry> { it.animeId in tools.pinned }.then(comparator) else comparator)
}

private fun String?.epochOrZero(): Long {
    if (isNullOrBlank()) return 0
    return runCatching { Instant.parse(this).epochSecond }
        .getOrElse { runCatching { OffsetDateTime.parse(this).toEpochSecond() }.getOrDefault(0) }
}

/** The shortcut considers the entire list rather than the current search/filter. */
internal fun List<MalListEntry>.continueWatching(): MalListEntry? = filter {
    it.status == WatchStatus.WATCHING && (it.totalEpisodes == null || it.totalEpisodes <= 0 || it.episodesWatched < it.totalEpisodes)
}.sortedFor(MyListSortOrder.RECENT).firstOrNull()
