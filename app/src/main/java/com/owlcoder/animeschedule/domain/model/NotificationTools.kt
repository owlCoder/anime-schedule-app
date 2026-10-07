package com.owlcoder.animeschedule.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.Serializable

@Serializable
data class QuietHours(val enabled: Boolean = false, val startHour: Int = 22, val endHour: Int = 8) {
    fun normalized() = copy(startHour = startHour.coerceIn(0, 23), endHour = endHour.coerceIn(0, 23))
    /** Start is inclusive, end exclusive. Equal hours deliberately silence the whole day. */
    fun isQuietAt(instant: Instant, zone: ZoneId): Boolean {
        if (!enabled) return false
        val value = normalized()
        val hour = instant.atZone(zone).hour
        return when {
            value.startHour == value.endHour -> true
            value.startHour < value.endHour -> hour in value.startHour until value.endHour
            else -> hour >= value.startHour || hour < value.endHour
        }
    }
}

/** Silencing system alerts never discards the corresponding in-app history entry. */
fun shouldPostSystemAlert(animeId: Int, muted: Set<Int>, quietHours: QuietHours, now: Instant, zone: ZoneId, scheduledAt: Instant = now): Boolean =
    animeId !in muted && !quietHours.isQuietAt(now, zone) && !quietHours.isQuietAt(scheduledAt, zone)

enum class NotificationPeriod(val days: Long?) { ALL(null), TODAY(1), WEEK(7), MONTH(30) }
fun AppNotification.inPeriod(period: NotificationPeriod, today: LocalDate, zone: ZoneId): Boolean {
    val days = period.days ?: return true
    val date = Instant.ofEpochSecond(createdAtEpochSeconds).atZone(zone).toLocalDate()
    return !date.isBefore(today.minusDays(days - 1)) && !date.isAfter(today)
}
