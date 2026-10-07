package com.owlcoder.animeschedule.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CalendarReminder(val minutes: Int?) {
    NONE(null), AT_START(0), FIVE(5), FIFTEEN(15), THIRTY(30), HOUR(60)
}

/** Includes empty days, so both the overview and export cover the same seven local dates. */
fun scheduleAgenda(today: LocalDate, days: List<ScheduleDay>): List<ScheduleDay> {
    val byDate = days.associateBy { it.date }
    return (0L..6L).map { offset ->
        val date = today.plusDays(offset)
        ScheduleDay(date, byDate[date]?.episodes.orEmpty().distinctBy { it.airingId }.sortedBy { it.airingAtEpochSeconds })
    }
}

fun ScheduleDay.toAgendaText(zone: ZoneId, locale: Locale, episodeLabel: (Int) -> String): String = buildString {
    append(date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", locale)))
    append(" · "); append(zone.id)
    val time = DateTimeFormatter.ofPattern("HH:mm", locale).withZone(zone)
    episodes.distinctBy { it.airingId }.sortedBy { it.airingAtEpochSeconds }.forEach {
        append("\n"); append(time.format(Instant.ofEpochSecond(it.airingAtEpochSeconds)))
        append(" · "); append(it.title); append(" · "); append(episodeLabel(it.episode))
    }
}

/** RFC 5545: UTC times avoid ambiguous local times at DST transitions. No calendar permissions. */
fun List<ScheduleDay>.toCalendarIcs(tools: WatchTools, generatedAt: Instant, episodeLabel: (Int) -> String): String {
    return toCalendarIcs(tools, generatedAt, CalendarReminder.NONE, episodeLabel)
}

fun List<ScheduleDay>.toCalendarIcs(tools: WatchTools, generatedAt: Instant, reminder: CalendarReminder, episodeLabel: (Int) -> String): String {
    val utc = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC)
    val lines = mutableListOf("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Anime Schedule//Agenda//EN", "CALSCALE:GREGORIAN", "METHOD:PUBLISH")
    flatMap { it.episodes }.distinctBy { it.airingId }.sortedBy { it.airingAtEpochSeconds }.forEach { episode ->
        val start = Instant.ofEpochSecond(episode.airingAtEpochSeconds)
        val minutes = (tools.durationOverrides[episode.malId] ?: tools.episodeMinutes).coerceIn(1, 180)
        lines += listOf("BEGIN:VEVENT", "UID:${episode.airingId}@anime-schedule.local", "DTSTAMP:${utc.format(generatedAt)}",
            "DTSTART:${utc.format(start)}", "DTEND:${utc.format(start.plusSeconds(minutes * 60L))}",
            "SUMMARY:${icalEscape(episode.title + " · " + episodeLabel(episode.episode))}")
        if (episode.animeId > 0) lines += "URL:https://anilist.co/anime/${episode.animeId}"
        reminder.minutes?.let { minutes ->
            lines += listOf("BEGIN:VALARM", "ACTION:DISPLAY", "TRIGGER:-PT${minutes}M",
                "DESCRIPTION:${icalEscape(episode.title + " · " + episodeLabel(episode.episode))}", "END:VALARM")
        }
        lines += "END:VEVENT"
    }
    lines += "END:VCALENDAR"
    return lines.joinToString("\r\n", postfix = "\r\n", transform = ::foldCalendarLine)
}

private fun icalEscape(value: String): String = value.replace("\r\n", "\n").replace('\r', '\n')
    .replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,").replace(";", "\\;")

/** Fold by UTF-8 octets, never through a multi-byte code point; the continuation space counts. */
private fun foldCalendarLine(value: String): String = buildString {
    var bytes = 0
    var index = 0
    while (index < value.length) {
        val codePoint = value.codePointAt(index)
        val piece = String(java.lang.Character.toChars(codePoint))
        val count = piece.toByteArray(Charsets.UTF_8).size
        if (bytes + count > 75) { append("\r\n "); bytes = 1 }
        append(piece); bytes += count; index += java.lang.Character.charCount(codePoint)
    }
}
