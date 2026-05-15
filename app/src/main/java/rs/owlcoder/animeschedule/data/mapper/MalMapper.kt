package rs.owlcoder.animeschedule.data.mapper

import rs.owlcoder.animeschedule.data.api.mal.dto.MalAnimeNode
import rs.owlcoder.animeschedule.data.local.db.MalListEntryEntity
import rs.owlcoder.animeschedule.domain.model.AnimeSearchResult
import rs.owlcoder.animeschedule.domain.model.MalListEntry
import rs.owlcoder.animeschedule.domain.model.WatchStatus

fun MalAnimeNode.toEntity(): MalListEntryEntity {
    val listStatus = myListStatus
    return MalListEntryEntity(
        animeId = id,
        malId = id,
        title = title,
        coverImageUrl = mainPicture?.large ?: mainPicture?.medium,
        totalEpisodes = numEpisodes,
        status = listStatus?.status ?: "plan_to_watch",
        numEpisodesWatched = listStatus?.numEpisodesWatched ?: 0,
        score = listStatus?.score ?: 0,
        updatedAt = listStatus?.updatedAt
    )
}

fun MalListEntryEntity.toDomain(): MalListEntry = MalListEntry(
    animeId = animeId,
    title = title,
    coverImageUrl = coverImageUrl,
    status = WatchStatus.fromMal(status),
    episodesWatched = numEpisodesWatched,
    score = score,
    totalEpisodes = totalEpisodes
)

fun MalAnimeNode.toSearchResult(existingEntry: MalListEntry? = null): AnimeSearchResult {
    val listEntry = myListStatus?.let {
        MalListEntry(
            animeId = id,
            status = WatchStatus.fromMal(it.status),
            episodesWatched = it.numEpisodesWatched,
            score = it.score,
            totalEpisodes = numEpisodes
        )
    } ?: existingEntry
    return AnimeSearchResult(
        malId = id,
        title = title,
        titleEnglish = alternativeTitles?.en,
        coverImageUrl = mainPicture?.large ?: mainPicture?.medium,
        type = mediaType,
        year = startDate?.take(4),
        meanScore = mean,
        totalEpisodes = numEpisodes,
        userListEntry = listEntry
    )
}
