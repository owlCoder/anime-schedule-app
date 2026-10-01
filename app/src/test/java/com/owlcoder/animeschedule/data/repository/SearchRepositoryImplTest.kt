package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.alternative.AlternativeAnimeDataSource
import com.owlcoder.animeschedule.data.api.alternative.CatalogAnime
import com.owlcoder.animeschedule.data.api.alternative.CatalogPage
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.datastore.RecentSearchesDataStore
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.offline.OfflineCatalogDataSource
import com.owlcoder.animeschedule.data.provider.ProviderClock
import com.owlcoder.animeschedule.data.provider.ProviderHealthStore
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.SearchPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SearchRepositoryImplTest {
    private lateinit var aniList: AniListRemoteDataSource
    private lateinit var alternatives: AlternativeAnimeDataSource
    private lateinit var detailDao: AnimeDetailDao
    private lateinit var offline: OfflineCatalogDataSource
    private lateinit var repository: SearchRepositoryImpl

    private val emptyPage = SearchPage(emptyList(), hasNextPage = false)

    @Before
    fun setUp() {
        aniList = mockk()
        alternatives = mockk()
        detailDao = mockk(relaxed = true)
        offline = mockk()
        val malDao = mockk<MalListEntryDao> { every { getAll() } returns flowOf(emptyList()) }
        val clock = ProviderClock { 0L }
        repository = SearchRepositoryImpl(
            aniListDataSource = aniList,
            malListEntryDao = malDao,
            animeDetailDao = detailDao,
            alternativeDataSource = alternatives,
            providerOrchestrator = ProviderOrchestrator(ProviderHealthStore(clock), clock),
            offlineCatalogDataSource = offline,
            recentSearchesDataStore = mockk<RecentSearchesDataStore>(relaxed = true),
        )
        coEvery { offline.search(any(), any(), any()) } returns AppResult.Success(emptyPage)
    }

    private fun catalogAnime(id: String, title: String) = CatalogAnime(
        providerId = "kitsu:$id", anilistId = null, malId = null, title = title,
        titleRomaji = title, titleEnglish = null, coverImageUrl = null,
    )

    @Test
    fun `a search that fails everywhere is an error not an empty result`() = runTest {
        coEvery { aniList.searchAnime(any(), any(), any()) } returns AppResult.Error(AppError.Network("offline"))
        coEvery { alternatives.searchKitsu(any(), any()) } throws IOException("offline")
        coEvery { alternatives.searchAnimeSchedule(any(), any()) } throws IOException("offline")

        val result = repository.searchAnime("naruto")

        assertTrue("expected an error but was $result", result is AppResult.Error)
    }

    @Test
    fun `cached titles are returned when every provider fails`() = runTest {
        coEvery { aniList.searchAnime(any(), any(), any()) } returns AppResult.Error(AppError.Network("offline"))
        coEvery { alternatives.searchKitsu(any(), any()) } throws IOException("offline")
        coEvery { alternatives.searchAnimeSchedule(any(), any()) } throws IOException("offline")
        val cached = SearchPage(
            listOf(AnimeSearchResult(1, null, "Cached", null, null, null, null, null, null, null)),
            hasNextPage = false,
        )
        coEvery { offline.search("naruto", 0, any()) } returns AppResult.Success(cached)

        val result = repository.searchAnime("naruto") as AppResult.Success

        assertEquals("Cached", result.data.results.single().title)
    }

    @Test
    fun `an empty AniList answer is a real answer and does not trigger fallbacks`() = runTest {
        coEvery { aniList.searchAnime(any(), any(), any()) } returns
            AppResult.Success(AniListRemoteDataSource.SearchPageResult(emptyList(), hasNextPage = false))

        val result = repository.searchAnime("zzzz")

        assertTrue(result is AppResult.Success && result.data.results.isEmpty())
        coVerify(exactly = 0) { alternatives.searchKitsu(any(), any()) }
    }

    @Test
    fun `fallback results are cached without overwriting richer existing rows`() = runTest {
        coEvery { aniList.searchAnime(any(), any(), any()) } returns AppResult.Error(AppError.Network("down"))
        coEvery { alternatives.searchKitsu("bebop", 1) } returns
            CatalogPage(listOf(catalogAnime("1", "Cowboy Bebop")), hasNextPage = false)
        val cached = slot<List<com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity>>()
        coEvery { detailDao.insertIfAbsent(capture(cached)) } returns Unit

        val result = repository.searchAnime("bebop") as AppResult.Success

        assertEquals("Cowboy Bebop", result.data.results.single().title)
        assertEquals(1, cached.captured.size)
        coVerify(exactly = 0) { detailDao.upsert(any()) }
    }

    @Test
    fun `queries are trimmed and too-short queries never reach a provider`() = runTest {
        val short = repository.searchAnime("  a ")

        assertTrue(short is AppResult.Success && short.data.results.isEmpty())
        coVerify(exactly = 0) { aniList.searchAnime(any(), any(), any()) }

        coEvery { aniList.searchAnime("naruto", 1, any()) } returns
            AppResult.Success(AniListRemoteDataSource.SearchPageResult(emptyList(), false))
        repository.searchAnime("  naruto  ")
        coVerify { aniList.searchAnime("naruto", 1, any()) }
    }
}
