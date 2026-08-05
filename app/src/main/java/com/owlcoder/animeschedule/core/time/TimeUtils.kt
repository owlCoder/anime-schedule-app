package com.owlcoder.animeschedule.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun epochSecondsToLocalDateTime(
    epochSeconds: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): LocalDateTime = Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDateTime()

fun todayRangeUtc(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    val today = LocalDate.now(zoneId)
    val start = today.atStartOfDay(zoneId).toEpochSecond()
    val end = today.plusDays(1).atStartOfDay(zoneId).toEpochSecond() - 1
    return start to end
}

fun tomorrowRangeUtc(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    val tomorrow = LocalDate.now(zoneId).plusDays(1)
    val start = tomorrow.atStartOfDay(zoneId).toEpochSecond()
    val end = tomorrow.plusDays(1).atStartOfDay(zoneId).toEpochSecond() - 1
    return start to end
}

fun weekRangeUtc(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    val today = LocalDate.now(zoneId)
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
    zoneId: ZoneId = ZoneId.systemDefault(),
    airedLabel: String = "Aired",
): String {
    // Keep zoneId in the public contract because callers use a user-selected zone,
    // while the duration itself is zone-independent.
    @Suppress("UNUSED_VARIABLE")
    val requestedZone = zoneId

    val now = Instant.now()
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
