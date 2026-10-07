package com.owlcoder.animeschedule.domain.model

import com.owlcoder.animeschedule.presentation.screens.schedule.*
import java.time.*
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class AgendaAndNotificationToolsTest {
    private val date = LocalDate.of(2026, 10, 7)
    private val zone = ZoneId.of("Europe/Belgrade")
    private fun episode(id: Int, watched: Int? = null, number: Int = 5, malId: Int? = 100 + id) = AiringEpisode(
        id, id, malId, number, date.atTime(23, 30).atZone(zone).toEpochSecond() + id * 60,
        "Anime $id", null, null, null, listOf("Action"), 80, 12, "RELEASING", "TV",
        watched?.let { MalListEntry(malId ?: 999, status = WatchStatus.WATCHING, episodesWatched = it, score = 8, totalEpisodes = 12) },
    )
    @Test fun `hide watched compares episode number rather than hiding all tracked titles`() {
        val rows = listOf(episode(1, 5), episode(2, 4), episode(3), episode(4, 9), episode(5, number = 0))
        assertEquals(listOf(2, 3, 5), rows.applyFilter(ScheduleFilter(hideWatched = true), 0).map { it.airingId })
    }
    @Test fun `favorite membership uses MAL IDs and combines with progress and title filters`() {
        val rows = listOf(episode(1, 5), episode(2, 4), episode(3))
        val filter = ScheduleFilter(favoritesOnly = true, hideWatched = true, query = "anime")
        assertEquals(listOf(2), rows.applyFilter(filter, 0, setOf(101, 102)).map { it.airingId })
        assertTrue(rows.applyFilter(filter, 0, setOf(2)).isEmpty())
    }
    @Test fun `agenda includes seven local dates and deduplicates sorted airings`() {
        val agenda = scheduleAgenda(date, listOf(ScheduleDay(date, listOf(episode(2), episode(1), episode(1))), ScheduleDay(date.plusDays(7), listOf(episode(9)))))
        assertEquals(7, agenda.size); assertEquals(date.plusDays(6), agenda.last().date)
        assertEquals(listOf(1, 2), agenda.first().episodes.map { it.airingId })
        assertTrue(agenda.drop(1).all { it.episodes.isEmpty() })
    }
    @Test fun `calendar uses UTC and per anime estimated duration with stable IDs`() {
        val generated = Instant.parse("2026-10-07T10:00:00Z")
        val ics = listOf(ScheduleDay(date, listOf(episode(1), episode(1)))).toCalendarIcs(WatchTools(durationOverrides = mapOf(101 to 48)), generated) { "Episode $it" }
        assertTrue(ics.contains("DTSTART:20261007T213100Z\r\nDTEND:20261007T221900Z"))
        assertTrue(ics.contains("DTSTAMP:20261007T100000Z")); assertTrue(ics.contains("UID:1@anime-schedule.local"))
        assertEquals(1, Regex("BEGIN:VEVENT").findAll(ics).count())
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
    }
    @Test fun `calendar escapes values and folds Unicode at no more than 75 UTF8 bytes`() {
        val title = "日本😀; comma, slash\\ and\nnext " + "日本😀".repeat(20)
        val ics = listOf(ScheduleDay(date, listOf(episode(1).copy(title = title)))).toCalendarIcs(WatchTools(), Instant.EPOCH) { "Ep $it" }
        assertTrue(ics.split("\r\n").all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        val unfolded = ics.replace("\r\n ", "")
        assertTrue(unfolded.contains("SUMMARY:日本😀\\; comma\\, slash\\\\ and\\nnext "))
        assertFalse(ics.contains('\uFFFD'))
        assertFalse(ics.replace("\r\n", "").contains('\n'))
    }
    @Test fun `daily sharing uses schedule zone and chronological airing order`() {
        val text = ScheduleDay(date, listOf(episode(2), episode(1), episode(1))).toAgendaText(zone, Locale.ENGLISH) { "Episode $it" }
        assertTrue(text.contains("Europe/Belgrade")); assertTrue(text.contains("23:31 · Anime 1 · Episode 5"))
        assertTrue(text.indexOf("Anime 1") < text.indexOf("Anime 2")); assertEquals(2, text.count { it == '\n' })
    }
    @Test fun `overnight quiet hours use selected zone and include start but exclude end`() {
        val quiet = QuietHours(true, 22, 8)
        assertFalse(quiet.isQuietAt(date.atTime(21, 59).atZone(zone).toInstant(), zone))
        assertTrue(quiet.isQuietAt(date.atTime(22, 0).atZone(zone).toInstant(), zone))
        assertTrue(quiet.isQuietAt(date.atTime(7, 59).atZone(zone).toInstant(), zone))
        assertFalse(quiet.isQuietAt(date.atTime(8, 0).atZone(zone).toInstant(), zone))
        val instant = Instant.parse("2026-10-07T21:00:00Z")
        assertTrue(quiet.isQuietAt(instant, zone)); assertFalse(quiet.isQuietAt(instant, ZoneOffset.UTC))
    }
    @Test fun `same day and all day quiet windows and disabled state work`() {
        val noon = date.atTime(12, 0).atZone(zone).toInstant()
        assertTrue(QuietHours(true, 10, 14).isQuietAt(noon, zone))
        assertTrue(QuietHours(true, 10, 10).isQuietAt(noon, zone))
        assertFalse(QuietHours(false, 10, 10).isQuietAt(noon, zone))
        assertEquals(QuietHours(true, 0, 23), QuietHours(true, -5, 99).normalized())
    }
    @Test fun `quiet policy also handles repeated DST hours and per anime mute independently`() {
        val dst = Instant.parse("2026-10-25T01:30:00Z")
        assertTrue(QuietHours(true, 1, 3).isQuietAt(dst, zone))
        assertFalse(shouldPostSystemAlert(1, setOf(1), QuietHours(), dst, zone))
        assertTrue(shouldPostSystemAlert(2, setOf(1), QuietHours(), dst, zone))
        assertFalse(shouldPostSystemAlert(2, emptySet(), QuietHours(true, 1, 3), dst, zone))
    }
    @Test fun `delayed checks do not replay a scheduled quiet hour alert after the window ends`() {
        val quiet = QuietHours(true, 22, 8)
        val due = date.atTime(7, 55).atZone(zone).toInstant()
        val delivered = date.atTime(8, 10).atZone(zone).toInstant()
        assertFalse(shouldPostSystemAlert(1, emptySet(), quiet, delivered, zone, due))
        assertTrue(shouldPostSystemAlert(1, emptySet(), QuietHours(), delivered, zone, due))
        assertTrue(shouldPostSystemAlert(1, emptySet(), quiet, date.atTime(10, 0).atZone(zone).toInstant(), zone, date.atTime(9, 0).atZone(zone).toInstant()))
    }
    @Test fun `notification period uses creation date in local zone with inclusive lower boundary`() {
        fun notification(day: LocalDate) = AppNotification(1, 1, "A", 1, null, 0, false, day.atStartOfDay(zone).toEpochSecond())
        assertTrue(notification(date).inPeriod(NotificationPeriod.TODAY, date, zone))
        assertFalse(notification(date.minusDays(1)).inPeriod(NotificationPeriod.TODAY, date, zone))
        assertTrue(notification(date.minusDays(6)).inPeriod(NotificationPeriod.WEEK, date, zone))
        assertFalse(notification(date.minusDays(7)).inPeriod(NotificationPeriod.WEEK, date, zone))
        assertTrue(notification(date.minusDays(29)).inPeriod(NotificationPeriod.MONTH, date, zone))
        assertFalse(notification(date.plusDays(1)).inPeriod(NotificationPeriod.MONTH, date, zone))
        assertTrue(notification(date.minusYears(1)).inPeriod(NotificationPeriod.ALL, date, zone))
    }
    @Test fun `character finder combines native name and case insensitive role and query`() {
        val characters = listOf(Character(1, "Alpha", "アルファ", null, "MAIN"), Character(2, "Beta Alpha", null, null, "supporting"))
        assertEquals(listOf(1, 2), characters.findCharacters(" alpha ", CharacterRole.ALL).map { it.id })
        assertEquals(listOf(1), characters.findCharacters("アル", CharacterRole.MAIN).map { it.id })
        assertEquals(listOf(2), characters.findCharacters("alpha", CharacterRole.SUPPORTING).map { it.id })
        assertTrue(characters.findCharacters("missing", CharacterRole.ALL).isEmpty())
    }
    @Test fun `backups include only valid mute IDs and remain backward compatible`() {
        val decoded = PersonalBackup.decode(PersonalBackup(tools = WatchTools(mutedNotifications = mapOf(1 to " A ", -2 to "Bad"))).encode())
        assertEquals(mapOf(1 to "A"), decoded.tools.mutedNotifications)
        assertTrue(PersonalBackup.decode("{\"schemaVersion\":1,\"tools\":{}}").tools.mutedNotifications.isEmpty())
    }
}
