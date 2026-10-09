package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.*
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.screens.mylist.MyListScreen
import com.owlcoder.animeschedule.presentation.screens.mylist.MyListViewModel
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleOverlay
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleScreen
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleSection
import com.owlcoder.animeschedule.presentation.screens.schedule.ScheduleViewModel
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real Compose dialogs on the emulator, with local repositories so no MAL account is mutated. */
class OverlayEditingTest {
    @get:Rule val compose = createComposeRule()
    private val viewModels = ViewModelStore()
    private fun <T : ViewModel> keep(name: String, vm: T): T = vm.also { viewModels.put(name, it) }
    @After fun cleanup() { InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModels.clear() } }

    private fun edit(id: Int) = compose.onNode(hasTestTag("schedule-edit-$id") and hasAnyAncestor(hasTestTag("schedule-day-list")))

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private class FakeMal : MalRepository {
        val entries = MutableStateFlow(listOf(
            MalListEntry(101, "Alpha Adventure", status = WatchStatus.WATCHING, episodesWatched = 4, score = 8, totalEpisodes = 12),
            MalListEntry(102, "Beta Journey", status = WatchStatus.WATCHING, episodesWatched = 2, score = 7, totalEpisodes = 24),
        ))
        val updates = mutableListOf<Pair<Int, MalListUpdate>>()
        override fun getUserList(): Flow<List<MalListEntry>> = entries
        override suspend fun updateListEntry(animeId: Int, update: MalListUpdate): AppResult<Unit> {
            updates += animeId to update
            entries.value = entries.value.map { entry ->
                if (entry.animeId != animeId) entry else entry.copy(
                    status = update.status ?: entry.status,
                    episodesWatched = update.episodesWatched ?: entry.episodesWatched,
                    score = update.score ?: entry.score,
                )
            }
            return AppResult.Success(Unit)
        }
        override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
        override suspend fun removeListEntry(animeId: Int): AppResult<Unit> {
            entries.value = entries.value.filter { it.animeId != animeId }
            return AppResult.Success(Unit)
        }
        override suspend fun refreshUserList(force: Boolean) = true
        override suspend fun flushPendingUpdates() = true
    }

    private object Settings : SettingsRepository {
        override val userPreferencesFlow = flowOf(UserPreferences(timezoneId = "Europe/Belgrade", malLoggedIn = true))
        override suspend fun setTimezoneId(timezoneId: String) = Unit
        override suspend fun setThemeOptions(options: com.owlcoder.animeschedule.domain.model.ThemeOptions) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setNotificationOffset(minutes: Int) = Unit
        override suspend fun setAppLanguage(language: AppLanguage) = Unit
        override suspend fun setCacheRetentionDays(days: Int) = Unit
    }
    private object Notifications : NotificationRepository {
        override fun getAll() = flowOf(emptyList<AppNotification>())
        override fun getUnreadCount() = flowOf(0)
        override suspend fun markRead(id: Int) = Unit
        override suspend fun markAllRead() = Unit
        override suspend fun deleteRead() = 0
    }
    private object Work : WorkScheduler {
        override fun scheduleFlushPendingUpdates() = Unit
        override fun checkAiringNotifications() = Unit
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

    private fun showSchedule(mal: FakeMal) {
        val repo = object : ScheduleRepository {
            override fun getWeekSchedule(zoneId: ZoneId, today: LocalDate) = mal.entries.map { entries ->
                listOf(ScheduleDay(today, entries.mapIndexed { index, entry ->
                    AiringEpisode(
                        entry.animeId, entry.animeId, entry.animeId, 5,
                        today.atTime(23, 0).atZone(zoneId).toEpochSecond() + index * 60,
                        entry.title, null, null, null, listOf("Action"), 80, entry.totalEpisodes,
                        "RELEASING", "TV", entry,
                    )
                }))
            }
            override suspend fun refreshSchedule(zoneId: ZoneId, startDate: java.time.LocalDate) = AppResult.Success(Unit)
        }
        val vm = keep("schedule", ScheduleViewModel(repo, Settings, mal, Notifications, Work))
        vm.setOpenOverlay(ScheduleOverlay.SeeAll(ScheduleSection.TODAY))
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.DARK) { ScheduleScreen(onAnimeClick = {}, viewModel = vm) } }
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("schedule-edit-101") and hasAnyAncestor(hasTestTag("schedule-day-list"))).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun consecutiveEditsReuseOneDialogAndKeepEachAnimesProgress() {
        val mal = FakeMal()
        showSchedule(mal)
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        edit(101).performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithTag("list-editor-episodes").performTextReplacement("999")
        compose.onNodeWithTag("list-editor-episodes").assertTextContains("12")
        compose.onNodeWithTag("list-editor-episodes").performImeAction()
        screenshot("editor-dark")
        compose.onNodeWithTag("editor-status-picker").performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.watch_status_completed)).performClick()
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.onNodeWithTag("schedule-day-list").assertIsDisplayed()
        edit(102).performClick()
        compose.onNodeWithTag("list-editor-episodes").assertTextContains("2")
        compose.onNodeWithTag("list-editor-episodes").performTextReplacement("5")
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.onNodeWithTag("schedule-day-list").assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.runOnIdle {
            assertEquals(listOf(101, 102), mal.updates.map { it.first })
            assertEquals(listOf(12, 5), mal.updates.map { it.second.episodesWatched })
            assertEquals(WatchStatus.COMPLETED, mal.updates.first().second.status)
        }
        screenshot("schedule-overlay-dark")
    }

    @Test fun editorBackAndRemovalReturnToAUsableDayList() {
        val mal = FakeMal()
        showSchedule(mal)
        edit(101).performClick()
        compose.onNodeWithContentDescription(text(R.string.common_back)).performClick()
        compose.onNodeWithTag("schedule-day-list").assertIsDisplayed()
        edit(102).performClick()
        compose.onNodeWithTag("list-editor-save").assertIsDisplayed()
        Espresso.pressBack()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("list-editor-save").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("schedule-day-list").assertIsDisplayed()
        edit(101).performClick()
        compose.onNodeWithText(text(R.string.list_status_remove)).performScrollTo().performClick()
        compose.onNodeWithTag("editor-remove-confirm").performScrollTo().performClick()
        edit(102).assertIsDisplayed().performClick()
        compose.onNodeWithTag("list-editor-episodes").assertTextContains("2")
        compose.onAllNodes(isDialog()).assertCountEquals(1)
    }

    @Test fun listSortAndStatisticsAreUsableInLightTheme() {
        val mal = FakeMal()
        val vm = keep("list", MyListViewModel(mal, Auth))
        val auth = keep("auth", AuthViewModel(Auth, mal))
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.LIGHT) { MyListScreen({}, vm, auth) } }
        compose.onNodeWithContentDescription(text(R.string.mylist_sort)).performClick()
        compose.onNodeWithText(text(R.string.mylist_sort_remaining)).performClick()
        compose.runOnIdle { assertEquals(listOf(101, 102), vm.uiState.value.entries.map { it.animeId }) }
        screenshot("my-list-light")
        compose.onNodeWithContentDescription(text(R.string.mylist_statistics)).performClick()
        compose.onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.mylist_backlog, 30L)).assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        screenshot("statistics-light")
    }

    @Test fun finishAllUsesTheKnownTotalForAnAnimeNotOnTheList() {
        var saved: MalListUpdate? = null
        compose.setContent {
            AnimeScheduleTheme(themeMode = ThemeMode.LIGHT) {
                ListStatusBottomSheet(200, null, {}, { _, update -> saved = update }, animeTitle = "New series", totalEpisodes = 12)
            }
        }
        compose.onNodeWithText(text(R.string.editor_finish_all)).performScrollTo().performClick()
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.runOnIdle {
            assertEquals(12, saved?.episodesWatched)
            assertEquals(WatchStatus.COMPLETED, saved?.status)
        }
    }

    @Test fun shortcutsOpenTheRequestedListToolAndFavoritesFilter() {
        val mal = FakeMal()
        val vm = keep("list-shortcuts", MyListViewModel(mal, Auth))
        val auth = keep("auth-shortcuts", AuthViewModel(Auth, mal))
        var shortcut by androidx.compose.runtime.mutableStateOf<com.owlcoder.animeschedule.domain.model.ToolShortcut?>(com.owlcoder.animeschedule.domain.model.ToolShortcut.PLANNER)
        var consumed = 0
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.DARK) {
            MyListScreen({}, vm, auth, initialTool = shortcut, onToolOpened = { consumed++; shortcut = null })
        } }
        compose.onNodeWithText(text(R.string.watch_planner)).assertIsDisplayed()
        Espresso.pressBack()
        compose.runOnIdle { shortcut = com.owlcoder.animeschedule.domain.model.ToolShortcut.HISTORY }
        compose.onNodeWithText(text(R.string.watch_history)).assertIsDisplayed()
        Espresso.pressBack()
        compose.runOnIdle { shortcut = com.owlcoder.animeschedule.domain.model.ToolShortcut.CALENDAR }
        compose.onNodeWithText(text(R.string.activity_calendar)).assertIsDisplayed()
        Espresso.pressBack()
        compose.runOnIdle { shortcut = com.owlcoder.animeschedule.domain.model.ToolShortcut.FAVORITES }
        compose.waitUntil { vm.uiState.value.favoritesOnly }
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.runOnIdle { assertEquals(4, consumed) }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "qa-530-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
