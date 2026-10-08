package com.owlcoder.animeschedule.domain.model

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import kotlin.math.ceil

/** Match every word across fields, ignoring accents, case and repeated whitespace. */
class LocalTextQuery(query: String) {
    private val words = normalized(query).split(Regex("\\s+")).filter(String::isNotEmpty)
    fun matches(vararg values: String?): Boolean {
        if (words.isEmpty()) return true
        val text = values.filterNotNull().joinToString(" ", transform = ::normalized)
        return words.all(text::contains)
    }
    companion object {
        private val marks = Regex("\\p{M}+")
        private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(marks, "").replace('đ', 'd').replace('Đ', 'D').lowercase(Locale.ROOT)
    }
}

fun WatchTools.renameView(old: String, replacement: String): WatchTools {
    val name = replacement.trim().take(32)
    if (name.isEmpty() || savedViews.none { it.name == old } || savedViews.any { it.name != old && it.name.equals(name, true) }) return this
    return copy(savedViews = savedViews.map { if (it.name == old) it.copy(name = name) else it })
}

fun WatchTools.moveView(name: String, offset: Int): WatchTools {
    val index = savedViews.indexOfFirst { it.name == name }
    val target = index + offset
    if (index < 0 || offset !in setOf(-1, 1) || target !in savedViews.indices) return this
    val next = savedViews.toMutableList()
    val item = next.removeAt(index); next.add(target, item)
    return copy(savedViews = next)
}

data class ActivityTitle(val animeId: Int, val title: String, val episodes: Long)
data class ActivitySummary(
    val days: Int, val episodes: Long, val previousEpisodes: Long, val activeDays: Int,
    val trend: List<Pair<LocalDate, Int>>, val titles: List<ActivityTitle>,
) {
    val change: Long get() = episodes - previousEpisodes
    val dailyAverage: Double get() = episodes.toDouble() / days
}

/** Based only on locally retained history; no invented lifetime totals or future events. */
fun WatchTools.activitySummary(today: LocalDate, periodDays: Int): ActivitySummary {
    val span = periodDays.coerceIn(1, 90)
    val start = today.minusDays(span - 1L)
    val daily = dailyEpisodes(today)
    val previousStart = start.minusDays(span.toLong())
    val events = activity.mapNotNull { item ->
        runCatching { LocalDate.parse(item.date) }.getOrNull()?.takeIf { it in start..today }?.let { item }
    }
    val titles = events.groupBy { it.animeId }.map { (id, items) ->
        ActivityTitle(id, items.first().title, items.sumOf { it.episodeDelta.toLong() }.coerceAtLeast(0))
    }.filter { it.episodes > 0 }.sortedWith(compareByDescending<ActivityTitle> { it.episodes }.thenBy { it.title }.thenBy { it.animeId })
    return ActivitySummary(span, daily.filterKeys { it in start..today }.values.sumOf { it.toLong() },
        daily.filterKeys { it >= previousStart && it < start }.values.sumOf { it.toLong() },
        daily.count { (date, count) -> date in start..today && count > 0 },
        (6L downTo 0L).map { today.minusDays(it).let { date -> date to (daily[date] ?: 0) } }, titles)
}

/** Estimated calendar date at the last seven days' pace; unknown totals stay unknown. */
fun List<MalListEntry>.finishForecast(tools: WatchTools, today: LocalDate): LocalDate? {
    val watching = distinctBy { it.animeId }.filter { it.status == WatchStatus.WATCHING }
    if (watching.isEmpty() || watching.any { it.totalEpisodes == null || it.totalEpisodes <= 0 }) return null
    val left = watching.sumOf { (it.totalEpisodes!!.toLong() - it.episodesWatched.coerceAtLeast(0)).coerceAtLeast(0) }
    if (left == 0L) return today
    val pace = tools.activitySummary(today, 7).episodes
    return if (pace <= 0) null else runCatching { today.plusDays(ceil(left.toDouble() * 7 / pace).toLong()) }.getOrNull()
}
