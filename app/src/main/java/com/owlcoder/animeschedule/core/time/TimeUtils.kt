package com.owlcoder.animeschedule.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Epoch-second bounds (inclusive) of the seven local days starting at [today].
 * Day boundaries come from [ZoneId.rules], so DST transition days are 23/25 hours long
 * instead of a fixed 24.
 */
fun weekRangeUtc(
    zoneId: ZoneId,
    today: LocalDate = LocalDate.now(zoneId),
): Pair<Long, Long> {
    val start = today.atStartOfDay(zoneId).toEpochSecond()
    val end = today.plusDays(7).atStartOfDay(zoneId).toEpochSecond() - 1
    return start to end
}

/**
 * Compact, single-line countdown intended for narrow schedule rows.
 *
 * Far-away events deliberately omit minutes so the label never wraps into three lines.
 * Examples: `4d 10h`, `14h`, `1h 3m`, `38m`.
 */
fun formatAiringCountdown(
    airingAtEpochSeconds: Long,
    airedLabel: String = "Aired",
    now: Instant = Instant.now(),
): String {
    val airingInstant = Instant.ofEpochSecond(airingAtEpochSeconds)
    if (!airingInstant.isAfter(now)) return airedLabel

    val totalMinutes = ChronoUnit.MINUTES.between(now, airingInstant).coerceAtLeast(0)
    val days = totalMinutes / 1_440
    val hours = (totalMinutes % 1_440) / 60
    val minutes = totalMinutes % 60

    return when {
        days > 0 -> if (hours > 0) "${days}d ${hours}h" else "${days}d"
        totalMinutes >= 6 * 60 -> "${totalMinutes / 60}h"
        totalMinutes >= 60 -> if (minutes > 0) "${totalMinutes / 60}h ${minutes}m" else "${totalMinutes / 60}h"
        else -> "${totalMinutes.coerceAtLeast(1)}m"
    }
}

fun epochSecondsToLocalDate(
    epochSeconds: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): LocalDate = Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate()
