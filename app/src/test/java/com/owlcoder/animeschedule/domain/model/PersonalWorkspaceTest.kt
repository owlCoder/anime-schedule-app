package com.owlcoder.animeschedule.domain.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PersonalWorkspaceTest {
    private fun entry(id: Int = 1, watched: Int = 0, total: Int? = 12, status: WatchStatus = WatchStatus.WATCHING) =
        MalListEntry(id, "Title $id", status = status, episodesWatched = watched, score = 0, totalEpisodes = total)

    @Test fun `smart filters reject unknown lengths and completed titles`() {
        val tools = WatchTools(pinned = setOf(2))
        assertTrue(entry(total = 13).matchesSmart(SmartListFilter.SHORT_SERIES, tools))
        assertFalse(entry(total = 14).matchesSmart(SmartListFilter.SHORT_SERIES, tools))
        assertFalse(entry(total = null).matchesSmart(SmartListFilter.SHORT_SERIES, tools))
        assertTrue(entry(watched = 9).matchesSmart(SmartListFilter.NEAR_FINISH, tools))
        assertFalse(entry(watched = 8).matchesSmart(SmartListFilter.NEAR_FINISH, tools))
        assertFalse(entry(watched = 12).matchesSmart(SmartListFilter.NEAR_FINISH, tools))
        assertFalse(entry(status = WatchStatus.COMPLETED).matchesSmart(SmartListFilter.SHORT_SERIES, tools))
        assertFalse(entry(status = WatchStatus.DROPPED).matchesSmart(SmartListFilter.UNSTARTED, tools))
        assertTrue(entry(status = WatchStatus.PLAN_TO_WATCH).matchesSmart(SmartListFilter.UNSTARTED, tools))
        assertTrue(entry(id = 2).matchesSmart(SmartListFilter.PINNED, tools))
    }

    @Test fun `planner prioritizes pins distributes episodes and respects known remainder`() {
        val tools = WatchTools(pinned = setOf(2), durationOverrides = mapOf(2 to 12))
        val plan = planWatchSession(listOf(entry(1), entry(2, 11), entry(1), entry(3, status = WatchStatus.PLAN_TO_WATCH), entry(4, 12)), tools, 60)
        assertEquals(listOf(2, 1), plan.map { it.entry.animeId })
        assertEquals(listOf(1, 2), plan.map { it.episodes })
        assertEquals(60, plan.sumOf { it.minutes })
        val fair = planWatchSession(listOf(entry(1), entry(2)), WatchTools(), 72)
        assertEquals(listOf(2, 1), fair.map { it.episodes })
    }

    @Test fun `planner stays bounded with unknown totals tiny durations and extreme budgets`() {
        val entries = listOf(entry(total = null), entry(2, total = 0), entry(-1))
        assertTrue(planWatchSession(entries, WatchTools(), 23).isEmpty())
        assertTrue(planWatchSession(entries, WatchTools(), -1).isEmpty())
        val plan = planWatchSession(entries, WatchTools(episodeMinutes = 0), Int.MAX_VALUE)
        assertEquals(480, plan.sumOf { it.minutes })
        assertEquals(480, plan.sumOf { it.episodes })
        assertEquals(180, WatchTools(durationOverrides = mapOf(1 to 999)).minutesFor(1))
    }

    @Test fun `daily activity accounts for corrections invalid dates and future dates`() {
        val today = LocalDate.of(2026, 10, 7)
        val tools = WatchTools(activity = listOf(
            WatchActivity(1,"A","2026-10-07",3,3), WatchActivity(1,"A","2026-10-07",-1,2),
            WatchActivity(1,"A","2026-10-06",-2,0), WatchActivity(1,"A","2026-10-08",99,99),
            WatchActivity(1,"A","invalid",99,99)))
        assertEquals(mapOf(today to 2, today.minusDays(1) to 0), tools.dailyEpisodes(today))
        assertEquals(WatchStreak(1,1), tools.streak(today))
    }

    @Test fun `streak includes yesterday until today ends and measures best retained run`() {
        val today = LocalDate.of(2026,10,7)
        val tools = WatchTools(activity = listOf(1L,2L,3L,7L,8L,9L,10L).map { WatchActivity(1,"A",today.minusDays(it).toString(),1,1) })
        assertEquals(WatchStreak(3,4), tools.streak(today))
        assertEquals(WatchStreak(0,4), tools.streak(today.plusDays(1)))
        assertEquals(WatchStreak(4,4), tools.withProgress(1,"A",0,1,today).streak(today))
    }

    @Test fun `views normalize limits names sort status and tags`() {
        val normalized = SavedListView("  Weekend ", "  A  ", WatchStatus.NOT_IN_LIST, tag = "  ", sort = "invalid").normalized()
        assertEquals("Weekend", normalized.name); assertEquals("A", normalized.query)
        assertNull(normalized.status); assertNull(normalized.tag); assertEquals("RECENT", normalized.sort)
        assertEquals(8, (listOf(SavedListView(" A "),SavedListView("a"),SavedListView(" ")) + (1..10).map { SavedListView("View $it") }).normalizedViews().size)
    }

    @Test fun `backup round trip preserves workspace and older backups gain defaults`() {
        val tools = WatchTools(pinned = setOf(1), dailyGoal = 5, durationOverrides = mapOf(1 to 48), savedViews = listOf(SavedListView("Weekend", status = WatchStatus.WATCHING, smartFilter = SmartListFilter.PINNED)))
        assertEquals(tools, PersonalBackup.decode(PersonalBackup(tools = tools).encode()).tools)
        val old = PersonalBackup.decode("{\"schemaVersion\":1,\"tools\":{\"favorites\":[5]}}")
        assertEquals(setOf(5), old.tools.favorites); assertEquals(3, old.tools.dailyGoal)
        assertTrue(old.tools.pinned.isEmpty()); assertTrue(old.tools.savedViews.isEmpty())
        val normalized = tools.copy(pinned = setOf(-1,1), dailyGoal = 999, durationOverrides = mapOf(-1 to 24, 1 to 999)).normalized()
        assertEquals(setOf(1), normalized.pinned); assertEquals(50, normalized.dailyGoal)
        assertEquals(mapOf(1 to 180), normalized.durationOverrides)
    }
}
