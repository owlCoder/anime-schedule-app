package com.owlcoder.animeschedule.presentation.screens.mylist

import com.owlcoder.animeschedule.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ActivityExportTest {
    @Test fun `activity export preserves order quotes commas and newlines and neutralizes formulas`() {
        val csv = listOf(WatchActivity(1, "=SUM(1,2)\n\"Title\"", "2026-10-07", -1, 2), WatchActivity(2,"Žanr", "2026-10-06",1,3)).toActivityCsv()
        assertTrue(csv.startsWith("\uFEFFmal_id,title,date,episode_delta,progress\r\n"))
        assertTrue(csv.contains("\"'=SUM(1,2)\n\"\"Title\"\"\"")); assertTrue(csv.contains("\"-1\"")); assertTrue(csv.contains("Žanr"))
        assertTrue(csv.indexOf("2026-10-07") < csv.indexOf("2026-10-06"))
        assertEquals(1, emptyList<WatchActivity>().toActivityCsv().split("\r\n").filter { it.isNotEmpty() }.size)
    }
    @Test fun `time sort uses personal durations places unknown last and has stable title ties`() {
        fun entry(id: Int, title: String, total: Int?) = MalListEntry(id,title,status=WatchStatus.WATCHING, episodesWatched=0, score=0,totalEpisodes=total)
        val entries = listOf(entry(1,"B",12),entry(2,"A",6),entry(3,"Unknown",null))
        assertEquals(listOf(1,2,3), entries.sortedFor(MyListSortOrder.WATCH_TIME, WatchTools(durationOverrides=mapOf(1 to 10))).map { it.animeId })
        assertEquals(listOf(2,1,3), entries.sortedFor(MyListSortOrder.WATCH_TIME, WatchTools(durationOverrides=mapOf(1 to 12))).map { it.animeId })
    }
}
