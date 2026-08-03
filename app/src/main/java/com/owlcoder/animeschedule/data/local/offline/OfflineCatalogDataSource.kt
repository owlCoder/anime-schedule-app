package com.owlcoder.animeschedule.data.local.offline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.core.time.epochSecondsToLocalDate
import com.owlcoder.animeschedule.core.time.weekRangeUtc
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.model.SearchPage
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only, cache-backed catalog used when all remote providers are unavailable.
 *
 * This deliberately does not ship a copied internet dataset. The only source of truth here is
 * data the app has already cached after a successful remote request. That keeps offline results
 * honest: an empty result means this installation has not seen that title/season yet.
 */
@Singleton
class OfflineCatalogDataSource @Inject constructor(
    private val animeDetailDao: AnimeDetailDao,
    private val airingEpisodeDao: AiringEpisodeDao,
    private val malListEntryDao: MalListEntryDao
) {

    suspend fun search(
        query: String,
        page: Int = 0,
        pageSize: Int = DEFAULT_SEARCH_PAGE_SIZE
    ): AppResult<SearchPage> = localCall {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            return@localCall AppResult.Success(SearchPage(emptyList(), hasNextPage = false))
        }

        val malEntries = malListEntryDao.getAll().first()
        val malByMalId = malEntries.associateBy { it.malId }
        val rows = animeDetailDao.searchByTitle(
            query = normalized,
            limit = pageSize,
            offset = page.coerceAtLeast(0) * pageSize
        )
        AppResult.Success(
            SearchPage(
                results = rows.map { row -> row.toSearchResult(malByMalId[row.malId]?.toDomain()) },
                hasNextPage = rows.size == pageSize
            )
        )
    }

    suspend fun getDetail(id: Int): AppResult<AnimeDetail> = localCall {
        // animeId is preferred because it is the app's persisted primary key; MAL is the
        // compatibility lookup for entries opened from the user's MAL list.
        val row = animeDetailDao.getByIdOnce(id) ?: animeDetailDao.getByMalId(id)
            ?: return@localCall AppResult.Error(AppError.NoCache)
        val malEntries = malListEntryDao.getAll().first()
        val malEntry = malEntries.firstOrNull {
            it.malId == row.malId || it.animeId == row.animeId || it.animeId == id
        }?.toDomain()
        AppResult.Success(row.toDomain(malEntry))
    }

    suspend fun getSeason(
        season: AnimeSeason,
        year: Int,
        page: Int = 0,
        pageSize: Int = DEFAULT_SEASON_PAGE_SIZE
    ): AppResult<List<SeasonalAnimeItem>> = localCall {
        val rows = animeDetailDao.getCachedSeason(
            season = season.name,
            year = year,
            limit = pageSize,
            offset = page.coerceAtLeast(0) * pageSize
        )
        if (rows.isEmpty()) {
            AppResult.Error(AppError.NoCache)
        } else {
            AppResult.Success(rows.map { it.toSeasonalItem() })
        }
    }

    /**
     * Last known airing schedule for the next seven local days. This is a cold Room-backed flow,
     * so the home screen continues to update if another local process refreshes the database.
     */
    fun observeHome(zoneId: ZoneId): Flow<AppResult<List<ScheduleDay>>> {
        val (from, to) = weekRangeUtc(zoneId)
        return combine(
            airingEpisodeDao.getAiringEpisodesInRange(from, to),
            malListEntryDao.getAll()
        ) { episodes, malEntries ->
            val malByMalId = malEntries.associateBy { it.malId }
            val grouped = episodes
                .map { episode -> episode.toDomain(episode.malId?.let { malByMalId[it]?.toDomain() }) }
                .groupBy { episode -> epochSecondsToLocalDate(episode.airingAtEpochSeconds, zoneId) }
                .entries
                .sortedBy { it.key }
                .map { (date, dayEpisodes) -> ScheduleDay(date, dayEpisodes) }
            AppResult.Success(grouped) as AppResult<List<ScheduleDay>>
        }
    }

    private suspend fun <T> localCall(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            AppResult.Error(AppError.Unknown(error.message))
        }

    private fun AnimeDetailEntity.toSearchResult(
        malListEntry: com.owlcoder.animeschedule.domain.model.MalListEntry?
    ) = AnimeSearchResult(
        anilistId = animeId,
        malId = malId,
        title = titleEnglish ?: titleRomaji ?: titleNative ?: "Unknown",
        titleEnglish = titleEnglish,
        coverImageUrl = coverImageUrl,
        type = format,
        year = seasonYear?.toString(),
        meanScore = (meanScore ?: averageScore)?.toDouble(),
        totalEpisodes = episodes,
        userListEntry = malListEntry
    )

    private fun AnimeDetailEntity.toSeasonalItem() = SeasonalAnimeItem(
        anilistId = animeId,
        malId = malId,
        title = titleEnglish ?: titleRomaji ?: titleNative ?: "Unknown",
        coverImageUrl = coverImageUrl,
        coverColor = coverColor,
        genres = genres,
        format = format,
        status = status,
        episodes = episodes,
        season = season,
        seasonYear = seasonYear,
        averageScore = averageScore,
        meanScore = meanScore
    )

    private companion object {
        const val DEFAULT_SEARCH_PAGE_SIZE = 20
        const val DEFAULT_SEASON_PAGE_SIZE = 50
    }
}
