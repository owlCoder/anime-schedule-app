package com.owlcoder.animeschedule.data.local.offline

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeEntity
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryEntity
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import java.time.LocalDate
import java.time.ZoneOffset

class OfflineCatalogDataSourceTest {
    private lateinit var detailDao: AnimeDetailDao
    private lateinit var airingDao: AiringEpisodeDao
    private lateinit var malListDao: MalListEntryDao
    private lateinit var source: OfflineCatalogDataSource

    @Before
    fun setUp() {
        detailDao = mockk()
        airingDao = mockk()
        malListDao = mockk()
        every { malListDao.getAll() } returns flowOf(emptyList())
        source = OfflineCatalogDataSource(detailDao, airingDao, malListDao)
    }

    @Test
    fun `search returns only previously cached rows while offline`() = runTest {
        val row = detail(123, 456, "Cached One Piece")
        coEvery { detailDao.searchByTitle("one piece", 20, 0) } returns listOf(row)

        val result = source.search(" one piece ")

        assertTrue(result is AppResult.Success)
        val item = (result as AppResult.Success).data.results.single()
        assertEquals(123, item.anilistId)
        assertEquals(456, item.malId)
        assertEquals("Cached One Piece", item.title)
    }

    @Test
    fun `detail resolves a cached row by MAL id without changing its canonical local id`() = runTest {
        val row = detail(123, 456, "Cached Detail")
        coEvery { detailDao.getByIdOnce(456) } returns null
        coEvery { detailDao.getByMalId(456) } returns row

        val result = source.getDetail(456)

        assertTrue(result is AppResult.Success)
        assertEquals(123, (result as AppResult.Success).data.animeId)
        assertEquals(456, result.data.malId)
    }

    @Test
    fun `season returns matching cached metadata and no fabricated rows`() = runTest {
        val row = detail(123, 456, "Cached Summer")
        coEvery { detailDao.getCachedSeason("SUMMER", 2026, 50, 0) } returns listOf(row)

        val result = source.getSeason(AnimeSeason.SUMMER, 2026)

        assertTrue(result is AppResult.Success)
        assertEquals("Cached Summer", (result as AppResult.Success).data.single().title)

        coEvery { detailDao.getCachedSeason("WINTER", 2020, 50, 0) } returns emptyList()
        assertTrue(source.getSeason(AnimeSeason.WINTER, 2020) is AppResult.Error)
        assertTrue(source.getSeason(AnimeSeason.WINTER, 2020) is AppResult.Error)
        assertEquals(AppError.NoCache, (source.getSeason(AnimeSeason.WINTER, 2020) as AppResult.Error).error)
    }

    @Test
    fun `home exposes cached airing rows and local MAL list state`() = runTest {
        val nextDay = LocalDate.now(ZoneOffset.UTC).plusDays(1)
        val airing = AiringEpisodeEntity(
            airingId = 1,
            animeId = 123,
            malId = 456,
            episode = 4,
            airingAtEpochSeconds = nextDay.atStartOfDay(ZoneOffset.UTC).toEpochSecond(),
            title = "Cached Episode",
            titleRomaji = "Cached Episode",
            coverImageUrl = null,
            coverColor = null,
            genres = emptyList(),
            averageScore = 80,
            totalEpisodes = 12,
            status = "RELEASING",
            format = "TV",
            cachedAtEpochSeconds = nextDay.atStartOfDay(ZoneOffset.UTC).toEpochSecond(),
            source = "offline-test"
        )
        val listEntry = MalListEntryEntity(
            animeId = 123,
            malId = 456,
            title = "Cached Episode",
            coverImageUrl = null,
            totalEpisodes = 12,
            status = "watching",
            numEpisodesWatched = 3,
            score = 8,
            updatedAt = null
        )
        every { airingDao.getAiringEpisodesInRange(any(), any()) } returns flowOf(listOf(airing))
        every { malListDao.getAll() } returns flowOf(listOf(listEntry))

        val result = source.observeHome(ZoneOffset.UTC).first()

        assertTrue(result is AppResult.Success)
        val episode = (result as AppResult.Success).data.single().episodes.single()
        assertEquals(4, episode.episode)
        assertEquals(3, episode.malListEntry?.episodesWatched)
    }

    private fun detail(animeId: Int, malId: Int, title: String) = AnimeDetailEntity(
        animeId = animeId,
        malId = malId,
        titleRomaji = title,
        titleEnglish = title,
        titleNative = null,
        coverImageUrl = null,
        coverColor = null,
        bannerImageUrl = null,
        description = null,
        genres = emptyList(),
        averageScore = 80,
        meanScore = 82,
        episodes = 12,
        duration = 24,
        status = "FINISHED",
        format = "TV",
        season = "SUMMER",
        seasonYear = 2026,
        nextAiringEpisode = null,
        nextAiringAt = null,
        studiosJson = null,
        relationsJson = null,
        trailerSite = null,
        trailerId = null,
        siteUrl = null,
        cachedAtEpochSeconds = 1_798_700_000L
    )
}
