package com.owlcoder.animeschedule.presentation.screens.mylist

import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyListInsightsTest {
    private fun entry(id: Int, title: String, watched: Int, total: Int?, score: Int = 0,
        status: WatchStatus = WatchStatus.WATCHING, updated: String? = null) =
        MalListEntry(id, title, status = status, episodesWatched = watched, totalEpisodes = total, score = score, updatedAt = updated)

    @Test fun `statistics exclude unrated scores and unknown totals from the backlog`() {
        val stats = listOf(
            entry(1, "A", 5, 12, 8), entry(2, "B", 12, 12, 10, WatchStatus.COMPLETED),
            entry(3, "C", 20, null), entry(4, "D", 0, 24, status = WatchStatus.PLAN_TO_WATCH),
            entry(5, "E", 13, 12),
        ).insights()
        assertEquals(5, stats.totalAnime)
        assertEquals(1, stats.completedAnime)
        assertEquals(50L, stats.watchedEpisodes)
        assertEquals(7L, stats.remainingEpisodes)
        assertEquals(2, stats.ratedAnime)
        assertEquals(9.0, stats.averageScore!!, 0.001)
        assertNull(emptyList<MalListEntry>().insights().averageScore)
    }

    @Test fun `progress uses a ratio and unknown totals sort last`() {
        val entries = listOf(entry(1, "B", 10, 100), entry(2, "A", 9, 12), entry(3, "C", 20, null))
        assertEquals(listOf(2, 1, 3), entries.sortedFor(MyListSortOrder.PROGRESS).map { it.animeId })
        assertEquals(listOf(2, 1, 3), entries.sortedFor(MyListSortOrder.REMAINING).map { it.animeId })
    }

    @Test fun `recent order compares instants across timezone offsets and handles invalid dates`() {
        val entries = listOf(
            entry(1, "B", 0, 12, updated = "2026-10-06T12:00:00+02:00"),
            entry(2, "A", 0, 12, updated = "2026-10-06T11:00:00Z"),
            entry(3, "C", 0, 12, updated = "invalid"),
        )
        assertEquals(listOf(2, 1, 3), entries.sortedFor(MyListSortOrder.RECENT).map { it.animeId })
    }

    @Test fun `title and score sorts have deterministic tie breakers`() {
        val entries = listOf(entry(1, "zebra", 0, 12, 10), entry(3, "Alpha", 0, 12, 8), entry(2, "alpha", 0, 12, 8))
        assertEquals(listOf(2, 3, 1), entries.sortedFor(MyListSortOrder.TITLE).map { it.animeId })
        assertEquals(listOf(1, 2, 3), entries.sortedFor(MyListSortOrder.SCORE).map { it.animeId })
    }
}
