package com.owlcoder.animeschedule.presentation.screens.seasonal

import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.domain.repository.SeasonalRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonalViewModelTest {

    private class FakeSeasonalRepository : SeasonalRepository {
        val gates = mutableMapOf<AnimeSeason, CompletableDeferred<List<SeasonalAnimeItem>>>()
        override suspend fun getSeasonalAnime(season: AnimeSeason, year: Int): AppResult<List<SeasonalAnimeItem>> =
            AppResult.Success(gates.getValue(season).await())
    }

    private object EmptyMalRepository : MalRepository {
        override fun getUserList(): Flow<List<MalListEntry>> = flowOf(emptyList())
        override suspend fun updateListEntry(animeId: Int, update: MalListUpdate) = AppResult.Success(Unit)
        override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
        override suspend fun removeListEntry(animeId: Int) = AppResult.Success(Unit)
        override suspend fun refreshUserList(force: Boolean) = true
        override suspend fun flushPendingUpdates() = true
    }

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun item(id: Int, title: String) = SeasonalAnimeItem(
        anilistId = id, malId = null, title = title, coverImageUrl = null, coverColor = null,
        genres = emptyList(), format = "TV", status = null, episodes = null, season = null,
        seasonYear = null, averageScore = null, meanScore = null,
    )

    @Test
    fun `a slow earlier season never overwrites the season the user switched to`() = runTest {
        val repo = FakeSeasonalRepository().apply {
            AnimeSeason.entries.forEach { gates[it] = CompletableDeferred() }
        }
        val viewModel = SeasonalViewModel(repo, EmptyMalRepository)
        // Initial load (current season) is now in flight; the user immediately picks another one.
        advanceUntilIdle()
        viewModel.setSeason(AnimeSeason.WINTER, 2020)
        viewModel.setSeason(AnimeSeason.SUMMER, 2021)
        advanceUntilIdle()

        // Responses arrive out of order: the superseded WINTER one comes last.
        repo.gates.getValue(AnimeSeason.SUMMER).complete(listOf(item(2, "Summer show")))
        advanceUntilIdle()
        repo.gates.getValue(AnimeSeason.WINTER).complete(listOf(item(1, "Winter show")))
        repo.gates.filterKeys { it != AnimeSeason.SUMMER && it != AnimeSeason.WINTER }
            .values.forEach { it.complete(listOf(item(3, "Other show"))) }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnimeSeason.SUMMER, state.season)
        assertEquals(listOf("Summer show"), state.allItems.map { it.title })
    }

    @Test
    fun `title sort ignores case`() = runTest {
        val repo = FakeSeasonalRepository().apply {
            AnimeSeason.entries.forEach { gates[it] = CompletableDeferred(listOf(item(1, "beta"), item(2, "Alpha"), item(3, "gamma"))) }
        }
        val viewModel = SeasonalViewModel(repo, EmptyMalRepository)
        advanceUntilIdle()

        viewModel.setSortOrder(SeasonalSortOrder.TITLE)

        assertEquals(listOf("Alpha", "beta", "gamma"), viewModel.uiState.value.filteredItems.map { it.title })
    }
    @Test fun `season search trims text and hiding tracked anime uses MAL identity`() {
        val items = listOf(item(1, "Alpha").copy(malId = 101), item(2, "Beta").copy(malId = 102), item(3, "ALPHA OVA"))
        assertEquals(listOf(1, 3), items.applyFilter(SeasonalFilter(query = "  alpha  ")).map { it.anilistId })
        assertEquals(listOf(3), items.applyFilter(SeasonalFilter(query = "alpha", hideTracked = true), setOf(101)).map { it.anilistId })
        assertEquals(listOf(1, 3), items.applyFilter(SeasonalFilter(query = "alpha", hideTracked = true), setOf(1)).map { it.anilistId })
    }

}
