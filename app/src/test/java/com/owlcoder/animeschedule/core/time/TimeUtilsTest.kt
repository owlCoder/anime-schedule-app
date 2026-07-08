package com.owlcoder.animeschedule.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimeUtilsTest {

    private val zone = ZoneId.of("Europe/Belgrade")

    @Test
    fun `today range covers exactly one day`() {
        val (start, end) = todayRangeUtc(zone)
        assertEquals(LocalDate.now(zone), epochSecondsToLocalDate(start, zone))
        assertEquals(LocalDate.now(zone), epochSecondsToLocalDate(end, zone))
    }

    @Test
    fun `tomorrow range starts right after today ends`() {
        val (_, todayEnd) = todayRangeUtc(zone)
        val (tomorrowStart, _) = tomorrowRangeUtc(zone)
        assertEquals(todayEnd + 1, tomorrowStart)
        assertEquals(LocalDate.now(zone).plusDays(1), epochSecondsToLocalDate(tomorrowStart, zone))
    }

    @Test
    fun `week range spans seven days from today`() {
        val (start, end) = weekRangeUtc(zone)
        assertEquals(LocalDate.now(zone), epochSecondsToLocalDate(start, zone))
        assertEquals(LocalDate.now(zone).plusDays(6), epochSecondsToLocalDate(end, zone))
    }

    @Test
    fun `epoch conversion respects the target timezone`() {
        // 2026-01-01T23:30Z is already Jan 2nd in Belgrade (UTC+1 in winter).
        val epoch = java.time.Instant.parse("2026-01-01T23:30:00Z").epochSecond
        assertEquals(LocalDate.of(2026, 1, 2), epochSecondsToLocalDate(epoch, zone))
        assertEquals(LocalDate.of(2026, 1, 1), epochSecondsToLocalDate(epoch, ZoneId.of("UTC")))
    }
}
