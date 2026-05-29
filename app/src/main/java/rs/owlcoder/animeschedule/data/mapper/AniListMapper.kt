package rs.owlcoder.animeschedule.data.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rs.owlcoder.animeschedule.data.api.anilist.generated.AiringScheduleQuery
import rs.owlcoder.animeschedule.data.api.anilist.generated.AnimeDetailByMalQuery
import rs.owlcoder.animeschedule.data.api.anilist.generated.AnimeDetailQuery
import rs.owlcoder.animeschedule.data.api.anilist.generated.AnimeSearchQuery
import rs.owlcoder.animeschedule.data.local.db.AiringEpisodeEntity
import rs.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import rs.owlcoder.animeschedule.domain.model.AnimeSearchResult

private val json = Json { ignoreUnknownKeys = true }

fun AnimeSearchQuery.Medium.toSearchResult(): AnimeSearchResult = AnimeSearchResult(
    malId = id,
    title = title?.romaji ?: title?.english ?: "Unknown",
    titleEnglish = title?.english?.takeIf { it != title?.romaji },
    coverImageUrl = coverImage?.large,
    type = format?.rawValue,
    year = seasonYear?.toString(),
    meanScore = meanScore?.toDouble(),
    totalEpisodes = episodes,
    userListEntry = null
)

fun AiringScheduleQuery.AiringSchedule.toEntity(nowEpoch: Long): AiringEpisodeEntity? {
    val media = media ?: return null
    return AiringEpisodeEntity(
        airingId = id,
        animeId = media.id,
        malId = media.idMal,
        episode = episode,
        airingAtEpochSeconds = airingAt.toLong(),
        title = media.title?.english ?: media.title?.romaji ?: "Unknown",
        titleRomaji = media.title?.romaji,
        coverImageUrl = media.coverImage?.large,
        coverColor = media.coverImage?.color,
        genres = media.genres?.filterNotNull() ?: emptyList(),
        averageScore = media.averageScore,
        totalEpisodes = media.episodes,
        status = media.status?.rawValue,
        format = media.format?.rawValue,
        cachedAtEpochSeconds = nowEpoch,
        source = "anilist"
    )
}

fun AnimeDetailByMalQuery.Media.toEntity(nowEpoch: Long, malId: Int): AnimeDetailEntity {
    val studiosJson = studios?.edges?.filterNotNull()?.map { edge ->
        mapOf(
            "id" to (edge.node?.id?.toString() ?: ""),
            "name" to (edge.node?.name ?: ""),
            "isMain" to (edge.isMain?.toString() ?: "false")
        )
    }?.let { kotlinx.serialization.json.Json.encodeToString(it) }

    val relationsJson = relations?.edges?.filterNotNull()?.map { edge ->
        mapOf(
            "id" to (edge.node?.id?.toString() ?: ""),
            "title" to (edge.node?.title?.romaji ?: ""),
            "coverUrl" to (edge.node?.coverImage?.medium ?: ""),
            "format" to (edge.node?.format?.rawValue ?: ""),
            "status" to (edge.node?.status?.rawValue ?: ""),
            "relation" to (edge.relationType?.rawValue ?: "")
        )
    }?.let { kotlinx.serialization.json.Json.encodeToString(it) }

    return AnimeDetailEntity(
        animeId = id,
        malId = malId,
        titleRomaji = title?.romaji,
        titleEnglish = title?.english,
        titleNative = title?.native,
        coverImageUrl = coverImage?.extraLarge ?: coverImage?.large,
        coverColor = coverImage?.color,
        bannerImageUrl = bannerImage,
        description = description,
        genres = genres?.filterNotNull() ?: emptyList(),
        averageScore = averageScore,
        meanScore = meanScore,
        episodes = episodes,
        duration = duration,
        status = status?.rawValue,
        format = format?.rawValue,
        season = season?.rawValue,
        seasonYear = seasonYear,
        nextAiringEpisode = nextAiringEpisode?.episode,
        nextAiringAt = nextAiringEpisode?.airingAt?.toLong(),
        studiosJson = studiosJson,
        relationsJson = relationsJson,
        trailerSite = trailer?.site,
        trailerId = trailer?.id,
        siteUrl = siteUrl,
        cachedAtEpochSeconds = nowEpoch
    )
}

fun AnimeDetailQuery.Media.toEntity(nowEpoch: Long): AnimeDetailEntity {
    val studiosJson = studios?.edges?.filterNotNull()?.map { edge ->
        mapOf(
            "id" to (edge.node?.id?.toString() ?: ""),
            "name" to (edge.node?.name ?: ""),
            "isMain" to (edge.isMain?.toString() ?: "false")
        )
    }?.let { json.encodeToString(it) }

    val relationsJson = relations?.edges?.filterNotNull()?.map { edge ->
        mapOf(
            "id" to (edge.node?.id?.toString() ?: ""),
            "title" to (edge.node?.title?.romaji ?: ""),
            "coverUrl" to (edge.node?.coverImage?.medium ?: ""),
            "format" to (edge.node?.format?.rawValue ?: ""),
            "status" to (edge.node?.status?.rawValue ?: ""),
            "relation" to (edge.relationType?.rawValue ?: "")
        )
    }?.let { json.encodeToString(it) }

    return AnimeDetailEntity(
        animeId = id,
        malId = null,
        titleRomaji = title?.romaji,
        titleEnglish = title?.english,
        titleNative = title?.native,
        coverImageUrl = coverImage?.extraLarge ?: coverImage?.large,
        coverColor = coverImage?.color,
        bannerImageUrl = bannerImage,
        description = description,
        genres = genres?.filterNotNull() ?: emptyList(),
        averageScore = averageScore,
        meanScore = meanScore,
        episodes = episodes,
        duration = duration,
        status = status?.rawValue,
        format = format?.rawValue,
        season = season?.rawValue,
        seasonYear = seasonYear,
        nextAiringEpisode = nextAiringEpisode?.episode,
        nextAiringAt = nextAiringEpisode?.airingAt?.toLong(),
        studiosJson = studiosJson,
        relationsJson = relationsJson,
        trailerSite = trailer?.site,
        trailerId = trailer?.id,
        siteUrl = siteUrl,
        cachedAtEpochSeconds = nowEpoch
    )
}
