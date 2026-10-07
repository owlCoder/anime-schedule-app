package com.owlcoder.animeschedule.domain.model

import java.time.Instant
import java.time.ZoneId

enum class ScheduleTimeOfDay {
    ALL, MORNING, AFTERNOON, EVENING, NIGHT;

    /** Uses the schedule zone, including daylight saving transitions. */
    fun matches(epochSeconds: Long, zone: ZoneId): Boolean {
        if (this == ALL) return true
        val hour = Instant.ofEpochSecond(epochSeconds).atZone(zone).hour
        return when (this) {
            MORNING -> hour in 6..11
            AFTERNOON -> hour in 12..17
            EVENING -> hour in 18..23
            NIGHT -> hour in 0..5
            ALL -> true
        }
    }
}
