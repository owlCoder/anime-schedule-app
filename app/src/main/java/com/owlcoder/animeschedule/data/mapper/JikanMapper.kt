package com.owlcoder.animeschedule.data.mapper

import com.owlcoder.animeschedule.data.api.jikan.JikanAnime
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

fun JikanAnime.toAiringEpisodeEntity(
    dayOfWeek: String,
    nowEpoch: Long,
    zoneId: ZoneId = ZoneId.of("Asia/Tokyo")
): AiringEpisodeEntity? {
    val broadcastTime = broadcast?.time ?: "00:00"
    val (hour, minute) = broadcastTime.split(":").map { it.toIntOrNull() ?: 0 }
    val today = LocalDate.now(zoneId)
    val targetDay = today.with(
        java.time.temporal.TemporalAdjusters.nextOrSame(
            java.time.DayOfWeek.valueOf(dayOfWeek.uppercase())
        )
    )
    val airingZdt = ZonedDateTime.of(targetDay, java.time.LocalTime.of(hour, minute), zoneId)
    return AiringEpisodeEntity(
        // Negated so this fallback (Jikan/MAL id space) can never collide with the primary
        // AniList path's `airingId` (AniList's own AiringSchedule id, always positive) in the
        // shared Room primary key column.
        airingId = -malId,
        animeId = malId,
        malId = malId,
        episode = 0,
        airingAtEpochSeconds = airingZdt.toEpochSecond(),
        title = titleEnglish ?: title,
        titleRomaji = title,
        coverImageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl,
        coverColor = null,
        genres = genres.map { it.name },
        averageScore = score?.times(10)?.toInt(),
        totalEpisodes = episodes,
        status = status,
        format = type,
        cachedAtEpochSeconds = nowEpoch,
        source = "jikan"
    )
}
