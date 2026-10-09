package com.owlcoder.animeschedule

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.*
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.local.datastore.*
import com.owlcoder.animeschedule.data.local.db.AnimeScheduleDatabase
import com.owlcoder.animeschedule.data.repository.SettingsRepositoryImpl
import com.owlcoder.animeschedule.data.work.CacheMaintenance
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.mylist.*
import com.owlcoder.animeschedule.presentation.screens.discovery.*
import com.owlcoder.animeschedule.presentation.screens.search.SearchFilterSheet
import com.owlcoder.animeschedule.presentation.screens.settings.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*

class Workspace5120UiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val today = LocalDate.of(2026, 10, 8)
    private val entries = listOf(
        MalListEntry(101, "Alpha Adventure", status = WatchStatus.WATCHING, episodesWatched = 4, score = 8, totalEpisodes = 5),
        MalListEntry(102, "Beta Journey", status = WatchStatus.ON_HOLD, episodesWatched = 0, score = 0, totalEpisodes = 1),
        MalListEntry(103, "Gamma Story", status = WatchStatus.PLAN_TO_WATCH, episodesWatched = 0, score = 0, totalEpisodes = 1))
    private fun show(dark: Boolean = true, large: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            val context = LocalContext.current; val density = LocalDensity.current
            val activityResults = requireNotNull(LocalActivityResultRegistryOwner.current)
            val config = Configuration(LocalConfiguration.current).apply { if (large) setLocale(Locale.forLanguageTag("sr-Latn")) }
            val localized = context.createConfigurationContext(config)
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides activityResults,
                LocalContext provides (if (large) localized else context), LocalConfiguration provides config, LocalResources provides localized.resources,
                LocalDensity provides Density(density.density, if (large) 1.35f else 1f)) {
                AnimeScheduleTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, options = ThemeOptions(amoled = dark, palette = ThemePalette.SAKURA), content = content)
            }
        }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5120-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
    @Test fun expandedSmartFiltersScrollAndSelectWithLargeSerbianText() {
        var selected by mutableStateOf(SmartListFilter.ALL)
        show(large = true) { SmartFiltersSheet(selected, { selected = it }, {}) }
        compose.onNodeWithTag("smart-filter-list").performScrollToNode(hasTestTag("smart-COMPLETED_UNRATED"))
        compose.onNodeWithTag("smart-COMPLETED_UNRATED").performClick().assertIsSelected()
        assertEquals(SmartListFilter.COMPLETED_UNRATED, selected)
        screenshot("smart-large-dark")
    }
    @Test fun savedViewRenameRejectsDuplicateAndKeepsSettingsWhenReordered() {
        var tools by mutableStateOf(WatchTools(savedViews = listOf(SavedListView("First", query = "Alpha", sort = "OLDEST"), SavedListView("Second"))))
        show(dark = false) { SavedViewsSheet(tools.savedViews, {}, {}, {}, {}, { old, name -> tools = tools.renameView(old, name) }, { name, offset -> tools = tools.moveView(name, offset) }) }
        compose.onNodeWithTag("view-rename-First").performScrollTo().performClick()
        compose.onNodeWithTag("view-rename-field").performTextReplacement("Second")
        compose.onNodeWithTag("view-rename-save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("view-rename-field").performTextReplacement("Weekend")
        compose.onNodeWithTag("view-rename-save").performScrollTo().performClick()
        compose.onNodeWithTag("view-down-Weekend").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("Second", "Weekend"), tools.savedViews.map { it.name }); assertEquals("Alpha", tools.savedViews.last().query) }
        compose.onNodeWithTag("view-up-Weekend").performScrollTo().assertIsEnabled()
        screenshot("views-light")
    }
    @Test fun plannerIncludesPausedAndPlannedTitlesAndBudgetsBreaksWithoutEditingProgress() {
        show(large = true) { WatchPlannerSheet(entries, WatchTools(), {}, {}) }
        compose.onNodeWithTag("planner-budget-120").performClick()
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("planner-paused"))
        compose.onNodeWithTag("planner-paused").performClick().assertIsOn()
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("planner-planned"))
        compose.onNodeWithTag("planner-planned").performClick().assertIsOn()
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("planner-break-increase"))
        repeat(3) { compose.onNodeWithTag("planner-break-increase").performClick() }
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("planner-rest-total"))
        compose.onNodeWithTag("planner-rest-total").assertIsDisplayed()
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("plan-range-101"))
        compose.onNodeWithTag("plan-range-101").assertTextContains("5", substring = true)
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("plan-range-103"))
        compose.onNodeWithTag("plan-range-103").assertIsDisplayed()
        assertEquals(4, entries.first().episodesWatched)
        compose.onNodeWithTag("planner-list").performScrollToIndex(0)
        screenshot("planner-large-dark")
    }
    @Test fun sharingPlanOpensNativeChooserAndReturnsWithoutSending() {
        show { WatchPlannerSheet(entries, WatchTools(), {}, {}) }
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("planner-share"))
        compose.onNodeWithTag("planner-share").performClick()
        compose.waitUntil(10_000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("intentresolver") == true }
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
        compose.waitUntil(10_000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName }
        compose.onNodeWithTag("planner-share").assertIsDisplayed()
    }
    @Test fun activityPeriodsComparisonAndTrendRemainReadableInLargeLightTheme() {
        val tools = WatchTools(activity = listOf(WatchActivity(101, "Alpha Adventure", today.toString(), 2, 4), WatchActivity(102, "Beta Journey", today.minusDays(8).toString(), 3, 3)))
        val state = MyListUiState(allEntries = entries, tools = tools, insights = entries.insights(), statusCounts = entries.groupingBy { it.status }.eachCount())
        show(dark = false, large = true) { CompositionLocalProvider(LocalWatchTools provides WatchToolsActions(data = tools, today = today)) { LibraryInsightsSheet(state, {}) } }
        compose.onNodeWithTag("library-insights").performScrollToNode(hasTestTag("insights-period-7"))
        compose.onNodeWithTag("insights-period-7").performClick().assertIsSelected()
        screenshot("insights-period-large-light")
        compose.onNodeWithTag("library-insights").performScrollToNode(hasTestTag("activity-trend-$today"))
        compose.onNodeWithTag("activity-trend-$today").assertIsDisplayed()
        val average = compose.onNodeWithTag("insights-average").fetchSemanticsNode().boundsInRoot
        val change = compose.onNodeWithTag("insights-change").fetchSemanticsNode().boundsInRoot
        assertEquals(average.height, change.height, 1f)
        screenshot("insights-trend-large-light")
        compose.onNodeWithTag("library-insights").performScrollToIndex(0)
        compose.onNodeWithTag("library-insights").performScrollToNode(hasTestTag("insights-period-90"))
        compose.onNodeWithTag("insights-period-90").performClick().assertIsSelected()
    }
    @Test fun searchNewYearSortStatusAndMaximumScoreCanBeCombinedAndReset() {
        var filter by mutableStateOf(SearchFilter())
        show { SearchFilterSheet(filter, listOf("TV"), { filter = filter.copy(tracking = it) }, {}, { filter = filter.copy(sort = it) }, { filter = SearchFilter() }, {},
            onMaximumScore = { filter = filter.copy(maximumScore = it) }, onWatchStatus = { filter = filter.copy(watchStatus = it, tracking = TrackingFilter.TRACKED) }) }
        compose.onNodeWithTag("search-filter-list").performScrollToNode(hasTestTag("search-status-ON_HOLD"))
        compose.onNodeWithTag("search-status-ON_HOLD").performClick().assertIsSelected()
        compose.onNodeWithTag("search-filter-list").performScrollToNode(hasTestTag("search-sort-NEWEST"))
        compose.onNodeWithTag("search-sort-NEWEST").performClick().assertIsSelected()
        compose.onNodeWithTag("search-filter-list").performScrollToNode(hasTestTag("search-max-score-7"))
        compose.onNodeWithTag("search-max-score-7").performClick().assertIsSelected()
        assertEquals(7, filter.maximumScore); assertEquals(WatchStatus.ON_HOLD, filter.watchStatus)
        screenshot("search-score-dark")
        compose.onNodeWithText(instrumentation.targetContext.getString(R.string.seasonal_filter_reset)).performClick()
        assertEquals(SearchFilter(), filter)
    }

    @Test fun filteredSettingsReleaseSearchFocusBeforeOpeningAndAfterClosingChangelog() {
        val context = instrumentation.targetContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(context.cacheDir, "qa5120-settings-${System.nanoTime()}.preferences_pb")
        val backing = PreferenceDataStoreFactory.create(scope = scope) { file }
        val prefs = UserPreferencesDataStore(backing)
        val tools = WatchToolsStore(backing, prefs)
        val db = Room.inMemoryDatabaseBuilder(context, AnimeScheduleDatabase::class.java).build()
        val models = ViewModelStore()
        val mal = object : MalRepository {
            override fun getUserList() = flowOf(emptyList<MalListEntry>())
            override suspend fun updateListEntry(animeId: Int, update: MalListUpdate) = AppResult.Success(Unit)
            override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
            override suspend fun removeListEntry(animeId: Int) = AppResult.Success(Unit)
            override suspend fun refreshUserList(force: Boolean) = true
            override suspend fun flushPendingUpdates() = true
        }
        val auth = object : AuthRepository {
            override val isLoggedIn = flowOf(false)
            override val username = flowOf("")
            override val avatarUrl = flowOf("")
            override val loginState = MutableStateFlow<LoginState>(LoginState.Idle)
            override fun beginLogin(): String = error("This test must not start sign-in")
            override suspend fun completeLogin(code: String, state: String?) = false
            override fun loginDenied() = Unit
            override fun loginAbandoned() = Unit
            override suspend fun logout() = Unit
        }
        val vm = SettingsViewModel(SettingsRepositoryImpl(prefs), auth, mal,
            CacheMaintenance(context, db.airingEpisodeDao(), db.animeDetailDao(), db.notificationDao(), prefs),
            PersonalBackupStore(backing, prefs, tools), prefs).also { models.put("settings", it) }
        val authVm = AuthViewModel(auth, mal).also { models.put("auth", it) }
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("sr-Latn")) }).resources
        try {
            show(dark = false, large = true) { SettingsScreen(vm, authVm) }
            val search = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings-search")))
            search.performTextInput("novo")
            search.assertIsFocused()
            compose.onNodeWithText(resources.getString(R.string.settings_changelog)).performClick()
            search.assertIsNotFocused()
            compose.onAllNodes(isDialog()).assertCountEquals(1)
            compose.onNode(hasText(resources.getString(R.string.changelog_5128_title)) and hasAnyAncestor(isDialog())).assertIsDisplayed()
            screenshot("settings-search-changelog-large-light")
            compose.onNode(hasContentDescription(resources.getString(android.R.string.cancel)) and hasAnyAncestor(isDialog())).performClick()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            search.assertIsNotFocused().assertTextContains("novo")
        } finally {
            instrumentation.runOnMainSync { models.clear() }
            runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }
            db.close()
            file.delete()
        }
    }
}
