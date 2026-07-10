package com.owlcoder.animeschedule.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

fun epochSecondsToLocalDateTime(epochSeconds: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDateTime()

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

fun formatAiringCountdown(
    airingAtEpochSeconds: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    airedLabel: String = "Aired"
): String {
    val now = Instant.now()
    val airingInstant = Instant.ofEpochSecond(airingAtEpochSeconds)
    if (airingInstant.isBefore(now)) return airedLabel
    val totalMinutes = ChronoUnit.MINUTES.between(now, airingInstant)
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60
    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0) append("${hours}h ")
        if (minutes > 0 || (days == 0L && hours == 0L)) append("${minutes}min")
    }.trim()
}

fun epochSecondsToLocalDate(epochSeconds: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate()
