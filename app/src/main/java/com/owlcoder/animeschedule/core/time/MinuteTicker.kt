package com.owlcoder.animeschedule.core.time

import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Fresh on collection/resume, then aligned to minute boundaries rather than drifting. */
fun currentMinuteFlow(clock: Clock = Clock.systemUTC()): Flow<Instant> = flow {
    while (true) {
        val now = clock.instant()
        emit(now)
        delay(60_000L - Math.floorMod(now.toEpochMilli(), 60_000L))
    }
}
