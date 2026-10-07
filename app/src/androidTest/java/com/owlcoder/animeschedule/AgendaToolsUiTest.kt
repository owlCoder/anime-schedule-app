package com.owlcoder.animeschedule

import android.graphics.Bitmap
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.result.*
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.*
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.schedule.*
import com.owlcoder.animeschedule.presentation.screens.detail.*
import com.owlcoder.animeschedule.presentation.screens.settings.*
import com.owlcoder.animeschedule.presentation.screens.notifications.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import java.time.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*

/** Exercise production Compose screens with local data; no user's MAL entries are changed. */
class AgendaToolsUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val models = ViewModelStore()
    private val zone = ZoneId.of("Europe/Belgrade")
    private val today = LocalDate.now(zone)
    private fun text(id: Int) = instrumentation.targetContext.getString(id)
    @After fun cleanup() { instrumentation.runOnMainSync { models.clear() } }
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { AnimeScheduleTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, options = ThemeOptions(palette = if (dark) ThemePalette.NEON else ThemePalette.SAKURA)) {
            val toast = remember { ToastController() }
            CompositionLocalProvider(LocalToast provides toast, LocalScheduleZone provides zone) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { ToastHost(toast, content) }
            }
        } }
    }
    private fun screenshot(name: String, composeIdle: Boolean = true) {
        if (composeIdle) compose.waitForIdle()
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-580-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
    private val entry = MalListEntry(101, "Alpha Adventure", status = WatchStatus.WATCHING, episodesWatched = 4, score = 8, totalEpisodes = 12)
    private fun episode(id: Int, date: LocalDate = today) = AiringEpisode(id, id, 100 + id, 5,
        date.atTime(23, 0).atZone(zone).toEpochSecond() + id * 60, "Anime $id", null, null, null, listOf("Action"), 80, 12, "RELEASING", "TV", if (id == 1) entry else null)
    private val days get() = scheduleAgenda(today, listOf(ScheduleDay(today, listOf(episode(1), episode(2))), ScheduleDay(today.plusDays(1), listOf(episode(3, today.plusDays(1))))))
    private val detail = AnimeDetail(animeId = 1, malId = 101, titleRomaji = "Alpha Adventure: A Very Long Journey Through Another World", titleEnglish = "A Long Adventure in Another World", titleNative = "アルファの大冒険", coverImageUrl = null, coverColor = null, bannerImageUrl = null, description = "An adventure through an unfamiliar world. ".repeat(12), genres = listOf("Action", "Fantasy"), averageScore = 85, meanScore = 84, episodes = 12, duration = 24, status = "RELEASING", format = "TV", season = "FALL", seasonYear = 2026, nextAiringEpisode = null, nextAiringAt = null, studios = emptyList(), characters = listOf(Character(1, "Alpha", "アルファ", null, "MAIN"), Character(2, "Beta Alpha", null, null, "SUPPORTING")), relations = emptyList(), trailerSite = null, trailerId = null, siteUrl = null, malListEntry = entry)
    private val settings = object : SettingsRepository {
        override val userPreferencesFlow = flowOf(UserPreferences(timezoneId = zone.id, malLoggedIn = true))
        override suspend fun setTimezoneId(timezoneId: String) = Unit
        override suspend fun setThemeOptions(options: ThemeOptions) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setNotificationOffset(minutes: Int) = Unit
        override suspend fun setAccentColor(color: AccentColor) = Unit
        override suspend fun setAppLanguage(language: AppLanguage) = Unit
        override suspend fun setCacheRetentionDays(days: Int) = Unit
    }
    private val mal = object : MalRepository {
        override fun getUserList() = flowOf(listOf(entry))
        override suspend fun updateListEntry(animeId: Int, update: MalListUpdate) = AppResult.Success(Unit)
        override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
        override suspend fun removeListEntry(animeId: Int) = AppResult.Success(Unit)
        override suspend fun refreshUserList(force: Boolean) = true
        override suspend fun flushPendingUpdates() = true
    }
    private val auth = object : AuthRepository {
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
    private fun scheduleVm(): ScheduleViewModel {
        val repo = object : ScheduleRepository {
            override fun getWeekSchedule(zoneId: ZoneId, today: LocalDate) = flowOf(days)
            override suspend fun refreshSchedule(zoneId: ZoneId) = AppResult.Success(Unit)
        }
        val notifications = object : NotificationRepository {
            override fun getAll() = flowOf(emptyList<AppNotification>())
            override fun getUnreadCount() = flowOf(0)
            override suspend fun markRead(id: Int) = Unit
            override suspend fun markAllRead() = Unit
            override suspend fun deleteRead() = 0
        }
        val work = object : WorkScheduler {
            override fun scheduleFlushPendingUpdates() = Unit
            override fun checkAiringNotifications() = Unit
        }
        return ScheduleViewModel(repo, settings, mal, notifications, work, flowOf(setOf(101))).also { models.put("schedule", it) }
    }

    @Test fun scheduleFiltersUseCheckboxesAndResetNewOptions() {
        var filter by mutableStateOf(ScheduleFilter())
        show(true) { ScheduleFilterSheet(filter, listOf("Action", "Slice of Life"), listOf("TV", "TV_SHORT", "MOVIE"), true, {}, {}, {}, { filter = filter.copy(formats = setOf(it)) }, { filter = ScheduleFilter() }, {}, { filter = filter.copy(hideWatched = it) }, { filter = filter.copy(favoritesOnly = it) }) }
        compose.onNodeWithTag("schedule-hide-watched").performClick().assertIsOn()
        compose.onNodeWithTag("schedule-favorites-only").performClick().assertIsOn()
        assertTrue(filter.hideWatched && filter.favoritesOnly)
        screenshot("schedule-filters")
        compose.onNodeWithTag("schedule-filter-list").performScrollToNode(hasTestTag("schedule-format-TV_SHORT"))
        compose.onNodeWithTag("schedule-format-TV_SHORT").performClick().assertIsOn()
        screenshot("schedule-formats")
        compose.onNodeWithText(text(R.string.filter_reset)).performClick()
        assertEquals(ScheduleFilter(), filter)
    }
    @Test fun weeklyOverviewShowsAllDaysAndUsesSelectedDayForSharing() {
        var selected by mutableStateOf(today)
        var exported = false
        show { ScheduleAgendaSheet(days, selected, { selected = it }, { exported = true }, {}, {}) }
        compose.onNodeWithTag("agenda-total").assertTextContains("3", substring = true)
        compose.onNodeWithTag("agenda-export").assertIsEnabled()
        screenshot("weekly-overview")
        compose.onNodeWithTag("agenda-days").performScrollToNode(hasTestTag("agenda-day-${today.plusDays(6)}"))
        compose.onNodeWithTag("agenda-day-${today.plusDays(6)}").performClick()
        compose.onNodeWithTag("agenda-share").assertIsNotEnabled()
        compose.onNodeWithTag("agenda-export").performClick()
        assertTrue(exported); assertEquals(today.plusDays(6), selected)
    }
    @Test fun calendarExportUsesAndroidDocumentPicker() {
        val vm = scheduleVm(); vm.setOpenOverlay(ScheduleOverlay.Agenda)
        show(true) { ScheduleScreen({}, viewModel = vm) }
        compose.waitUntil(5000) { vm.uiState.value.weekDays.isNotEmpty() }
        compose.onNodeWithTag("agenda-export").performClick()
        val automation = instrumentation.uiAutomation
        compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui") == true }
        fun descendants(root: AccessibilityNodeInfo): Sequence<AccessibilityNodeInfo> = sequence { yield(root); for (i in 0 until root.childCount) root.getChild(i)?.let { yieldAll(descendants(it)) } }
        val name = "qa580-${System.nanoTime() % 1_000_000}.ics"
        val input = descendants(automation.rootInActiveWindow).first { it.isEditable }
        assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, name) }))
        screenshot("calendar-picker", false)
        val save = automation.rootInActiveWindow.findAccessibilityNodeInfosByText("Save").first { it.text?.toString()?.equals("Save", true) == true }
        assertTrue(save.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.schedule_agenda_exported)).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(ScheduleOverlay.None, vm.openOverlay.value)
    }
    @Test fun dailyAgendaOpensNativeShareSheetAndReturnsWithoutSending() {
        val vm = scheduleVm(); vm.setOpenOverlay(ScheduleOverlay.Agenda)
        show { ScheduleScreen({}, viewModel = vm) }
        compose.waitUntil(5000) { vm.uiState.value.weekDays.isNotEmpty() }
        compose.onNodeWithTag("agenda-share").performClick()
        val automation = instrumentation.uiAutomation
        compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString()?.contains("intentresolver") == true }
        screenshot("agenda-share", false)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
        compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName }
        assertEquals(ScheduleOverlay.None, vm.openOverlay.value)
        compose.onNodeWithTag("schedule-agenda").assertIsDisplayed()
    }
    @Test fun titlesCopyExactTextAndCharacterSearchCombinesWithRoleInOneDialog() {
        var copied = ""; var picked = 0
        show { DetailToolsSheet(detail, {}, { picked = it }, { copied = it }) }
        compose.onNodeWithTag("detail-titles").performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        screenshot("alternative-titles")
        compose.onNodeWithTag("detail-title-list").performScrollToNode(hasTestTag("copy-title-${R.string.detail_title_native}"))
        compose.onNodeWithTag("copy-title-${R.string.detail_title_native}").performClick()
        assertEquals(detail.titleNative, copied)
        Espresso.pressBack(); compose.waitForIdle()
        screenshot("tools-after-back")
        compose.onNodeWithTag("detail-character-finder").assertIsDisplayed().performClick()
        compose.onNodeWithTag("character-role-SUPPORTING").performClick().assertIsSelected()
        val input = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("character-search")))
        input.performTextInput("alpha"); input.performImeAction()
        compose.onNodeWithTag("finder-character-1").assertDoesNotExist()
        screenshot("character-before-assert")
        compose.onNodeWithTag("finder-character-2").assertIsDisplayed()
        screenshot("character-finder")
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithTag("finder-character-2").performClick(); assertEquals(2, picked)
        input.performTextReplacement("missing"); input.performImeAction()
        compose.onNodeWithText(text(R.string.detail_character_empty)).assertIsDisplayed()
    }
    @Test fun animeMuteAndManagementUpdateTheSameLocalMetadata() {
        var tools by mutableStateOf(WatchTools())
        var settingsPage by mutableStateOf(false)
        show(true) { CompositionLocalProvider(LocalWatchTools provides WatchToolsActions(data = tools, setNotificationMuted = { id, name, muted -> tools = tools.copy(mutedNotifications = if (muted) tools.mutedNotifications + (id to name) else tools.mutedNotifications - id) })) {
            if (settingsPage) NotificationSettingsSheet(true, 0, {}, {}, {}) else DetailToolsSheet(detail, {}, {}, {})
        } }
        compose.onNodeWithTag("detail-mute").performClick().assertIsOn()
        assertTrue(1 in tools.mutedNotifications)
        screenshot("detail-tools-muted")
        compose.runOnIdle { settingsPage = true }
        compose.onNodeWithTag("unmute-1").performScrollTo().performClick()
        assertTrue(tools.mutedNotifications.isEmpty())
    }
    @Test fun quietHoursCanBeChangedAndDisabled() {
        var quiet by mutableStateOf(QuietHours())
        show(true) { NotificationSettingsSheet(true, 0, {}, {}, {}, quietHours = quiet, onQuietChange = { quiet = it }) }
        compose.onNodeWithTag("quiet-hours-enabled").performClick().assertIsOn()
        compose.onNodeWithTag("quiet-start-plus").performScrollTo().performClick()
        compose.onNodeWithTag("quiet-end-minus").performScrollTo().performClick()
        assertEquals(QuietHours(true, 23, 7), quiet)
        screenshot("quiet-hours")
        compose.onNodeWithTag("quiet-hours-enabled").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithTag("quiet-start-value").assertDoesNotExist()
    }
    @Test fun notificationDatesCombineWithTitleSearchAndReadTabs() {
        val rows = listOf(
            AppNotification(1, 1, "Alpha Today", 5, null, 0, false, today.atStartOfDay(zone).toEpochSecond()),
            AppNotification(2, 2, "Beta Last Week", 5, null, 0, false, today.minusDays(6).atStartOfDay(zone).toEpochSecond()),
            AppNotification(3, 3, "Gamma Old", 4, null, 0, true, today.minusDays(40).atStartOfDay(zone).toEpochSecond()))
        val repo = object : NotificationRepository {
            override fun getAll() = flowOf(rows)
            override fun getUnreadCount() = flowOf(2)
            override suspend fun markRead(id: Int) = Unit
            override suspend fun markAllRead() = Unit
            override suspend fun deleteRead() = 0
        }
        val vm = NotificationsViewModel(repo).also { models.put("notifications", it) }
        show { NotificationsOverlay({}, {}, vm) }
        compose.waitUntil(5000) { vm.notifications.value.size == 3 }
        compose.onNodeWithTag("notif-period-TODAY").performClick()
        compose.onNodeWithText("Alpha Today").assertIsDisplayed()
        compose.onNodeWithText("Beta Last Week").assertDoesNotExist()
        screenshot("notification-date-filter")
        compose.onNodeWithTag("notif-period-WEEK").performClick()
        val input = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("notification-search")))
        input.performTextInput("Beta"); input.performImeAction()
        compose.onNodeWithText("Beta Last Week").assertIsDisplayed()
        input.performTextReplacement(""); input.performImeAction()
        compose.onNodeWithText(text(R.string.notif_tab_read), substring = true).performClick()
        compose.onNodeWithText(text(R.string.notif_period_empty)).assertIsDisplayed()
        screenshot("notification-empty-range")
        compose.onNodeWithText(text(R.string.notif_period_reset)).performClick()
        compose.onNodeWithText("Gamma Old").assertIsDisplayed()
    }
    @Test fun detailHeaderAndToolsRemainReadableInLightTheme() {
        val repo = object : AnimeDetailRepository {
            override fun getAnimeDetail(animeId: Int) = flowOf(AppResult.Success(detail))
            override suspend fun getCharacterDetail(characterId: Int) = AppResult.Success(CharacterDetail(characterId, "Alpha", null, null, "Character description"))
        }
        val sources = object : WatchSourceRepository {
            override fun getAll() = flowOf(emptyList<WatchSource>())
            override suspend fun add(name: String, urlTemplate: String, faviconUrl: String?, openExternally: Boolean) = Unit
            override suspend fun update(source: WatchSource) = Unit
            override suspend fun delete(source: WatchSource) = Unit
        }
        val vm = DetailViewModel(SavedStateHandle(mapOf("animeId" to 1)), repo, sources, auth, mal).also { models.put("detail", it) }
        val authVm = AuthViewModel(auth, mal).also { models.put("auth", it) }
        show { AnimeDetailScreen({}, {}, {}, viewModel = vm, authViewModel = authVm) }
        compose.waitUntil(5000) { vm.uiState.value.detail != null }
        compose.onNodeWithText(detail.titleRomaji!!).assertIsDisplayed()
        screenshot("detail-light-header")
        compose.onNodeWithTag("detail-tools").performClick()
        compose.onNodeWithTag("detail-titles").performClick()
        compose.onNodeWithTag("copy-title-${R.string.detail_title_romaji}").performClick()
        val clipboard = instrumentation.targetContext.getSystemService(android.content.ClipboardManager::class.java)
        compose.waitUntil(5000) { clipboard.primaryClip?.getItemAt(0)?.text?.toString() == detail.titleRomaji }
        Espresso.pressBack(); compose.waitForIdle()
        screenshot("tools-after-back")
        compose.onNodeWithTag("detail-character-finder").assertIsDisplayed().performClick()
        // Click the sheet's accessibility scrim action once; native clipboard previews
        // can intercept coordinate taps or race the next composition.
        compose.onNodeWithContentDescription(instrumentation.targetContext.getString(androidx.compose.ui.R.string.close_sheet)).performClick()
        compose.onNodeWithTag("detail-character-finder").assertIsDisplayed()
        Espresso.pressBack(); compose.waitForIdle()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.onNodeWithTag("detail-tools").performClick()
        compose.onNodeWithTag("detail-character-finder").performClick()
        compose.onNodeWithTag("finder-character-1").performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithText("Character description").assertIsDisplayed()
        screenshot("character-detail")
    }
}
