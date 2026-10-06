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
