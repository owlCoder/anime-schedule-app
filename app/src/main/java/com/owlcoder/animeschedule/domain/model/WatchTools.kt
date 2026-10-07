package com.owlcoder.animeschedule.domain.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Serializable
data class WatchActivity(
    val animeId: Int,
    val title: String,
    val date: String,
    val episodeDelta: Int,
    val progress: Int,
)

@Serializable
data class WatchTools(
    val favorites: Set<Int> = emptySet(),
    val notes: Map<Int, String> = emptyMap(),
    val activity: List<WatchActivity> = emptyList(),
    val weeklyGoal: Int = 12,
    val tags: Map<Int, Set<String>> = emptyMap(),
    val episodeMinutes: Int = 24,
    val pinned: Set<Int> = emptySet(),
    val dailyGoal: Int = 3,
    val durationOverrides: Map<Int, Int> = emptyMap(),
    val savedViews: List<SavedListView> = emptyList(),
) {
    fun episodesThisWeek(today: LocalDate): Int {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return activity.filter {
            val date = runCatching { LocalDate.parse(it.date) }.getOrNull()
            date != null && !date.isBefore(monday) && !date.isAfter(today)
        }.sumOf { it.episodeDelta }.coerceAtLeast(0)
    }

    fun withProgress(
        animeId: Int,
        title: String,
        before: Int,
        after: Int,
        date: LocalDate
    ): WatchTools {
        if (before == after) return this
        return copy(
            activity = (listOf(
                WatchActivity(
                    animeId,
                    title,
                    date.toString(),
                    after - before,
                    after
                )
            ) + activity).take(300)
        )
    }
}

fun WatchTools.filteredActivity(query: String, thisWeek: Boolean, today: LocalDate): List<WatchActivity> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val search = query.trim()
    return activity.filter { item ->
        val date = runCatching { LocalDate.parse(item.date) }.getOrNull()
        (search.isBlank() || item.title.contains(search, true)) && (!thisWeek || date != null && !date.isBefore(monday) && !date.isAfter(today))
    }
}
