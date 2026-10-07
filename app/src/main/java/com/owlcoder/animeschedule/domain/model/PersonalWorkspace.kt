package com.owlcoder.animeschedule.domain.model

import java.time.LocalDate
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
enum class SmartListFilter { ALL, PINNED, SHORT_SERIES, NEAR_FINISH, UNSTARTED }

@Serializable
data class SavedListView(
    val name: String,
    val query: String = "",
    val status: WatchStatus? = null,
    val favoritesOnly: Boolean = false,
    val unratedOnly: Boolean = false,
    val tag: String? = null,
    val smartFilter: SmartListFilter = SmartListFilter.ALL,
    val sort: String = "RECENT",
    val minimumScore: Int = 0,
    val maximumScore: Int = 10,
) {
    fun normalized() = copy(
        name = name.trim().take(32), query = query.trim().take(128),
        status = status?.takeUnless { it == WatchStatus.NOT_IN_LIST },
        tag = tag?.trim()?.take(24)?.takeIf { it.isNotBlank() },
        minimumScore = minimumScore.coerceIn(0, 10),
        maximumScore = maximumScore.coerceIn(minimumScore.coerceIn(0, 10), 10),
        sort = sort.takeIf { it in setOf("RECENT", "TITLE", "SCORE", "PROGRESS", "REMAINING", "WATCH_TIME") } ?: "RECENT",
    )
}

fun List<SavedListView>.normalizedViews(): List<SavedListView> = map { it.normalized() }
    .filter { it.name.isNotEmpty() }.distinctBy { it.name.lowercase(Locale.ROOT) }.take(8)

fun WatchTools.minutesFor(animeId: Int): Int = (durationOverrides[animeId] ?: episodeMinutes).coerceIn(1, 180)

fun MalListEntry.matchesSmart(filter: SmartListFilter, tools: WatchTools): Boolean {
    val remaining = totalEpisodes?.takeIf { it > 0 }?.let { (it - episodesWatched.coerceAtLeast(0)).coerceAtLeast(0) }
    val unfinished = status != WatchStatus.COMPLETED && status != WatchStatus.DROPPED
    return when (filter) {
        SmartListFilter.ALL -> true
        SmartListFilter.PINNED -> animeId in tools.pinned
        SmartListFilter.SHORT_SERIES -> totalEpisodes in 1..13 && unfinished
        SmartListFilter.NEAR_FINISH -> remaining in 1..3 && unfinished
        SmartListFilter.UNSTARTED -> episodesWatched == 0 && unfinished
    }
}

/** Corrections subtract from that day's total; future and malformed dates do not affect streaks. */
fun WatchTools.dailyEpisodes(today: LocalDate): Map<LocalDate, Int> = activity.mapNotNull {
    runCatching { LocalDate.parse(it.date) }.getOrNull()?.takeUnless { it.isAfter(today) }?.let { date -> date to it.episodeDelta }
}.groupBy({ it.first }, { it.second }).mapValues { (_, deltas) -> deltas.sumOf { it.toLong() }.coerceIn(0, Int.MAX_VALUE.toLong()).toInt() }

data class WatchStreak(val current: Int, val best: Int)
fun WatchTools.streak(today: LocalDate): WatchStreak {
    val days = dailyEpisodes(today).filterValues { it > 0 }.keys.sorted()
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    days.forEach { date -> run = if (previous?.plusDays(1) == date) run + 1 else 1; best = maxOf(best, run); previous = date }
    var cursor = if (today in days) today else today.minusDays(1)
    var current = 0
    val watched = days.toSet()
    while (cursor in watched) { current++; cursor = cursor.minusDays(1) }
    return WatchStreak(current, best)
}

enum class PlannerStrategy { BALANCED, FINISH_FIRST, FOCUS }

data class WatchPlanItem(val entry: MalListEntry, val episodes: Int, val minutes: Int)

/** Balanced rounds share episodes; focused modes fill each title in priority order. All modes respect the budget and known remainder. */
fun planWatchSession(entries: List<MalListEntry>, tools: WatchTools, budgetMinutes: Int, strategy: PlannerStrategy = PlannerStrategy.BALANCED, excluded: Set<Int> = emptySet()): List<WatchPlanItem> {
    val candidates = entries.distinctBy { it.animeId }.filter {
        it.animeId > 0 && it.animeId !in excluded && it.status == WatchStatus.WATCHING && (it.totalEpisodes == null || it.totalEpisodes <= 0 || it.episodesWatched < it.totalEpisodes)
    }.let { candidates ->
        if (strategy == PlannerStrategy.FINISH_FIRST) candidates.sortedWith(compareBy<MalListEntry> { it.remainingMinutes(tools) ?: Long.MAX_VALUE }.thenByDescending { it.animeId in tools.pinned })
        else candidates.sortedByDescending { it.animeId in tools.pinned }
    }
    val counts = linkedMapOf<Int, Int>()
    var left = budgetMinutes.coerceIn(0, 480)
    if (strategy != PlannerStrategy.BALANCED) {
        candidates.forEach { entry ->
            val duration = tools.minutesFor(entry.animeId)
            val remaining = entry.totalEpisodes?.takeIf { it > 0 }?.let { (it - entry.episodesWatched.coerceAtLeast(0)).coerceAtLeast(0) } ?: Int.MAX_VALUE
            val count = minOf(left / duration, remaining)
            if (count > 0) { counts[entry.animeId] = count; left -= count * duration }
        }
    } else do {
        var added = false
        candidates.forEach { entry ->
            val duration = tools.minutesFor(entry.animeId)
            val count = counts[entry.animeId] ?: 0
            val remaining = entry.totalEpisodes?.takeIf { it > 0 }?.let { (it - entry.episodesWatched.coerceAtLeast(0)).coerceAtLeast(0) }
            if (duration <= left && (remaining == null || count < remaining)) {
                counts[entry.animeId] = count + 1; left -= duration; added = true
            }
        }
    } while (added)
    return candidates.mapNotNull { entry -> counts[entry.animeId]?.let { count -> WatchPlanItem(entry, count, count * tools.minutesFor(entry.animeId)) } }
}
