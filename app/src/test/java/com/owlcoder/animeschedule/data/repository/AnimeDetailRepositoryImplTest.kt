package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.alternative.AlternativeAnimeDataSource
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.offline.OfflineCatalogDataSource
import com.owlcoder.animeschedule.data.provider.ProviderClock
import com.owlcoder.animeschedule.data.provider.ProviderHealthStore
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

class AnimeDetailRepositoryImplTest {
    private lateinit var detailDao: AnimeDetailDao
    private lateinit var aniList: AniListRemoteDataSource
    private lateinit var alternatives: AlternativeAnimeDataSource
    private lateinit var offline: OfflineCatalogDataSource
    private lateinit var repository: AnimeDetailRepositoryImpl

    @Before
    fun setUp() {
        detailDao = mockk(relaxed = true)
        aniList = mockk()
        alternatives = mockk()
        offline = mockk()
        val malDao = mockk<MalListEntryDao> {
            every { observeByMalId(any()) } returns flowOf(null)
            every { observeByAnimeId(any()) } returns flowOf(null)
        }
        val clock = ProviderClock { 0L }
        repository = AnimeDetailRepositoryImpl(
            animeDetailDao = detailDao,
            malListEntryDao = malDao,
            aniListDataSource = aniList,
            alternativeDataSource = alternatives,
            providerOrchestrator = ProviderOrchestrator(ProviderHealthStore(clock), clock),
            offlineCatalogDataSource = offline,
        )
    }

    private fun entity(id: Int, cachedAt: Long) = AnimeDetailEntity(
        animeId = id, malId = 500 + id, titleRomaji = "Cached $id", titleEnglish = null, titleNative = null,
        coverImageUrl = null, coverColor = null, bannerImageUrl = null, description = null,
        genres = emptyList(), averageScore = null, meanScore = null, episodes = 12, duration = 24,
        status = "FINISHED", format = "TV", season = null, seasonYear = null, nextAiringEpisode = null,
        nextAiringAt = null, studiosJson = null, charactersJson = null, relationsJson = null,
        trailerSite = null, trailerId = null, siteUrl = null, cachedAtEpochSeconds = cachedAt,
    )

    private fun cache(entity: AnimeDetailEntity) {
        coEvery { detailDao.getByIdOnce(entity.animeId) } returns entity
        every { detailDao.getById(entity.animeId) } returns flowOf(entity)
    }

    @Test
    fun `a stale cached detail is shown immediately while the refresh is still running`() = runTest {
        cache(entity(7, cachedAt = 1L)) // far older than the one hour TTL
        val networkGate = CompletableDeferred<AppResult<com.owlcoder.animeschedule.data.api.anilist.generated.AnimeDetailQuery.Media>>()
        coEvery { aniList.getAnimeDetail(7) } coAnswers { networkGate.await() }
        coEvery { alternatives.getByAniListId(any()) } returns null

        // Would hang (and fail by timeout) if the first emission waited for the network.
        val first = withTimeout(1_000) { repository.getAnimeDetail(7).first() }

        assertEquals("Cached 7", (first as AppResult.Success).data.titleRomaji)
        networkGate.cancel()
    }

    @Test
    fun `a fresh cached detail causes no network traffic`() = runTest {
        cache(entity(7, cachedAt = Instant.now().epochSecond))

        val first = repository.getAnimeDetail(7).first()

        assertTrue(first is AppResult.Success)
        coVerify(exactly = 0) { aniList.getAnimeDetail(any()) }
        coVerify(exactly = 0) { alternatives.getByAniListId(any()) }
    }

    @Test
    fun `a network failure does not trigger a second doomed MAL lookup`() = runTest {
        coEvery { detailDao.getByIdOnce(any()) } returns null
        coEvery { detailDao.getByMalId(any()) } returns null
        coEvery { aniList.getAnimeDetail(42) } returns AppResult.Error(AppError.Network("offline"))
        coEvery { alternatives.getByAniListId(42) } throws IOException("offline")
        coEvery { alternatives.getByMalId(42) } throws IOException("offline")
        coEvery { offline.getDetail(42) } returns AppResult.Error(AppError.NoCache)

        val first = repository.getAnimeDetail(42).first()

        assertEquals(AppResult.Error(AppError.NoCache), first)
        coVerify(exactly = 0) { aniList.getAnimeDetailByMalId(any()) }
    }

    @Test
    fun `a not-found AniList id falls back to the MAL id lookup`() = runTest {
        coEvery { detailDao.getByIdOnce(any()) } returns null
        coEvery { detailDao.getByMalId(any()) } returns null
        coEvery { aniList.getAnimeDetail(42) } returns AppResult.Error(AppError.NoCache)
        coEvery { aniList.getAnimeDetailByMalId(42) } returns AppResult.Error(AppError.NoCache)
        coEvery { alternatives.getByAniListId(any()) } returns null
        coEvery { alternatives.getByMalId(any()) } returns null
        coEvery { offline.getDetail(42) } returns AppResult.Error(AppError.NoCache)

        repository.getAnimeDetail(42).first()

        coVerify(exactly = 1) { aniList.getAnimeDetailByMalId(42) }
    }
}
