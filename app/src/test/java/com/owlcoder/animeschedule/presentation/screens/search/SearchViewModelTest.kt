package com.owlcoder.animeschedule.presentation.screens.search

import com.owlcoder.animeschedule.core.result.*
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.*
import com.owlcoder.animeschedule.presentation.screens.discovery.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private class FakeSearch : SearchRepository {
        override val recentSearches = MutableStateFlow(listOf("Alpha", "Beta"))
        var search: suspend (String, Int) -> AppResult<SearchPage> = { _, _ -> AppResult.Success(SearchPage(emptyList(), false)) }
        override suspend fun searchAnime(query: String, page: Int) = search(query, page)
        override suspend fun saveRecentSearch(query: String) { recentSearches.value = listOf(query) + recentSearches.value.filterNot { it == query } }
        override suspend fun clearRecentSearches() { recentSearches.value = emptyList() }
        override suspend fun removeRecentSearch(query: String) { recentSearches.value -= query }
    }
    private class FakeMal : MalRepository {
        val entries = MutableStateFlow<List<MalListEntry>>(emptyList())
        override fun getUserList() = entries
        override suspend fun updateListEntry(animeId: Int, update: MalListUpdate) = AppResult.Success(Unit)
        override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
        override suspend fun removeListEntry(animeId: Int) = AppResult.Success(Unit)
        override suspend fun refreshUserList(force: Boolean) = true
        override suspend fun flushPendingUpdates() = true
    }
    @Before fun before() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun after() { Dispatchers.resetMain() }
    private fun item(id: Int) = AnimeSearchResult(id, id, "Title $id", null, null, "TV", null, null, 12, null)
    private fun TestScope.observe(vm: SearchViewModel) = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

    @Test fun `failed next page retains results and retries same page with deduplication`() = runTest {
        val repo = FakeSearch(); val pages = mutableListOf<Int>()
        repo.search = { _, page -> pages += page; if (page == 0) AppResult.Success(SearchPage(listOf(item(1)), true)) else if (pages.count { it == 1 } == 1) AppResult.Error(AppError.NoCache) else AppResult.Success(SearchPage(listOf(item(1), item(2)), false)) }
        val vm = SearchViewModel(repo, FakeMal()); observe(vm); vm.setQuery("Title"); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        assertTrue(vm.uiState.value.loadMoreError); assertEquals(listOf(1), vm.uiState.value.results.map { it.anilistId })
        vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf(0, 1, 1), pages); assertFalse(vm.uiState.value.loadMoreError)
        assertEquals(listOf(1, 2), vm.uiState.value.results.map { it.anilistId }); assertFalse(vm.uiState.value.hasNextPage)
    }
    @Test fun `old noncancellable response cannot enter new query during debounce`() = runTest {
        val gate = CompletableDeferred<Unit>(); val repo = FakeSearch()
        repo.search = { query, _ -> if (query == "old") withContext(NonCancellable) { gate.await(); AppResult.Success(SearchPage(listOf(item(1)), false)) } else AppResult.Success(SearchPage(listOf(item(2)), false)) }
        val vm = SearchViewModel(repo, FakeMal()); observe(vm); vm.setQuery("old"); advanceTimeBy(301); runCurrent()
        vm.setQuery("new"); gate.complete(Unit); runCurrent()
        assertTrue(vm.uiState.value.results.isEmpty()); assertEquals("new", vm.uiState.value.query)
        advanceUntilIdle(); assertEquals(listOf(2), vm.uiState.value.results.map { it.anilistId })
    }
    @Test fun `tracking filter follows live MAL changes without another network call`() = runTest {
        val repo = FakeSearch(); var calls = 0; repo.search = { _, _ -> calls++; AppResult.Success(SearchPage(listOf(item(1), item(2)), false)) }
        val mal = FakeMal(); val vm = SearchViewModel(repo, mal); observe(vm); vm.setQuery("Title"); advanceUntilIdle()
        vm.setTracking(TrackingFilter.TRACKED); runCurrent(); assertTrue(vm.uiState.value.results.isEmpty())
        mal.entries.value = listOf(MalListEntry(2, "Title 2", status = WatchStatus.WATCHING, episodesWatched = 0, score = 0, totalEpisodes = 12)); runCurrent()
        assertEquals(listOf(2), vm.uiState.value.results.map { it.anilistId }); assertEquals(2, vm.uiState.value.loadedCount); assertEquals(1, calls)
    }
    @Test fun `surrounding whitespace edits keep results and do not refetch`() = runTest {
        val repo = FakeSearch()
        val queries = mutableListOf<String>()
        repo.search = { query, _ -> queries += query; AppResult.Success(SearchPage(listOf(item(1)), true)) }
        val vm = SearchViewModel(repo, FakeMal()); observe(vm)
        vm.setQuery("  Title "); advanceUntilIdle()
        vm.setQuery("Title  "); advanceUntilIdle()
        assertEquals(listOf("Title"), queries)
        assertEquals("Title", vm.uiState.value.query)
        assertEquals(listOf(1), vm.uiState.value.results.map { it.anilistId })
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.hasNextPage)
    }

    @Test fun `deleting one recent search preserves other history`() = runTest {
        val repo = FakeSearch(); val vm = SearchViewModel(repo, FakeMal())
        vm.removeRecentSearch("Alpha"); advanceUntilIdle(); assertEquals(listOf("Beta"), repo.recentSearches.value)
    }

    @Test fun `unexpected first page failure stops spinner and retry succeeds`() = runTest {
        val repo = FakeSearch(); var calls = 0
        repo.search = { _, _ -> if (++calls == 1) throw IllegalStateException("fixture") else AppResult.Success(SearchPage(listOf(item(1)), false)) }
        val vm = SearchViewModel(repo, FakeMal()); observe(vm); vm.setQuery("Title"); advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading); assertNotNull(vm.uiState.value.errorRes)
        vm.retrySearch(); advanceUntilIdle()
        assertNull(vm.uiState.value.errorRes); assertEquals(listOf(1), vm.uiState.value.results.map { it.anilistId })
    }

    @Test fun `unexpected next page failure preserves page and retry cursor`() = runTest {
        val repo = FakeSearch(); val pages = mutableListOf<Int>()
        repo.search = { _, page ->
            pages += page
            if (page == 0) AppResult.Success(SearchPage(listOf(item(1)), true))
            else if (pages.count { it == 1 } == 1) throw IllegalStateException("fixture")
            else AppResult.Success(SearchPage(listOf(item(2)), false))
        }
        val vm = SearchViewModel(repo, FakeMal()); observe(vm); vm.setQuery("Title"); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoadingMore); assertTrue(vm.uiState.value.loadMoreError)
        assertEquals(listOf(1), vm.uiState.value.results.map { it.anilistId })
        vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf(0, 1, 1), pages); assertEquals(listOf(1, 2), vm.uiState.value.results.map { it.anilistId })
    }

    @Test fun `score interval remains valid and status follows live list without refetch`() = runTest {
        val repo = FakeSearch(); var calls = 0
        repo.search = { _, _ -> calls++; AppResult.Success(SearchPage(listOf(item(1)), false)) }
        val mal = FakeMal(); val vm = SearchViewModel(repo, mal); observe(vm)
        vm.setQuery("Title"); advanceUntilIdle()
        vm.setMinimumScore(9); vm.setMaximumScore(7); runCurrent()
        assertEquals(7, vm.uiState.value.filter.minimumScore); assertEquals(7, vm.uiState.value.filter.maximumScore)
        vm.setMinimumScore(10); runCurrent(); assertEquals(10, vm.uiState.value.filter.maximumScore)
        vm.clearFilter(); vm.setWatchStatus(WatchStatus.ON_HOLD); runCurrent()
        mal.entries.value = listOf(MalListEntry(1, "Title 1", status = WatchStatus.ON_HOLD, episodesWatched = 0, score = 0, totalEpisodes = 12)); runCurrent()
        assertEquals(1, vm.uiState.value.results.size)
        mal.entries.value = mal.entries.value.map { it.copy(status = WatchStatus.WATCHING) }; runCurrent()
        assertTrue(vm.uiState.value.results.isEmpty()); assertEquals(1, calls)
        vm.setTracking(TrackingFilter.UNTRACKED); runCurrent()
        assertNull(vm.uiState.value.filter.watchStatus)
    }
}
