package com.owlcoder.animeschedule.domain.model

import com.owlcoder.animeschedule.presentation.screens.mylist.*
import com.owlcoder.animeschedule.presentation.screens.discovery.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class Workspace5120Test {
    private val today = LocalDate.of(2026, 10, 8)
    private fun entry(id: Int = 1, status: WatchStatus = WatchStatus.WATCHING, watched: Int = 4, total: Int? = 12, score: Int = 0) =
        MalListEntry(id, "Title $id", status = status, episodesWatched = watched, score = score, totalEpisodes = total)

    @Test fun `word matching spans notes and title without depending on accents or whitespace`() {
        assertTrue(LocalTextQuery("  CUVAJ    dragon  ").matches("Dragon", "Čuvaj za vikend"))
        assertTrue(LocalTextQuery("dorde").matches("Đorđe"))
        assertFalse(LocalTextQuery("dragon film").matches("Dragon", "Čuvaj za vikend"))
        assertTrue(LocalTextQuery("").matches(null))
    }
    @Test fun `smart filters distinguish unknown totals incomplete progress notes tags and unrated completion`() {
        val tools = WatchTools(notes = mapOf(1 to "note"), tags = mapOf(1 to setOf("Weekend")))
        assertTrue(entry().matchesSmart(SmartListFilter.WITH_NOTES, tools))
        assertFalse(entry().matchesSmart(SmartListFilter.UNTAGGED, tools))
        assertTrue(entry(2).matchesSmart(SmartListFilter.UNTAGGED, tools))
        assertTrue(entry(total = 27).matchesSmart(SmartListFilter.LONG_SERIES, tools))
        assertFalse(entry(total = 26).matchesSmart(SmartListFilter.LONG_SERIES, tools))
        assertFalse(entry(watched = 27, total = 27).matchesSmart(SmartListFilter.LONG_SERIES, tools))
        assertTrue(entry(total = null).matchesSmart(SmartListFilter.UNKNOWN_LENGTH, tools))
        assertTrue(entry().matchesSmart(SmartListFilter.IN_PROGRESS, tools))
        assertFalse(entry(watched = 12).matchesSmart(SmartListFilter.IN_PROGRESS, tools))
        assertTrue(entry(total = null).matchesSmart(SmartListFilter.IN_PROGRESS, tools))
        assertFalse(entry(status = WatchStatus.COMPLETED).matchesSmart(SmartListFilter.IN_PROGRESS, tools))
        assertTrue(entry(status = WatchStatus.COMPLETED).matchesSmart(SmartListFilter.COMPLETED_UNRATED, tools))
        assertFalse(entry(status = WatchStatus.COMPLETED, score = 8).matchesSmart(SmartListFilter.COMPLETED_UNRATED, tools))
    }
    @Test fun `view rename and reorder preserve filters and reject collisions`() {
        val view = SavedListView("First", query = "Dragon", smartFilter = SmartListFilter.WITH_NOTES, sort = "OLDEST")
        val tools = WatchTools(savedViews = listOf(view, SavedListView("Second")))
        assertEquals(tools, tools.renameView("First", "second"))
        assertEquals(tools, tools.moveView("First", -1))
        val changed = tools.renameView("First", " Weekend ").moveView("Weekend", 1)
        assertEquals(listOf("Second", "Weekend"), changed.savedViews.map { it.name })
        assertEquals(view.copy(name = "Weekend"), changed.savedViews.last())
        assertEquals(changed.savedViews, PersonalBackup.decode(PersonalBackup(tools = changed).encode()).tools.savedViews)
    }
    @Test fun `planner breaks fit the budget and only requested unfinished statuses are included`() {
        val entries = listOf(entry(total = 5), entry(2, WatchStatus.ON_HOLD, 0, 1), entry(3, WatchStatus.PLAN_TO_WATCH, 0, 1), entry(4, WatchStatus.COMPLETED, 0, 12))
        val allowed = setOf(WatchStatus.WATCHING, WatchStatus.ON_HOLD, WatchStatus.PLAN_TO_WATCH, WatchStatus.COMPLETED)
        PlannerStrategy.entries.forEach { strategy ->
            val plan = planWatchSession(entries, WatchTools(), 90, strategy, statuses = allowed, breakMinutes = 15)
            val count = plan.sumOf { it.episodes }
            assertEquals(2, count)
            assertTrue(plan.sumOf { it.minutes } + (count - 1).coerceAtLeast(0) * 15 <= 90)
            assertFalse(plan.any { it.entry.status == WatchStatus.COMPLETED })
        }
        assertEquals(1, planWatchSession(entries, WatchTools(), 24, breakMinutes = 30).sumOf { it.episodes })
        assertTrue(planWatchSession(entries, WatchTools(), 23, breakMinutes = 30).isEmpty())
        assertTrue(planWatchSession(entries, WatchTools(), 480, statuses = emptySet()).isEmpty())
    }
    @Test fun `period reports handle corrections boundaries and unknown or future dates`() {
        val tools = WatchTools(activity = listOf(
            WatchActivity(1, "Alpha", today.toString(), 4, 4), WatchActivity(1, "Alpha", today.toString(), -1, 3),
            WatchActivity(2, "Beta", today.minusDays(6).toString(), 2, 2), WatchActivity(2, "Beta", today.minusDays(7).toString(), 3, 5),
            WatchActivity(3, "Future", today.plusDays(1).toString(), 100, 100), WatchActivity(3, "Broken", "bad", 100, 100)))
        val summary = tools.activitySummary(today, 7)
        assertEquals(5L, summary.episodes); assertEquals(3L, summary.previousEpisodes); assertEquals(2L, summary.change)
        assertEquals(2, summary.activeDays); assertEquals(7, summary.trend.size)
        assertEquals(listOf(1, 2), summary.titles.map { it.animeId })
        assertEquals(3L, summary.titles.first().episodes)
    }
    @Test fun `finish forecast uses known watching remainder and weekly pace`() {
        val tools = WatchTools(activity = listOf(WatchActivity(1, "A", today.toString(), 7, 7)))
        assertEquals(today.plusDays(8), listOf(entry()).finishForecast(tools, today))
        assertNull(listOf(entry(total = null)).finishForecast(tools, today))
        assertNull(listOf(entry()).finishForecast(WatchTools(), today))
        assertEquals(today, listOf(entry(watched = 12)).finishForecast(tools, today))
    }
    @Test fun `new sorts keep unknown values last and preserve pin priority in one order`() {
        val entries = listOf(entry(1, score = 8).copy(updatedAt = "2026-10-08T00:00:00Z"), entry(2, score = 3).copy(updatedAt = "2025-01-01T00:00:00Z"), entry(3))
        assertEquals(listOf(2, 1, 3), entries.sortedFor(MyListSortOrder.OLDEST).map { it.animeId })
        assertEquals(listOf(2, 1, 3), entries.sortedFor(MyListSortOrder.LOWEST_SCORE).map { it.animeId })
        assertEquals(listOf(3, 2, 1), entries.sortedFor(MyListSortOrder.LOWEST_SCORE, WatchTools(pinned = setOf(3)), true).map { it.animeId })
    }
    @Test fun `search upper score year and live status filters preserve unknown metadata rules`() {
        fun item(id: Int, year: String?, score: Double?, status: WatchStatus?) = AnimeSearchResult(id, id, "Title $id", null, null, "TV", year, score, 12, status?.let { entry(id, it) })
        val entries = listOf(item(1, "2024", 78.0, WatchStatus.WATCHING), item(2, "2026", 65.0, WatchStatus.ON_HOLD), item(3, null, null, null))
        assertEquals(listOf(2, 1, 3), entries.discover(SearchFilter(sort = SearchSort.NEWEST)).map { it.anilistId })
        assertEquals(listOf(2), entries.discover(SearchFilter(maximumScore = 7)).map { it.anilistId })
        assertEquals(listOf(1), entries.discover(SearchFilter(watchStatus = WatchStatus.WATCHING)).map { it.anilistId })
    }
}
