package com.owlcoder.animeschedule.domain.model

import com.owlcoder.animeschedule.presentation.screens.schedule.*
import com.owlcoder.animeschedule.presentation.screens.discovery.ReleaseFilter
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class Workspace590Test {
    private val zone = ZoneId.of("Europe/Belgrade")
    private val today = LocalDate.of(2026, 10, 7)
    private fun episode(id: Int, hour: Int = 10) = AiringEpisode(id, id, id, 1,
        today.atTime(hour, 0).atZone(zone).toEpochSecond(), "Anime $id", null, null, null, listOf("Action"), 80, 12, "RELEASING", "TV", null)
    @Test fun `premieres exclude later and unknown episodes`() {
        val rows = listOf(episode(1), episode(2).copy(episode = 2), episode(3).copy(episode = 0))
        assertEquals(listOf(1), rows.applyFilter(ScheduleFilter(premieresOnly = true), 0).map { it.animeId })
    }
    @Test fun `score floor includes boundary and excludes absent score`() {
        val rows = listOf(episode(1), episode(2).copy(averageScore = 79), episode(3).copy(averageScore = null))
        assertEquals(listOf(1), rows.applyFilter(ScheduleFilter(minimumScore = 80), 0).map { it.animeId })
        assertEquals(3, rows.applyFilter(ScheduleFilter(), 0).size)
    }
    @Test fun `release aliases work and unknown statuses only pass all`() {
        val rows = listOf(episode(1).copy(status = "currently_airing"), episode(2).copy(status = "completed"), episode(3).copy(status = null))
        assertEquals(listOf(1), rows.applyFilter(ScheduleFilter(release = ReleaseFilter.AIRING), 0).map { it.animeId })
        assertEquals(listOf(2), rows.applyFilter(ScheduleFilter(release = ReleaseFilter.FINISHED), 0).map { it.animeId })
        assertEquals(3, rows.applyFilter(ScheduleFilter(), 0).size)
    }
    @Test fun `time windows partition every hour including boundaries`() {
        for (hour in 0..23) {
            val time = episode(1, hour).airingAtEpochSeconds
            val matches = ScheduleTimeOfDay.entries.filter { it != ScheduleTimeOfDay.ALL && it.matches(time, zone) }
            assertEquals(1, matches.size)
            assertEquals(when (hour) { in 6..11 -> ScheduleTimeOfDay.MORNING; in 12..17 -> ScheduleTimeOfDay.AFTERNOON; in 18..23 -> ScheduleTimeOfDay.EVENING; else -> ScheduleTimeOfDay.NIGHT }, matches.single())
        }
    }
    @Test fun `time filter uses schedule timezone and handles repeated DST hours`() {
        val time = today.atTime(6, 0).atZone(zone).toEpochSecond()
        assertTrue(ScheduleTimeOfDay.MORNING.matches(time, zone))
        assertTrue(ScheduleTimeOfDay.NIGHT.matches(time, ZoneOffset.UTC))
        listOf("2026-10-25T00:30:00Z", "2026-10-25T01:30:00Z").forEach { assertTrue(ScheduleTimeOfDay.NIGHT.matches(Instant.parse(it).epochSecond, zone)) }
    }
    @Test fun `four filters combine and reset returns full schedule`() {
        val rows = listOf(episode(1), episode(2, 22), episode(3).copy(episode = 2), episode(4).copy(averageScore = null), episode(5).copy(status = "FINISHED"))
        val filter = ScheduleFilter(premieresOnly = true, minimumScore = 80, release = ReleaseFilter.AIRING, timeOfDay = ScheduleTimeOfDay.MORNING)
        assertTrue(filter.isActive)
        assertEquals(listOf(1), rows.applyFilter(filter, 0, zone = zone).map { it.animeId })
        assertFalse(ScheduleFilter().isActive); assertEquals(5, rows.applyFilter(ScheduleFilter(), 0, zone = zone).size)
    }
    @Test fun `each calendar reminder exports one alarm per deduplicated event`() {
        val days = listOf(ScheduleDay(today, listOf(episode(1), episode(1), episode(2))))
        CalendarReminder.entries.forEach { reminder ->
            val ics = days.toCalendarIcs(WatchTools(), Instant.EPOCH, reminder) { "Episode $it" }
            assertEquals(if (reminder == CalendarReminder.NONE) 0 else 2, Regex("BEGIN:VALARM").findAll(ics).count())
            if (reminder.minutes != null) assertEquals(2, Regex("TRIGGER:-PT${reminder.minutes}M").findAll(ics).count())
            assertEquals(2, Regex("BEGIN:VEVENT").findAll(ics).count())
            assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
        }
        assertEquals(days.toCalendarIcs(WatchTools(), Instant.EPOCH) { "E$it" }, days.toCalendarIcs(WatchTools(), Instant.EPOCH, CalendarReminder.NONE) { "E$it" })
    }
    @Test fun `calendar alarm descriptions escape and fold Unicode safely`() {
        val ics = listOf(ScheduleDay(today, listOf(episode(1).copy(title = "日本😀;\n".repeat(40))))).toCalendarIcs(WatchTools(), Instant.EPOCH, CalendarReminder.HOUR) { "Episode $it" }
        assertTrue(ics.split("\r\n").all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertTrue(ics.replace("\r\n ", "").contains("DESCRIPTION:日本😀\\;\\n"))
        assertFalse(ics.contains('\uFFFD'))
    }
    private fun related(id: Int, title: String, type: String? = "SEQUEL", media: String? = "ANIME") = RelatedAnime(id, title, null, null, null, type, media)
    @Test fun `related finder only navigates anime and deduplicates graph edges`() {
        val rows = listOf(related(1,"beta"), related(1,"beta"), related(2,"Alpha", media = null), related(3,"Manga",media="MANGA"), related(0,"Invalid"), related(4,"Other",type="OTHER"))
        assertEquals(listOf(2,1), rows.findRelatedAnime().map { it.animeId })
    }
    @Test fun `relation type and trimmed title query are combined before deduplication`() {
        val rows = listOf(related(1,"Alpha", "PREQUEL"), related(1,"Alpha", "SEQUEL"), related(2,"Beta", "SEQUEL"))
        assertEquals(listOf(1), rows.findRelatedAnime(" alpha ", "sequel").map { it.animeId })
        assertTrue(rows.findRelatedAnime("missing").isEmpty())
    }
    private fun notification(id: Int, day: LocalDate, title: String = "Alpha") = AppNotification(id,id,title,1,null,0,false,day.atStartOfDay(zone).toEpochSecond())
    @Test fun `notification sorting is chronological with stable ties`() {
        val rows = listOf(notification(3,today), notification(1,today.minusYears(1)), notification(2,today))
        assertEquals(listOf(3,2,1), rows.visibleNotifications("",NotificationPeriod.ALL,today,zone).map { it.id })
        assertEquals(listOf(1,2,3), rows.visibleNotifications("",NotificationPeriod.ALL,today,zone,NotificationSort.OLDEST).map { it.id })
    }
    @Test fun `notification scope combines query period and timezone`() {
        val rows = listOf(notification(1,today),notification(2,today.minusDays(6)),notification(3,today.minusDays(7)),notification(4,today,"Beta"))
        assertEquals(listOf(1,2), rows.visibleNotifications(" ALPHA ",NotificationPeriod.WEEK,today,zone).map { it.id })
        assertEquals(listOf(1), rows.visibleNotifications("Alpha",NotificationPeriod.TODAY,today,zone).map { it.id })
    }
}
