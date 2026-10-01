package com.owlcoder.animeschedule.core.time

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DayTickerTest {

    @Test
    fun `the date rolls over at local midnight of the given zone`() = runTest {
        val zone = ZoneId.of("Europe/Belgrade")
        // 23:59 local on Aug 3 (21:59Z): one minute before the date changes.
        val start = Instant.parse("2026-08-03T21:59:00Z")
        val clock = object : Clock() {
            override fun getZone(): ZoneId = zone
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = start.plusMillis(testScheduler.currentTime)
        }
        val dates = mutableListOf<LocalDate>()

        val collector = launch { currentDateFlow(zone, clock).take(3).toList(dates) }
        advanceUntilIdle()
        collector.join()

        assertEquals(
            listOf(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 5)),
            dates,
        )
    }
}
