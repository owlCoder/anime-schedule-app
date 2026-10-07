package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.result.*
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.*
import com.owlcoder.animeschedule.presentation.screens.discovery.*
import com.owlcoder.animeschedule.presentation.screens.search.*
import com.owlcoder.animeschedule.presentation.screens.seasonal.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*

class DiscoveryWorkspaceUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    @After fun cleanup() { instrumentation.runOnMainSync { store.clear() } }
    private fun text(id: Int) = instrumentation.targetContext.getString(id)
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { AnimeScheduleTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, options = ThemeOptions(palette = if (dark) ThemePalette.NEON else ThemePalette.SAKURA), content = {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
        }) }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle(); instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-570-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
    private val malEntry = MalListEntry(101, "Alpha Adventure", status = WatchStatus.WATCHING, episodesWatched = 4, score = 8, totalEpisodes = 12)
    private val searchItems = listOf(
        AnimeSearchResult(1, 101, "Alpha Adventure", null, null, "TV", "2025", 80.0, 12, null),
        AnimeSearchResult(2, 102, "Beta: A Very Long Anime Title Across Multiple Lines", null, null, "MOVIE", "2024", 90.0, 1, null),
        AnimeSearchResult(3, null, "Gamma Unknown", null, null, null, null, null, null, null),
    )
    private val seasonItems = listOf(
        SeasonalAnimeItem(1, 101, "Alpha Adventure", null, null, listOf("Action"), "TV", "RELEASING", 12, "WINTER", 2025, 80, null),
        SeasonalAnimeItem(2, 102, "Beta: A Very Long Anime Title Across Multiple Lines", null, null, listOf("Drama"), "MOVIE", "FINISHED", 1, "WINTER", 2025, 90, null),
    )
    private val mal = object : MalRepository {
        override fun getUserList() = flowOf(listOf(malEntry))
        override suspend fun updateListEntry(animeId: Int, update: MalListUpdate) = AppResult.Success(Unit)
        override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
        override suspend fun removeListEntry(animeId: Int) = AppResult.Success(Unit)
        override suspend fun refreshUserList(force: Boolean) = true
        override suspend fun flushPendingUpdates() = true
    }
    private fun seasonVm(): SeasonalViewModel {
        lateinit var vm: SeasonalViewModel
        instrumentation.runOnMainSync {
            vm = SeasonalViewModel(object : SeasonalRepository {
                override suspend fun getSeasonalAnime(season: AnimeSeason, year: Int) = AppResult.Success(seasonItems)
            }, mal)
            vm.setSeason(AnimeSeason.WINTER, 2025); store.put("season", vm)
        }
        return vm
    }

    @Test fun searchFiltersExposeTrackingSortAndFormatWithReset() {
        var filter by mutableStateOf(SearchFilter())
        show { SearchFilterSheet(filter, listOf("TV", "MOVIE"), { filter = filter.copy(tracking = it) }, { filter = filter.copy(formats = if (it in filter.formats) filter.formats - it else filter.formats + it) }, { filter = filter.copy(sort = it) }, { filter = SearchFilter() }, {}) }
        compose.onNodeWithTag("search-tracking-UNTRACKED").performClick()
        compose.onNodeWithTag("search-tracking-UNTRACKED").assertIsSelected()
        screenshot("search-filters")
        compose.onNodeWithTag("search-filter-list").performScrollToNode(hasTestTag("search-sort-EPISODES"))
        compose.onNodeWithTag("search-sort-EPISODES").performClick()
        compose.onNodeWithTag("search-filter-list").performScrollToNode(hasTestTag("search-format-MOVIE"))
        compose.onNodeWithTag("search-format-MOVIE").performClick().assertIsOn()
        assertEquals(SearchFilter(TrackingFilter.UNTRACKED, setOf("MOVIE"), SearchSort.EPISODES), filter)
        screenshot("search-formats")
        compose.onNodeWithText(text(R.string.seasonal_filter_reset)).performClick()
        assertEquals(SearchFilter(), filter)
    }

    @Test fun individualHistoryRemovalDoesNotSelectOrDeleteOtherSearches() {
        var searches by mutableStateOf(listOf("Alpha Adventure", "Beta Journey")); var selected = ""
        show(true) { RecentSearches(searches, { searches = emptyList() }, { selected = it }, { searches = searches - it }) }
        compose.onNodeWithTag("recent-remove-Alpha Adventure").performClick()
        assertEquals(listOf("Beta Journey"), searches); assertEquals("", selected)
        compose.onNodeWithText("Alpha Adventure").assertDoesNotExist()
        screenshot("recent-searches")
        compose.onNodeWithText("Beta Journey").performClick(); assertEquals("Beta Journey", selected)
    }

    @Test fun filteredSearchCanLoadMoreAndFailedPageHasExplicitRetry() {
        var attempts = 0
        show { SearchResults(emptyList(), true, false, 20.dp, {}, {}, { attempts++ }, true, {}) }
        compose.onNodeWithText(text(R.string.discovery_filtered_empty)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.discovery_page_error)).assertIsDisplayed()
        screenshot("search-retry")
        compose.onNodeWithTag("search-load-more").performClick(); assertEquals(1, attempts)
    }

    @Test fun searchScreenValidatesShortQueriesAndUpdatesLiveFilteredResults() {
        lateinit var vm: SearchViewModel
        val repo = object : SearchRepository {
            override val recentSearches = flowOf(emptyList<String>())
            override suspend fun searchAnime(query: String, page: Int) = AppResult.Success(SearchPage(searchItems, false))
            override suspend fun saveRecentSearch(query: String) {}
            override suspend fun clearRecentSearches() {}
            override suspend fun removeRecentSearch(query: String) {}
        }
        instrumentation.runOnMainSync { vm = SearchViewModel(repo, mal); store.put("search", vm) }
        show(true) { SearchScreen({}, viewModel = vm) }
        val field = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("anime-search-field")))
        field.performTextInput("a")
        screenshot("search-minimum")
        compose.onNodeWithText(text(R.string.discovery_min_query)).assertIsDisplayed()
        field.performTextReplacement("anime")
        field.performImeAction()
        compose.waitUntil(5000) { vm.uiState.value.loadedCount == 3 }
        screenshot("search-results")
        compose.onNodeWithTag("search-filters").performClick()
        compose.onNodeWithTag("search-tracking-TRACKED").performClick()
        compose.onNodeWithText(text(R.string.seasonal_filter_apply)).performClick()
        compose.waitUntil(5000) { vm.uiState.value.results.size == 1 }
        compose.onNodeWithText("Alpha Adventure").assertIsDisplayed()
        compose.onNodeWithText("Gamma Unknown").assertDoesNotExist()
        screenshot("search-tracked")
    }

    @Test fun seasonalReleaseLengthAndScoreCombineAndReset() {
        var filter by mutableStateOf(SeasonalFilter())
        show { SeasonalFilterSheet(filter, listOf("Action", "Drama"), listOf("TV", "MOVIE"), {}, {}, { filter = filter.copy(sortOrder = it) }, { filter = filter.copy(release = it) }, { filter = filter.copy(length = it) }, { filter = filter.copy(minimumScore = it) }, { filter = SeasonalFilter(sortOrder = filter.sortOrder) }, {}) }
        compose.onNodeWithTag("season-filter-list").performScrollToNode(hasTestTag("season-release-FINISHED"))
        compose.onNodeWithTag("season-release-FINISHED").performClick().assertIsSelected()
        screenshot("season-release")
        compose.onNodeWithTag("season-filter-list").performScrollToNode(hasTestTag("season-length-SHORT"))
        compose.onNodeWithTag("season-length-SHORT").performClick().assertIsSelected()
        compose.onNodeWithTag("season-filter-list").performScrollToNode(hasTestTag("season-score-8"))
        compose.onNodeWithTag("season-score-8").performClick().assertIsSelected()
        assertEquals(ReleaseFilter.FINISHED, filter.release); assertEquals(EpisodeLength.SHORT, filter.length); assertEquals(8, filter.minimumScore)
        screenshot("season-score")
        compose.onNodeWithText(text(R.string.seasonal_filter_reset)).performClick(); assertFalse(filter.isActive)
    }

    @Test fun seasonalListLayoutYearNavigationAndRandomPickUseVisibleResults() {
        val vm = seasonVm(); var chosen = 0
        show(true) { SeasonalOverlay({ chosen = it }, {}, vm) }
        compose.waitUntil(5000) { !vm.uiState.value.isLoading }
        compose.onNodeWithTag("season-previous").performClick()
        compose.waitUntil(5000) { vm.uiState.value.season == AnimeSeason.FALL && !vm.uiState.value.isLoading }
        assertEquals(2024, vm.uiState.value.year)
        compose.onNodeWithTag("season-next").performClick()
        compose.waitUntil(5000) { vm.uiState.value.season == AnimeSeason.WINTER && !vm.uiState.value.isLoading }
        assertEquals(2025, vm.uiState.value.year)
        compose.onNodeWithTag("season-layout").performClick()
        compose.onNodeWithTag("season-results-list").assertIsDisplayed()
        screenshot("season-list")
        compose.runOnIdle { vm.setRelease(ReleaseFilter.FINISHED) }
        compose.onNodeWithText("Alpha Adventure").assertDoesNotExist()
        compose.onNodeWithTag("season-random").performClick(); assertEquals(2, chosen)
        compose.runOnIdle { vm.setMinimumScore(10) }
        compose.onNodeWithTag("season-random").assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.seasonal_empty_title)).assertIsDisplayed()
        screenshot("season-empty")
    }
}
