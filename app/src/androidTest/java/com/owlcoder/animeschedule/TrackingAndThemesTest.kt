package com.owlcoder.animeschedule

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.datastore.WatchToolsStore
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.*
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.mylist.*
import com.owlcoder.animeschedule.presentation.screens.settings.AppearanceSheet
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TrackingAndThemesTest {
    @get:Rule
    val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val models = ViewModelStore()
    private val file =
        File(instrumentation.targetContext.cacheDir, "qa-${System.nanoTime()}.preferences_pb")
    private val backing = PreferenceDataStoreFactory.create(scope = scope) { file }
    private val preferences = UserPreferencesDataStore(backing)
    private val tools = WatchToolsStore(backing, preferences)
    private fun text(id: Int) = instrumentation.targetContext.getString(id)
    @After
    fun cleanup() {
        instrumentation.runOnMainSync { models.clear() }; scope.cancel(); file.delete()
    }

    private class Mal : MalRepository {
        val entries = MutableStateFlow(
            listOf(
                MalListEntry(
                    101,
                    "Alpha Adventure",
                    status = WatchStatus.WATCHING,
                    episodesWatched = 4,
                    score = 8,
                    totalEpisodes = 12
                ),
                MalListEntry(
                    102,
                    "Beta Journey",
                    status = WatchStatus.PLAN_TO_WATCH,
                    episodesWatched = 0,
                    score = 0,
                    totalEpisodes = 24
                ),
            )
        )

        override fun getUserList() = entries
        override suspend fun updateListEntry(animeId: Int, update: MalListUpdate): AppResult<Unit> {
            entries.value = entries.value.map {
                if (it.animeId == animeId) it.copy(
                    status = update.status ?: it.status,
                    episodesWatched = update.episodesWatched ?: it.episodesWatched,
                    score = update.score ?: it.score
                ) else it
            }
            return AppResult.Success(Unit)
        }

        override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
        override suspend fun removeListEntry(animeId: Int) = AppResult.Success(Unit)
        override suspend fun refreshUserList(force: Boolean) = true
        override suspend fun flushPendingUpdates() = true
    }

    private object Auth : AuthRepository {
        override val isLoggedIn = flowOf(true)
        override val username = flowOf("QA")
        override val avatarUrl = flowOf("")
        override val loginState = MutableStateFlow<LoginState>(LoginState.Idle)
        override fun beginLogin() = ""
        override suspend fun completeLogin(code: String, state: String?) = false
        override fun loginDenied() = Unit
        override fun loginAbandoned() = Unit
        override suspend fun logout() = Unit
    }

    private fun showList(): MyListViewModel {
        val mal = Mal()
        val vm = MyListViewModel(mal, Auth, tools).also { models.put("list", it) }
        val auth = AuthViewModel(Auth, mal).also { models.put("auth", it) }
        compose.setContent {
            val data by tools.data.collectAsState(initial = WatchTools())
            val uiScope = rememberCoroutineScope()
            val toast = remember { ToastController() }
            CompositionLocalProvider(
                LocalToast provides toast, LocalWatchTools provides WatchToolsActions(
                    data = data,
                    toggleFavorite = { id -> uiScope.launch { tools.toggleFavorite(id) } },
                    setNote = { id, note -> uiScope.launch { tools.setNote(id, note) } },
                    setTags = { id, tags -> uiScope.launch { tools.setTags(id, tags) } },
                    togglePin = { id -> uiScope.launch { tools.togglePin(id) } },
                    setDurationOverride = { id, duration -> uiScope.launch { tools.setDurationOverride(id, duration) } })
            ) {
                AnimeScheduleTheme(
                    themeMode = ThemeMode.LIGHT,
                    options = ThemeOptions(palette = ThemePalette.SAKURA)
                ) {
                    ToastHost(toast) { MyListScreen({}, vm, auth) }
                }
            }
        }
        compose.waitUntil(10_000) { vm.uiState.value.entries.isNotEmpty() && !vm.uiState.value.isLoading }
        return vm
    }

    @Test
    fun favoritesNotesAllStatusesAndUnratedFiltersWorkTogether() {
        runBlocking { tools.toggleFavorite(101); tools.setNote(101, "Stopped at the opening") }
        val vm = showList()
        compose.onNodeWithTag("list-status-ALL").performClick()
        compose.waitUntil { vm.uiState.value.entries.size == 2 }
        compose.onNodeWithTag("list-favorites-filter").performScrollTo().performClick()
        compose.waitUntil { vm.uiState.value.entries.map { it.animeId } == listOf(101) }
        compose.onNodeWithTag("list-unrated-filter").performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.list_filtered_empty)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.list_clear_filters)).performClick()
        compose.waitUntil { vm.uiState.value.entries.size == 2 }
        // Scope the edit action to its card, since both cards contain an edit icon.
        compose.onNode(
            hasContentDescription(text(R.string.cd_edit_list_status)) and hasAnyAncestor(
                hasTestTag("mylist-entry-101")
            )
        ).performClick()
        compose.onNodeWithText(text(R.string.personal_note)).performScrollTo().performClick()
        compose.onNodeWithTag("editor-note").performScrollTo()
            .assertTextContains("Stopped at the opening")
        compose.onNodeWithTag("editor-note").performTextReplacement("Remember the ending")
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.tools.notes[101] == "Remember the ending" }
        vm.setSearchQuery("remember the ending")
        compose.waitUntil { vm.uiState.value.entries.map { it.animeId } == listOf(101) }
        screenshot("favorites-notes-sakura")
    }

    @Test
    fun weeklyGoalAndRandomPickAreUsable() {
        runBlocking { tools.recordProgress(101, "Alpha Adventure", 4, 7) }
        val vm = showList()
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.watch_history)).performScrollTo().performClick()
        compose.onNodeWithTag("goal-increase").performClick()
        compose.waitUntil { vm.uiState.value.tools.weeklyGoal == 13 }
        compose.onNodeWithText(
            instrumentation.targetContext.resources.getQuantityString(
                R.plurals.weekly_goal_progress_count,
                3,
                3,
                13
            )
        ).assertIsDisplayed()
        screenshot("watch-history-sakura")
        compose.onNodeWithContentDescription(instrumentation.targetContext.getString(android.R.string.cancel))
            .performClick()
        compose.onNodeWithTag("list-status-ALL").performClick()
        compose.onNodeWithTag("list-unrated-filter").performScrollTo().performClick()
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.list_pick)).performScrollTo().performClick()
        compose.onNode(hasText("Beta Journey") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.pick_again)).performClick()
        compose.onNode(hasText("Beta Journey") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        screenshot("random-pick-sakura")
    }

    @Test
    fun appearancePalettesModesAndAccessibilityOptionsStayUsable() {
        var options by mutableStateOf(ThemeOptions())
        var mode by mutableStateOf(ThemeMode.LIGHT)
        compose.setContent {
            AnimeScheduleTheme(themeMode = mode, options = options) {
                AppearanceSheet(
                    mode,
                    AccentColor.TELEGRAM_BLUE,
                    options,
                    { mode = it },
                    { options = it },
                    { options = ThemeOptions() },
                    {})
            }
        }
        compose.onNodeWithTag("theme-palette-SAKURA").performClick()
        compose.runOnIdle { assertEquals(ThemePalette.SAKURA, options.palette) }
        assertSheetSystemBars(darkIcons = true)
        screenshot("appearance-sakura-light")
        compose.onNodeWithText(text(R.string.settings_theme_dark)).performClick()
        assertSheetSystemBars(darkIcons = false)
        compose.onNodeWithText(text(R.string.settings_theme_light)).performClick()
        assertSheetSystemBars(darkIcons = true)
        compose.onNodeWithText(text(R.string.settings_theme_dark)).performClick()
        compose.onNodeWithText(text(R.string.theme_contrast)).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(options.highContrast); assertEquals(ThemeMode.DARK, mode) }
        compose.onNodeWithText(text(R.string.theme_motion)).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(options.reduceMotion) }
        compose.onNodeWithText(text(R.string.theme_dynamic)).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(options.dynamicColors) }
        screenshot("appearance-dynamic-dark")
    }

    @Test fun tagsCanBeEditedSavedAndCombinedWithListFilters() {
        val vm = showList()
        compose.onNode(hasContentDescription(text(R.string.cd_edit_list_status)) and hasAnyAncestor(hasTestTag("mylist-entry-101"))).performClick()
        compose.onNode(hasText(text(R.string.personal_tags)) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        compose.onNodeWithTag("editor-tags").performScrollTo().performTextReplacement("Akcija, Drama, akcija")
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.waitUntil { vm.uiState.value.tools.tags[101] == setOf("Akcija","Drama") }
        compose.onNodeWithTag("list-status-ALL").performClick()
        compose.onNodeWithTag("list-tags-filter").performScrollTo().performClick()
        compose.onNode(hasText("Drama") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil { vm.uiState.value.entries.map { it.animeId } == listOf(101) }
        screenshot("tag-filter-and-time-estimate")
        compose.onNodeWithTag("list-unrated-filter").performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.list_filtered_empty)).assertIsDisplayed()
        compose.onNode(hasText(text(R.string.list_clear_filters)) and hasClickAction()).assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.entries.size == 2 && vm.uiState.value.activeTag == null }
        vm.setSearchQuery("dRaMa")
        compose.waitUntil { vm.uiState.value.entries.map { it.animeId } == listOf(101) }
        vm.setSearchQuery("")
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.continue_watching)).assertIsEnabled()
    }

    @Test fun historySearchAndWeekFilterApplyTogether() {
        val vm = showList()
        runBlocking { tools.recordProgress(101,"Alpha Adventure",4,5); tools.recordProgress(102,"Beta Journey",0,1) }
        compose.waitUntil { vm.uiState.value.tools.activity.size == 2 }
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.watch_history)).performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("history-search"))).performTextInput("alpha")
        compose.onNodeWithTag("history-this-week").performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("history-search"))).assertIsNotFocused()
        compose.onNode(hasText("Alpha Adventure") and hasAnyAncestor(isDialog())).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Beta Journey") and hasAnyAncestor(isDialog())).assertDoesNotExist()
        screenshot("history-search-this-week")
    }

    @Test fun predefinedAccentsSchedulingCompactModeAndSavedLooksWorkTogether() {
        var options by mutableStateOf(ThemeOptions())
        var mode by mutableStateOf(ThemeMode.LIGHT)
        var accent by mutableStateOf(AccentColor.TELEGRAM_BLUE)
        var presets by mutableStateOf(emptyList<AppearancePreset>())
        compose.setContent {
            AnimeScheduleTheme(themeMode=mode,options=options) {
                AppearanceSheet(mode,accent,options,{ mode=it },{ options=it },{ options=ThemeOptions() },{},presets,{ presets=listOf(it) },{ mode=it.mode;options=it.options;accent=it.accent },{ name -> presets=presets.filterNot{it.name==name} },{ accent=it;options=options.copy(palette=ThemePalette.CLASSIC,dynamicColors=false) })
            }
        }
        compose.onNodeWithTag("theme-accent-GREEN").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(AccentColor.GREEN,accent) }
        compose.onNodeWithText(text(R.string.compact_layout)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.scheduled_theme)).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(options.compactLayout);assertTrue(options.scheduled) }
        screenshot("scheduled-theme-display")
        compose.onNodeWithTag("preset-name").performScrollTo().performTextInput("Evening")
        compose.onNodeWithTag("preset-save").performScrollTo().performClick()
        compose.onNodeWithTag("preset-name").assertIsNotFocused()
        compose.runOnIdle { assertEquals(1,presets.size);options=ThemeOptions() }
        compose.onNodeWithTag("preset-Evening").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(options.compactLayout);assertTrue(options.scheduled);assertEquals(AccentColor.GREEN,accent) }
        screenshot("appearance-saved-look")
        compose.onNodeWithContentDescription(instrumentation.targetContext.getString(R.string.preset_delete,"Evening")).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(presets.isEmpty()) }
    }

    private fun assertSheetSystemBars(darkIcons: Boolean) = compose.runOnIdle {
        fun findDialog(view: View): DialogWindowProvider? {
            if (view is DialogWindowProvider) return view
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    findDialog(view.getChildAt(index))?.let { return it }
                }
            }
            return null
        }
        val window = WindowInspector.getGlobalWindowViews().mapNotNull(::findDialog).single().window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        assertEquals(darkIcons, controller.isAppearanceLightStatusBars)
        assertEquals(darkIcons, controller.isAppearanceLightNavigationBars)
    }

    @Test
    fun exportOpensAndroidDocumentPickerAndSavesTheCsv() {
        showList()
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.list_export)).performScrollTo().performClick()
        val automation = instrumentation.uiAutomation
        compose.waitUntil(10_000) {
            automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui") == true
        }
        screenshot("export-picker", waitForCompose = false)
        val buttons = automation.rootInActiveWindow.findAccessibilityNodeInfosByText("Save")
        val save =
            buttons.firstOrNull { it.text?.toString()?.equals("Save", ignoreCase = true) == true }
                ?: error("Save button missing")
        assertTrue(save.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName }
        compose.onNodeWithText(text(R.string.list_exported)).assertIsDisplayed()
    }

    @Test
    fun pinsSmartFiltersAndSavedViewsPersistAndRestoreSelection() {
        val vm = showList()
        compose.onNodeWithTag("list-status-ALL").performClick()
        compose.onNode(hasContentDescription(text(R.string.cd_edit_list_status)) and hasAnyAncestor(hasTestTag("mylist-entry-102"))).performClick()
        compose.onNodeWithTag("editor-pin").performClick()
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.entries.first().animeId == 102 }
        compose.onNodeWithTag("list-smart-filter").performClick()
        compose.onNodeWithTag("smart-PINNED").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.entries.map { it.animeId } == listOf(102) }
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNode(hasText(text(R.string.saved_list_views)) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        compose.onNodeWithTag("view-name").performTextInput("Pinned weekend")
        compose.onNodeWithTag("view-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.tools.savedViews.size == 1 }
        screenshot("saved-list-views")
        compose.onNodeWithContentDescription(instrumentation.targetContext.getString(android.R.string.cancel)).performClick()
        compose.runOnIdle { vm.clearQuickFilters(); vm.setFilter(WatchStatus.WATCHING) }
        compose.waitUntil(5_000) { vm.uiState.value.entries.map { it.animeId } == listOf(101) }
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNode(hasText(text(R.string.saved_list_views)) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        compose.onNodeWithTag("saved-view-Pinned weekend").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.entries.map { it.animeId } == listOf(102) && vm.uiState.value.activeFilter == null }
        compose.onNodeWithTag("list-smart-filter").performClick()
        compose.onNodeWithTag("smart-NEAR_FINISH").performClick()
        compose.onNodeWithText(text(R.string.list_filtered_empty)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.list_clear_filters)).performClick()
        compose.waitUntil(5_000) { vm.uiState.value.entries.size == 2 }
        screenshot("pinned-list")
    }

    @Test
    fun durationOverrideValidatesAndFeedsPlannerWithoutChangingProgress() {
        val vm = showList()
        fun editor() = compose.onNode(hasContentDescription(text(R.string.cd_edit_list_status)) and hasAnyAncestor(hasTestTag("mylist-entry-101"))).performClick()
        editor()
        compose.onNodeWithTag("editor-duration-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("editor-duration").performScrollTo().performTextInput("999")
        compose.onNodeWithTag("list-editor-save").assertIsNotEnabled()
        compose.onNodeWithTag("editor-duration").performTextReplacement("12")
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.tools.durationOverrides[101] == 12 }
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.watch_planner)).performScrollTo().performClick()
        compose.onNodeWithTag("planner-budget-30").performClick()
        compose.onAllNodesWithText(instrumentation.targetContext.resources.getQuantityString(R.plurals.planner_episode_count, 2, 2, 24)).onFirst().assertIsDisplayed()
        compose.onNodeWithTag("plan-101").assertIsDisplayed()
        val presetBounds = listOf(30, 60, 120).map { compose.onNodeWithTag("planner-budget-$it").fetchSemanticsNode().boundsInRoot }
        assertTrue("Planner presets must align when labels wrap", presetBounds.maxOf { it.height } - presetBounds.minOf { it.height } <= 1f)
        assertEquals(4, vm.uiState.value.entries.single().episodesWatched)
        screenshot("watch-planner")
        compose.onNodeWithContentDescription(instrumentation.targetContext.getString(android.R.string.cancel)).performClick()
        editor()
        compose.onNodeWithTag("editor-duration-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("editor-duration-default").performScrollTo().performClick()
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.tools.durationOverrides.isEmpty() }
    }

    @Test
    fun calendarShowsDailyActivityAndSavesDailyGoal() {
        runBlocking { tools.recordProgress(101, "Alpha Adventure", 4, 7) }
        val vm = showList()
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.activity_calendar)).performScrollTo().performClick()
        compose.onNodeWithTag("daily-goal-increase").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.tools.dailyGoal == 4 }
        compose.onNodeWithText(instrumentation.targetContext.getString(R.string.daily_goal_progress, 3, 4)).assertIsDisplayed()
        compose.onNodeWithTag("calendar-${LocalDate.now()}").performScrollTo().performClick()
        screenshot("activity-calendar")
        compose.onNodeWithTag("calendar-${LocalDate.now().minusDays(1)}").performScrollTo().performClick()
        compose.onNodeWithTag("activity-calendar-list").performScrollToNode(hasText(text(R.string.calendar_day_empty)))
        compose.onNodeWithText(text(R.string.calendar_day_empty)).assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
    }

    @Test
    fun sharingOpensAndroidChooserWithOnlyCurrentResults() {
        val vm = showList()
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.list_share)).performScrollTo().performClick()
        val automation = instrumentation.uiAutomation
        compose.waitUntil(10_000) { automation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Alpha Adventure")?.isNotEmpty() == true }
        val content = automation.rootInActiveWindow.findAccessibilityNodeInfosByText("Alpha Adventure").joinToString { it.text?.toString().orEmpty() }
        assertFalse(content.contains("Beta Journey"))
        assertEquals(4, vm.uiState.value.entries.single().episodesWatched)
        screenshot("list-share-chooser", waitForCompose = false)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
    }

    @Test
    fun ratingRangeSavedViewsAndTimeSortingAreConnectedToList() {
        val vm = showList()
        vm.setFilter(null)
        vm.setScoreRange(7,10)
        compose.waitUntil { vm.uiState.value.entries.map { it.animeId } == listOf(101) }
        vm.saveView("Rated picks")
        compose.waitUntil { vm.uiState.value.tools.savedViews.size == 1 }
        val saved = vm.uiState.value.tools.savedViews.single()
        assertEquals(7, saved.minimumScore)
        vm.clearQuickFilters()
        compose.waitUntil { vm.uiState.value.entries.size == 2 }
        vm.applyView(saved)
        compose.waitUntil { vm.uiState.value.scoreRange == (7 to 10) && vm.uiState.value.entries.size == 1 }
        compose.onNodeWithTag("list-clear-active").performClick()
        compose.waitUntil { vm.uiState.value.entries.size == 2 }
        runBlocking { tools.setDurationOverride(102,1) }
        compose.onNodeWithContentDescription(text(R.string.mylist_sort)).performClick()
        compose.onNodeWithText(text(R.string.sort_watch_time)).performClick()
        compose.waitUntil { vm.uiState.value.entries.map { it.animeId } == listOf(102,101) }
        screenshot("time-sort")
    }

    @Test
    fun groupedToolsBulkChangesAndTagManagerUsePersistentStore() {
        runBlocking { tools.setTags(101,"Action") }
        val vm = showList()
        vm.setFilter(null)
        compose.waitUntil { vm.uiState.value.entries.size == 2 }
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        screenshot("grouped-tools")
        compose.onNodeWithText(text(R.string.bulk_tools)).performScrollTo().performClick()
        compose.onNodeWithTag("bulk-all").performClick()
        compose.onNodeWithTag("bulk-action-0").performClick()
        compose.waitUntil { vm.uiState.value.tools.favorites == setOf(101,102) }
        compose.onNodeWithContentDescription(text(R.string.list_tools)).performClick()
        compose.onNodeWithText(text(R.string.manage_tags)).performScrollTo().performClick()
        compose.onNodeWithTag("tag-edit-Action").performClick()
        compose.onNodeWithTag("tag-replacement").performTextReplacement("Adventure")
        compose.onNodeWithTag("tag-rename-save").performClick()
        compose.waitUntil { vm.uiState.value.tools.tags[101] == setOf("Adventure") }
        screenshot("tags-persisted")
    }

    private fun screenshot(name: String, waitForCompose: Boolean = true) {
        if (waitForCompose) compose.waitForIdle()
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(
            instrumentation.targetContext.getExternalFilesDir(null),
            "qa-550-$name.png"
        ).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
