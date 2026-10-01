package com.owlcoder.animeschedule.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeUtilsTest {

    private val belgrade = ZoneId.of("Europe/Belgrade")

    @Test
    fun `week range spans seven local days from the given day`() {
        val today = LocalDate.of(2026, 8, 3)
        val (start, end) = weekRangeUtc(belgrade, today)

        assertEquals(today, epochSecondsToLocalDate(start, belgrade))
        assertEquals(today.plusDays(6), epochSecondsToLocalDate(end, belgrade))
        assertEquals(today.plusDays(7), epochSecondsToLocalDate(end + 1, belgrade))
    }

    @Test
    fun `week range is one hour shorter when it contains the spring DST change`() {
        // Europe/Belgrade moves forward on 2026-03-29, so that week has 7 * 24h - 1h.
        val (start, end) = weekRangeUtc(belgrade, LocalDate.of(2026, 3, 28))

        assertEquals(Duration.ofDays(7).minusHours(1).seconds, end + 1 - start)
    }

    @Test
    fun `week range is one hour longer when it contains the autumn DST change`() {
        val (start, end) = weekRangeUtc(belgrade, LocalDate.of(2026, 10, 24))

        assertEquals(Duration.ofDays(7).plusHours(1).seconds, end + 1 - start)
    }

    @Test
    fun `epoch conversion respects the target timezone`() {
        // 2026-01-01T23:30Z is already Jan 2nd in Belgrade (UTC+1 in winter).
        val epoch = Instant.parse("2026-01-01T23:30:00Z").epochSecond
        assertEquals(LocalDate.of(2026, 1, 2), epochSecondsToLocalDate(epoch, belgrade))
        assertEquals(LocalDate.of(2026, 1, 1), epochSecondsToLocalDate(epoch, ZoneId.of("UTC")))
    }

    @Test
    fun `countdown formats days hours and minutes`() {
        val now = Instant.parse("2026-08-03T12:00:00Z")
        fun label(offsetMinutes: Long) =
            formatAiringCountdown(now.plusSeconds(offsetMinutes * 60).epochSecond, "Aired", now)

        assertEquals("4d 10h", label(4 * 1_440 + 10 * 60 + 5))
        assertEquals("2d", label(2 * 1_440))
        assertEquals("14h", label(14 * 60 + 30))
        assertEquals("1h 3m", label(63))
        assertEquals("38m", label(38))
        assertEquals("1m", formatAiringCountdown(now.plusSeconds(20).epochSecond, "Aired", now))
    }

    @Test
    fun `countdown reports aired once the episode time has passed`() {
        val now = Instant.parse("2026-08-03T12:00:00Z")

        assertEquals("Aired", formatAiringCountdown(now.epochSecond, "Aired", now))
        assertEquals("Aired", formatAiringCountdown(now.minusSeconds(60).epochSecond, "Aired", now))
    }

    @Test
    fun `next midnight is measured in the given zone`() {
        val evening = ZonedDateTime.of(2026, 8, 3, 23, 30, 0, 0, belgrade)

        assertEquals(Duration.ofMinutes(30).toMillis(), millisUntilNextDay(evening))
    }

    @Test
    fun `next midnight accounts for the shortened spring DST day`() {
        // 00:30 on the day clocks jump forward: only 22.5 hours remain, not 23.5.
        val now = ZonedDateTime.of(2026, 3, 29, 0, 30, 0, 0, belgrade)

        assertEquals(Duration.ofMinutes(22 * 60 + 30).toMillis(), millisUntilNextDay(now))
    }

    @Test
    fun `next midnight accounts for the lengthened autumn DST day`() {
        val now = ZonedDateTime.of(2026, 10, 25, 0, 30, 0, 0, belgrade)

        assertEquals(Duration.ofMinutes(24 * 60 + 30).toMillis(), millisUntilNextDay(now))
    }
}
