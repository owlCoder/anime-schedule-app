package com.owlcoder.animeschedule.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil

/** Known totals only; Long arithmetic keeps large imported lists safe. */
fun MalListEntry.remainingMinutes(tools: WatchTools): Long? = totalEpisodes?.takeIf { it > 0 }
    ?.let { (it.toLong() - episodesWatched.coerceAtLeast(0)).coerceAtLeast(0) * tools.minutesFor(animeId) }

fun MalListEntry.matchesRating(minimum: Int, maximum: Int): Boolean = score in minimum.coerceIn(0, 10)..maximum.coerceIn(minimum.coerceIn(0, 10), 10)

data class BacklogGroup(val status: WatchStatus, val titles: Int, val episodes: Long, val minutes: Long, val unknown: Int) {
    fun daysAt(goal: Int): Long? = if (goal > 0) ceil(episodes.toDouble() / goal).toLong() else null
}

fun List<MalListEntry>.backlog(tools: WatchTools): List<BacklogGroup> =
    listOf(WatchStatus.WATCHING, WatchStatus.PLAN_TO_WATCH, WatchStatus.ON_HOLD).map { status ->
        val entries = distinctBy { it.animeId }.filter { it.status == status }
        BacklogGroup(status, entries.size, entries.sumOf {
            it.totalEpisodes?.takeIf { total -> total > 0 }?.let { total -> (total.toLong() - it.episodesWatched.coerceAtLeast(0)).coerceAtLeast(0) } ?: 0L
        }, entries.sumOf { it.remainingMinutes(tools) ?: 0L }, entries.count { it.totalEpisodes == null || it.totalEpisodes <= 0 })
    }

enum class ActivityRange { ALL, THIS_WEEK, LAST_30, LAST_90 }
fun WatchTools.activityInRange(query: String, range: ActivityRange, today: LocalDate): List<WatchActivity> {
    val start = when (range) {
        ActivityRange.ALL -> null
        ActivityRange.THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        ActivityRange.LAST_30 -> today.minusDays(29)
        ActivityRange.LAST_90 -> today.minusDays(89)
    }
    val search = query.trim()
    return activity.filter { item ->
        val date = runCatching { LocalDate.parse(item.date) }.getOrNull()
        (search.isBlank() || item.title.contains(search, true)) &&
            (start == null || date != null && date >= start && date <= today)
    }
}

/** One atomic local change, never a MAL list update. */
fun WatchTools.withMarkers(ids: Set<Int>, favorite: Boolean? = null, pin: Boolean? = null): WatchTools {
    val valid = ids.filter { it > 0 }.take(20_000).toSet()
    return copy(
        favorites = when (favorite) { true -> favorites + valid; false -> favorites - valid; null -> favorites },
        pinned = when (pin) { true -> pinned + valid; false -> pinned - valid; null -> pinned },
    )
}

/** Renaming merges existing labels without duplicates and also updates saved views. */
fun WatchTools.renameTag(old: String, replacement: String?): WatchTools {
    val label = replacement?.trim()?.take(24)?.takeIf { it.isNotEmpty() && ',' !in it }
    if (replacement != null && label == null) return this
    return copy(
        tags = tags.mapValues { (_, labels) ->
            normalizedTags(labels.mapNotNull { if (it.equals(old, true)) label else it }.joinToString(","))
        }.filterValues { it.isNotEmpty() },
        savedViews = savedViews.map { if (it.tag.equals(old, true)) it.copy(tag = label) else it },
    )
}
