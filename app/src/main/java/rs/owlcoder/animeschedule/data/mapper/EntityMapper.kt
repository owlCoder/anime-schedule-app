package rs.owlcoder.animeschedule.data.mapper

import kotlinx.serialization.json.Json
import rs.owlcoder.animeschedule.data.local.db.AiringEpisodeEntity
import rs.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import rs.owlcoder.animeschedule.data.local.db.NotificationEntity
import rs.owlcoder.animeschedule.domain.model.AiringEpisode
import rs.owlcoder.animeschedule.domain.model.AnimeDetail
import rs.owlcoder.animeschedule.domain.model.AppNotification
import rs.owlcoder.animeschedule.domain.model.MalListEntry
import rs.owlcoder.animeschedule.domain.model.RelatedAnime
import rs.owlcoder.animeschedule.domain.model.Studio

private val json = Json { ignoreUnknownKeys = true }

fun NotificationEntity.toDomain(): AppNotification = AppNotification(
    id = id,
    animeId = animeId,
    title = title,
    episode = episode,
    coverImageUrl = coverImageUrl,
    airingAtEpochSeconds = airingAtEpochSeconds,
    isRead = isRead,
    createdAtEpochSeconds = createdAtEpochSeconds
)

fun AiringEpisodeEntity.toDomain(malListEntry: MalListEntry? = null): AiringEpisode = AiringEpisode(
    airingId = airingId,
    animeId = animeId,
    episode = episode,
    airingAtEpochSeconds = airingAtEpochSeconds,
    title = title,
    titleRomaji = titleRomaji,
    coverImageUrl = coverImageUrl,
    coverColor = coverColor,
    genres = genres,
    averageScore = averageScore,
    totalEpisodes = totalEpisodes,
    status = status,
    format = format,
    malListEntry = malListEntry
)

fun AnimeDetailEntity.toDomain(malListEntry: MalListEntry? = null): AnimeDetail {
    val studios = studiosJson?.let {
        runCatching {
            json.decodeFromString<List<Map<String, String>>>(it).map { m ->
                Studio(
                    id = m["id"]?.toIntOrNull() ?: 0,
                    name = m["name"] ?: "",
                    isMain = m["isMain"] == "true"
                )
            }
        }.getOrDefault(emptyList())
    } ?: emptyList()

    val relations = relationsJson?.let {
        runCatching {
            json.decodeFromString<List<Map<String, String>>>(it).map { m ->
                RelatedAnime(
                    animeId = m["id"]?.toIntOrNull() ?: 0,
                    title = m["title"] ?: "",
                    coverImageUrl = m["coverUrl"]?.takeIf { u -> u.isNotEmpty() },
                    format = m["format"]?.takeIf { f -> f.isNotEmpty() },
                    status = m["status"]?.takeIf { s -> s.isNotEmpty() },
                    relationType = m["relation"]?.takeIf { r -> r.isNotEmpty() }
                )
            }
        }.getOrDefault(emptyList())
    } ?: emptyList()

    return AnimeDetail(
        animeId = animeId,
        titleRomaji = titleRomaji,
        titleEnglish = titleEnglish,
        titleNative = titleNative,
        coverImageUrl = coverImageUrl,
        coverColor = coverColor,
        bannerImageUrl = bannerImageUrl,
        description = description,
        genres = genres,
        averageScore = averageScore,
        meanScore = meanScore,
        episodes = episodes,
        duration = duration,
        status = status,
        format = format,
        season = season,
        seasonYear = seasonYear,
        nextAiringEpisode = nextAiringEpisode,
        nextAiringAt = nextAiringAt,
        studios = studios,
        relations = relations,
        trailerSite = trailerSite,
        trailerId = trailerId,
        siteUrl = siteUrl,
        malListEntry = malListEntry
    )
}
