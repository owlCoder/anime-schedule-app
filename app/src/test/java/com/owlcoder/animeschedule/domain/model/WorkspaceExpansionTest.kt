package com.owlcoder.animeschedule.domain.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class WorkspaceExpansionTest {
    private fun entry(id: Int = 1, watched: Int = 0, total: Int? = 12, score: Int = 8, status: WatchStatus = WatchStatus.WATCHING) =
        MalListEntry(id, "Title $id", status = status, episodesWatched = watched, score = score, totalEpisodes = total)

    @Test fun `rating boundaries include unrated only when zero is allowed and views normalize ranges`() {
        assertTrue(entry(score = 8).matchesRating(8, 10)); assertFalse(entry(score = 7).matchesRating(8, 10))
        assertTrue(entry(score = 0).matchesRating(0, 0)); assertFalse(entry(score = 0).matchesRating(1, 10))
        val view = SavedListView("Rated", sort = "WATCH_TIME", minimumScore = 8, maximumScore = 10)
        assertEquals(view, PersonalBackup.decode(PersonalBackup(tools = WatchTools(savedViews = listOf(view))).encode()).tools.savedViews.single())
        val normalized = view.copy(minimumScore = 12, maximumScore = -1).normalized()
        assertEquals(10, normalized.minimumScore); assertEquals(10, normalized.maximumScore)
    }

    @Test fun `time estimates use overrides clamp watched and stay safe for large episode totals`() {
        val tools = WatchTools(durationOverrides = mapOf(1 to 12))
        assertEquals(96L, entry(watched = 4).remainingMinutes(tools))
        assertEquals(0L, entry(watched = 99).remainingMinutes(tools))
        assertNull(entry(total = null).remainingMinutes(tools))
        assertEquals(Int.MAX_VALUE.toLong() * 180, entry(total = Int.MAX_VALUE).remainingMinutes(WatchTools(episodeMinutes = 180)))
    }

    @Test fun `backlog excludes finished dropped duplicates and reports unknown separately`() {
        val groups = listOf(entry(watched = 4), entry(watched = 4), entry(2, total = null), entry(3, status = WatchStatus.COMPLETED), entry(4, status = WatchStatus.DROPPED), entry(5, status = WatchStatus.PLAN_TO_WATCH)).backlog(WatchTools())
        assertEquals(BacklogGroup(WatchStatus.WATCHING, 2, 8, 192, 1), groups.first())
        assertEquals(3L, groups.first().daysAt(3)); assertNull(groups.first().daysAt(0))
        assertEquals(12L, groups[1].episodes)
    }

    @Test fun `planner styles finish short known series or focus while excluding chosen ids`() {
        val entries = listOf(entry(1), entry(2, watched = 11), entry(3, total = null))
        val finish = planWatchSession(entries, WatchTools(pinned = setOf(1)), 72, PlannerStrategy.FINISH_FIRST)
        assertEquals(listOf(2, 1), finish.map { it.entry.animeId }); assertEquals(listOf(1, 2), finish.map { it.episodes })
        val focus = planWatchSession(entries, WatchTools(pinned = setOf(1)), 72, PlannerStrategy.FOCUS)
        assertEquals(listOf(1), focus.map { it.entry.animeId }); assertEquals(3, focus.single().episodes)
        val excluded = planWatchSession(entries, WatchTools(), 72, PlannerStrategy.FOCUS, setOf(1, 2))
        assertEquals(3, excluded.single().entry.animeId); assertEquals(72, excluded.sumOf { it.minutes })
        assertTrue(planWatchSession(entries, WatchTools(), 480, excluded = setOf(1, 2, 3)).isEmpty())
    }

    @Test fun `history ranges have inclusive boundaries reject future and malformed dates and combine search`() {
        val today = LocalDate.of(2026, 10, 7)
        val dates = listOf(today, today.minusDays(29), today.minusDays(30), today.minusDays(89), today.minusDays(90), today.plusDays(1))
        val tools = WatchTools(activity = dates.map { WatchActivity(1, "Alpha", it.toString(), 1, 1) } + WatchActivity(2,"Beta","invalid",1,1))
        assertEquals(7, tools.activityInRange("", ActivityRange.ALL, today).size)
        assertEquals(2, tools.activityInRange("alpha", ActivityRange.LAST_30, today).size)
        assertEquals(4, tools.activityInRange("", ActivityRange.LAST_90, today).size)
        assertEquals(1, tools.activityInRange("", ActivityRange.THIS_WEEK, today).size)
        assertTrue(tools.activityInRange("Beta", ActivityRange.LAST_90, today).isEmpty())
    }

    @Test fun `bulk markers preserve unrelated personal data and reject invalid ids`() {
        val tools = WatchTools(favorites = setOf(9), pinned = setOf(10), notes = mapOf(1 to "Keep"))
        val updated = tools.withMarkers(setOf(-1, 0, 1, 2), favorite = true, pin = true)
        assertEquals(setOf(9, 1, 2), updated.favorites); assertEquals(setOf(10, 1, 2), updated.pinned)
        assertEquals(tools.notes, updated.notes)
        assertEquals(setOf(9, 2), updated.withMarkers(setOf(1), favorite = false).favorites)
        assertEquals(updated.favorites, updated.withMarkers(setOf(1), pin = false).favorites)
    }

    @Test fun `renaming merges tags case insensitively updates saved views and deletion keeps other data`() {
        val tools = WatchTools(tags = mapOf(1 to linkedSetOf("Action", "Weekend"), 2 to setOf("ACTION", "Other")), savedViews = listOf(SavedListView("Tagged", tag = "action")), favorites = setOf(1))
        val updated = tools.renameTag("Action", " Weekend ")
        assertEquals(setOf("Weekend"), updated.tags[1]); assertEquals(setOf("Weekend", "Other"), updated.tags[2])
        assertEquals("Weekend", updated.savedViews.single().tag)
        assertEquals(tools, tools.renameTag("Action", " "))
        assertEquals(tools, tools.renameTag("Action", "A,B"))
        val deleted = updated.renameTag("weekend", null)
        assertNull(deleted.tags[1]); assertEquals(setOf("Other"), deleted.tags[2]); assertNull(deleted.savedViews.single().tag)
        assertEquals(setOf(1), deleted.favorites)
    }
}
