package com.owlcoder.animeschedule.presentation.screens.mylist

import com.owlcoder.animeschedule.domain.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class WatchToolsTest {
    @Test fun `weekly goal uses Monday boundary and subtracts corrections`() {
        val sunday = LocalDate.parse("2026-10-04")
        var tools = WatchTools().withProgress(1, "A", 0, 7, sunday)
        tools = tools.withProgress(1, "A", 7, 10, sunday.plusDays(1))
        tools = tools.withProgress(1, "A", 10, 9, sunday.plusDays(2))
        assertEquals(7, tools.episodesThisWeek(sunday))
        assertEquals(2, tools.episodesThisWeek(sunday.plusDays(2)))
        assertEquals(0, tools.episodesThisWeek(sunday.plusDays(8)))
    }
    @Test fun `unchanged progress does not log an event and history is bounded`() {
        val today = LocalDate.parse("2026-10-06")
        var tools = WatchTools()
        assertSame(tools, tools.withProgress(1, "A", 4, 4, today))
        repeat(350) { tools = tools.withProgress(1, "A", it, it + 1, today) }
        assertEquals(300, tools.activity.size)
        assertEquals(350, tools.activity.first().progress)
    }
    @Test fun `CSV preserves quotes multiline notes and non ASCII while neutralizing formulas`() {
        val entry = MalListEntry(4, "=\"Anime, naziv\"", status = WatchStatus.WATCHING, episodesWatched = 2, score = 0, totalEpisodes = null)
        val csv = listOf(entry).toListCsv(WatchTools(favorites = setOf(4), notes = mapOf(4 to "Lična\nbeleška \"tekst\"")))
        assertTrue(csv.startsWith("\uFEFFmal_id,"))
        assertTrue(csv.contains("\"'=\"\"Anime, naziv\"\"\""))
        assertTrue(csv.contains("\"Lična\nbeleška \"\"tekst\"\"\""))
        assertTrue(csv.contains("\"true\""))
        assertTrue(csv.endsWith("\r\n"))
    }
}
