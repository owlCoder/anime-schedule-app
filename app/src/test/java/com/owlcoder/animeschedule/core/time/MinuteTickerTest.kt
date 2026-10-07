package com.owlcoder.animeschedule.core.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MinuteTickerTest {
    @Test fun `ticks align to minute boundaries without accumulating initial offset`() = runTest {
        val start = Instant.parse("2026-10-07T12:00:03.123Z")
        val clock = object : Clock() {
            override fun getZone() = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = start.plusMillis(testScheduler.currentTime)
        }
        val values = mutableListOf<Instant>()
        val job = launch { currentMinuteFlow(clock).take(3).toList(values) }
        advanceUntilIdle(); job.join()
        assertEquals(listOf(start, Instant.parse("2026-10-07T12:01:00Z"), Instant.parse("2026-10-07T12:02:00Z")), values)
        advanceTimeBy(300_000)
        assertEquals(clock.instant(), currentMinuteFlow(clock).first())
    }
}
