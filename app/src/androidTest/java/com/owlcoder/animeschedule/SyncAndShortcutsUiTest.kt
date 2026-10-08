package com.owlcoder.animeschedule

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.settings.SyncCenterSheet
import com.owlcoder.animeschedule.presentation.screens.search.SearchLoadingState
import com.owlcoder.animeschedule.presentation.screens.detail.DetailLoadingState
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*

class SyncAndShortcutsUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun text(id: Int) = instrumentation.targetContext.getString(id)
    @Composable private fun SerbianLarge(content: @Composable () -> Unit) {
        val context = LocalContext.current; val density = LocalDensity.current
        val config = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag("sr-Latn")) }
        val localized = context.createConfigurationContext(config)
        CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config, LocalResources provides localized.resources,
            LocalDensity provides Density(density.density, 1.35f), content = content)
    }
    @Test fun syncCenterReportsOfflineQueueRetryBusyFailureAndExpiredSession() {
        var state by mutableStateOf(MalSyncState(loggedIn = true, pendingCount = 3, lastSuccessEpochMs = 1_800_000_000_000, online = false))
        var retry = 0; var login = 0
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(amoled = true)) {
            SyncCenterSheet(state, { retry++ }, { login++ }, {})
        } }
        compose.onNodeWithTag("sync-status").assertTextEquals(instrumentation.targetContext.resources.getQuantityString(R.plurals.sync_pending_offline, 3, 3))
        compose.onNodeWithTag("sync-retry").assertIsNotEnabled()
        screenshot("sync-offline-dark")
        compose.runOnIdle { state = state.copy(online = true) }
        compose.onNodeWithTag("sync-retry").performClick()
        compose.runOnIdle { assertEquals(1, retry); state = state.copy(syncing = true) }
        compose.onNodeWithTag("sync-retry").assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(syncing = false, rejectedCount = 1) }
        compose.onNodeWithTag("sync-status").assertTextEquals(text(R.string.sync_needs_attention))
        compose.runOnIdle { state = state.copy(loggedIn = false) }
        compose.onNodeWithText(text(R.string.sync_session_expired)).assertIsDisplayed()
        compose.onNodeWithTag("sync-login").performClick()
        compose.runOnIdle { assertEquals(1, login); state = state.copy(loggedIn = true, pendingCount = 0, rejectedCount = 0) }
        compose.onNodeWithTag("sync-status").assertTextEquals(text(R.string.sync_all_saved))
    }
    @Test fun syncCenterRemainsReadableWithLargeSerbianTextInLightTheme() {
        compose.setContent { SerbianLarge { AnimeScheduleTheme(themeMode = ThemeMode.LIGHT, options = ThemeOptions(palette = ThemePalette.SAKURA)) {
            SyncCenterSheet(MalSyncState(loggedIn = true, pendingCount = 3, online = false), {}, {}, {})
        } } }
        compose.onNodeWithTag("sync-retry").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        screenshot("sync-serbian-large-light")
    }
    @Test fun shortcutPickerSupportsFourChoicesReorderRemovalAndReset() {
        var selected by mutableStateOf(DefaultToolShortcuts)
        compose.setContent { SerbianLarge { AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(amoled = true)) {
            ToolShortcutsSheet(selected, { selected = it }, {})
        } } }
        compose.onNodeWithTag("shortcut-choice-SYNC").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(4, selected.size) }
        compose.onNodeWithTag("shortcut-choice-CALENDAR").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("shortcut-up-SYNC").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(ToolShortcut.SYNC, selected[2]) }
        compose.onNodeWithTag("shortcut-choice-PLANNER").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(ToolShortcut.PLANNER in selected) }
        compose.onNodeWithTag("shortcut-choice-CALENDAR").performScrollTo().performClick()
        screenshot("shortcuts-serbian-large-dark")
        val reset = instrumentation.targetContext.createConfigurationContext(Configuration(instrumentation.targetContext.resources.configuration).apply { setLocale(Locale.forLanguageTag("sr-Latn")) }).getString(R.string.common_reset)
        compose.onNodeWithText(reset).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(DefaultToolShortcuts, selected) }
    }
    @Test fun customizeStaysVisibleBesideFourLongShortcutLabels() {
        var customized = 0
        compose.setContent { SerbianLarge { AnimeScheduleTheme(themeMode = ThemeMode.DARK) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(16.dp)) {
                ToolShortcutBar(listOf(ToolShortcut.CALENDAR, ToolShortcut.SYNC, ToolShortcut.WEEK_OVERVIEW, ToolShortcut.PLANNER), {}, { customized++ })
            }
        } } }
        listOf(ToolShortcut.CALENDAR, ToolShortcut.SYNC, ToolShortcut.WEEK_OVERVIEW, ToolShortcut.PLANNER).forEach { value ->
            compose.onNodeWithTag("shortcut-${value.name}").assertIsDisplayed()
        }
        compose.onNodeWithTag("shortcut-customize").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, customized) }
        screenshot("shortcut-grid-serbian-large")
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun undoIsTappableInsideTheEditingOverlayAndConsumedOnce() {
        val repo = UndoFixture(); val toast = ToastController(); var sheet by mutableStateOf(true)
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(amoled = true)) {
            CompositionLocalProvider(LocalToast provides toast) {
                ListUndoMessages(repo, toast)
                ToastHost(toast) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    if (sheet) AppSheet(onDismissRequest = { sheet = false }, title = "Edit anime") { Text("Fixture Adventure · 5 / 12") }
                }
            }
        } }
        compose.runOnIdle { repo.change.value = UndoListChange(1, 101, "Fixture Adventure", MalListUpdate(WatchStatus.WATCHING, 4, 8), MalListUpdate(WatchStatus.WATCHING, 5, 8)) }
        compose.onNode(hasTestTag("toast-action") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNode(hasTestTag("app-toast") and hasAnyAncestor(isDialog())).assertHeightIsAtLeast(100.dp)
        screenshot("undo-inside-overlay")
        compose.onNode(hasTestTag("toast-action") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil { repo.restored == 1 }
        compose.runOnIdle { assertEquals(1, repo.restored); assertNull(toast.current?.action) }
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        androidx.test.espresso.Espresso.pressBack()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun feedbackUsesOnlyTheActiveOverlayAndReturnsToThePage() {
        val toast = ToastController(); var first by mutableStateOf(true); var second by mutableStateOf(false)
        compose.setContent { SerbianLarge { AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(amoled = true)) {
                ToastHost(toast) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    if (first) AppSheet(onDismissRequest = { first = false }, title = "Prva alatka") {
                        AppButton("Otvori drugu alatku", { second = true }, Modifier.testTag("open-second"))
                        Spacer(Modifier.height(240.dp))
                    }
                    if (second) AppSheet(onDismissRequest = { second = false }, title = "Druga alatka") {
                        Text("Provera poruka u otvorenom overlayu", Modifier.testTag("overlay-description"))
                    }
                }
        } } }
        compose.runOnIdle { toast.error("Izmena nije sačuvana. Proveri internet vezu i pokušaj ponovo.") }
        compose.onAllNodesWithTag("app-toast").assertCountEquals(1)
        compose.onNode(hasTestTag("app-toast") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        screenshot("error-inside-overlay-serbian")
        compose.onNodeWithTag("toast-dismiss").performClick()
        compose.onNodeWithTag("open-second").performClick()
        compose.runOnIdle { toast.success("Podešavanje je sačuvano") }
        compose.onAllNodesWithTag("app-toast").assertCountEquals(1)
        compose.onNode(hasTestTag("app-toast") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithTag("overlay-description").assertIsDisplayed()
        val description = compose.onNodeWithTag("overlay-description").fetchSemanticsNode().boundsInRoot
        val feedback = compose.onNodeWithTag("app-toast").fetchSemanticsNode().boundsInRoot
        assertTrue("Feedback must leave the description visible", description.bottom <= feedback.top)
        compose.runOnIdle { second = false }
        compose.onAllNodesWithTag("app-toast").assertCountEquals(1)
        compose.runOnIdle { first = false }
        compose.onNode(hasTestTag("app-toast") and !hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithTag("toast-dismiss").performClick()
        compose.runOnIdle { assertNull(toast.current) }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun movingUndoOutOfASheetKeepsItsOriginalTimeout() {
        val toast = ToastController(); var open by mutableStateOf(true); var dismissed = 0
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(reduceMotion = true)) {
            CompositionLocalProvider(LocalToast provides toast) {
                ToastHost(toast) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    if (open) AppSheet(onDismissRequest = { open = false }, title = "Edit") { Text("Progress saved") }
                }
            }
        } }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { toast.show("Progress saved", action = ToastAction(41, "Undo", {}, { dismissed++ })) }
        compose.mainClock.advanceTimeBy(8000)
        compose.runOnIdle { open = false }
        compose.mainClock.advanceTimeBy(2500)
        compose.runOnIdle { assertNull(toast.current); assertEquals(1, dismissed) }
        compose.mainClock.autoAdvance = true
    }
    @Test fun loadingSkeletonsRespectReducedMotionAndBothThemes() {
        var dark by mutableStateOf(true); var detail by mutableStateOf(false)
        compose.setContent { AnimeScheduleTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, options = ThemeOptions(reduceMotion = true, amoled = true)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(16.dp)) { if (detail) DetailLoadingState() else SearchLoadingState() }
        } }
        compose.onNodeWithTag("search-loading").assertIsDisplayed()
        screenshot("search-skeleton-dark")
        compose.runOnIdle { dark = false }
        compose.onNodeWithTag("search-loading").assertIsDisplayed()
        screenshot("search-skeleton-light")
        compose.runOnIdle { detail = true }
        compose.onNodeWithTag("detail-loading").assertIsDisplayed()
        screenshot("detail-skeleton-light")
        compose.runOnIdle { dark = true }
        screenshot("detail-skeleton-dark")
    }
    private class UndoFixture : MalRepository {
        val change = MutableStateFlow<UndoListChange?>(null); var restored = 0
        override val undoChange = change
        override fun getUserList() = flowOf(emptyList<MalListEntry>())
        override suspend fun undoListChange(id: Long): AppResult<Unit> { if (change.value?.id != id) return AppResult.Error(com.owlcoder.animeschedule.core.result.AppError.NoCache); restored++; change.value = null; return AppResult.Success(Unit) }
        override fun dismissUndo(id: Long) { if (change.value?.id == id) change.value = null }
        override suspend fun updateListEntry(animeId: Int, update: MalListUpdate) = AppResult.Success(Unit)
        override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
        override suspend fun removeListEntry(animeId: Int) = AppResult.Success(Unit)
        override suspend fun refreshUserList(force: Boolean) = true
        override suspend fun flushPendingUpdates() = true
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5111-$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
    }
}
