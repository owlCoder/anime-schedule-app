package com.owlcoder.animeschedule.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Emits the current local date in [zoneId] and again every time midnight passes, so screens that
 * stay open across midnight roll "Today" over instead of showing yesterday's schedule.
 */
fun currentDateFlow(zoneId: ZoneId, clock: Clock = Clock.system(zoneId)): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(clock.withZone(zoneId))
        emit(now.toLocalDate())
        delay(millisUntilNextDay(now))
    }
}

/** Milliseconds from [now] to the next local midnight (DST-aware, never less than 1 ms). */
fun millisUntilNextDay(now: ZonedDateTime): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
}
