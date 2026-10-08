package com.owlcoder.animeschedule

import android.graphics.Bitmap
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5100-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
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
    private fun scheduleVm(muted: Set<Int> = emptySet()): ScheduleViewModel {
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
        return ScheduleViewModel(repo, settings, mal, notifications, work, flowOf(setOf(101)), mutedIds = flowOf(muted)).also { models.put("schedule", it) }
    }

    @Test fun dateRailCountsFollowMutedFilterAndDatesRemainSelectable() {
        val vm = scheduleVm(setOf(1))
        show(true) { ScheduleScreen({}, viewModel = vm) }
        compose.waitUntil(5000) { vm.uiState.value.weekDays.isNotEmpty() }
        compose.onNodeWithTag("schedule-count-$today", useUnmergedTree = true).assertTextEquals("2")
        compose.runOnIdle { vm.setHideMuted(true) }
        compose.waitUntil(5000) { vm.uiState.value.weekDays.first { it.date == today }.episodes.size == 1 }
        compose.onNodeWithTag("schedule-count-$today", useUnmergedTree = true).assertTextEquals("1")
        compose.onNodeWithTag("schedule-date-$today").assertHeightIsAtLeast(48.dp).performClick().assertIsSelected()
        screenshot("date-counts-dark")
        compose.onNodeWithTag("shortcut-WEEK_OVERVIEW").performClick()
        compose.onNodeWithTag("agenda-days").performScrollToNode(hasTestTag("agenda-load-$today"))
        compose.onNode(hasText("1") and hasAnyAncestor(hasTestTag("agenda-load-$today"))).assertIsDisplayed()
        screenshot("weekly-load-dark")
    }

    @Test fun weekOverviewHasOneEntryPointAndRemainsAvailableWithoutAShortcut() {
        val vm = scheduleVm(); var shortcuts by mutableStateOf(DefaultToolShortcuts)
        show { CompositionLocalProvider(LocalWatchTools provides WatchToolsActions(data = WatchTools(shortcuts = shortcuts))) {
            ScheduleScreen({}, viewModel = vm)
        } }
        compose.waitUntil(5000) { vm.uiState.value.weekDays.isNotEmpty() }
        compose.onNodeWithTag("shortcut-WEEK_OVERVIEW").assertIsDisplayed()
        compose.onNodeWithTag("schedule-agenda").assertDoesNotExist()
        compose.runOnIdle { shortcuts = listOf(ToolShortcut.PLANNER, ToolShortcut.FAVORITES) }
        compose.onNodeWithTag("shortcut-WEEK_OVERVIEW").assertDoesNotExist()
        compose.onNodeWithTag("schedule-agenda").performScrollTo().performClick()
        compose.onNodeWithTag("agenda-total").assertIsDisplayed()
        assertEquals(ScheduleOverlay.Agenda, vm.openOverlay.value)
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
        compose.onNodeWithTag("agenda-reminder").performClick()
        compose.onNodeWithTag("agenda-reminder-FIFTEEN").performClick()
        compose.onNodeWithTag("agenda-export").performClick()
        val automation = instrumentation.uiAutomation
        compose.waitUntil(10_000) { automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui") == true }
        fun descendants(root: AccessibilityNodeInfo): Sequence<AccessibilityNodeInfo> = sequence { yield(root); for (i in 0 until root.childCount) root.getChild(i)?.let { yieldAll(descendants(it)) } }
        val name = "qa5100-${System.nanoTime() % 1_000_000}.ics"
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
        compose.onNodeWithTag("shortcut-WEEK_OVERVIEW").assertIsDisplayed()
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
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-character-finder"))
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
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-mute"))
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
        var characterRequests = 0
        val repo = object : AnimeDetailRepository {
            override fun getAnimeDetail(animeId: Int) = flowOf(AppResult.Success(detail))
            override suspend fun getCharacterDetail(characterId: Int): AppResult<CharacterDetail> {
                characterRequests++
                return if (characterRequests == 1) AppResult.Error(AppError.NoCache)
                else AppResult.Success(CharacterDetail(characterId, "Alpha", null, null, "Character description"))
            }
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
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-copy-synopsis"))
        compose.onNodeWithTag("detail-copy-synopsis").performClick()
        val synopsisClipboard = instrumentation.targetContext.getSystemService(android.content.ClipboardManager::class.java)
        compose.waitUntil(5000) { synopsisClipboard.primaryClip?.getItemAt(0)?.text?.toString() == detail.description!!.trim() }
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-share-progress"))
        compose.onNodeWithTag("detail-share-progress").performClick()
        compose.waitUntil(10000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("intentresolver") == true }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
        compose.waitUntil(10000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName }
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-copy-link"))
        compose.onNodeWithTag("detail-copy-link").performClick()
        compose.waitUntil(5000) { synopsisClipboard.primaryClip?.getItemAt(0)?.text?.toString() == "https://anilist.co/anime/1" }
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-titles"))
        compose.onNodeWithTag("detail-titles").performClick()
        compose.onNodeWithTag("copy-title-${R.string.detail_title_romaji}").performClick()
        val clipboard = instrumentation.targetContext.getSystemService(android.content.ClipboardManager::class.java)
        compose.waitUntil(5000) { clipboard.primaryClip?.getItemAt(0)?.text?.toString() == detail.titleRomaji }
        compose.onNodeWithContentDescription(text(android.R.string.cancel)).performClick(); compose.waitForIdle()
        screenshot("tools-after-back")
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-character-finder"))
        compose.onNodeWithTag("detail-character-finder").assertIsDisplayed().performClick()
        // Invoke the scrim's accessibility action. Its full-window bounds overlap the
        // expanded sheet; a coordinate tap at their center would hit sheet content.
        compose.onNodeWithContentDescription(instrumentation.targetContext.getString(androidx.compose.ui.R.string.close_sheet))
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-character-finder"))
        compose.onNodeWithTag("detail-character-finder").assertIsDisplayed()
        compose.onNodeWithContentDescription(text(android.R.string.cancel)).performClick(); compose.waitForIdle()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.onNodeWithTag("detail-tools").performClick()
        compose.onNodeWithTag("detail-character-finder").performClick()
        compose.onNodeWithTag("finder-character-1").performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithText(text(R.string.common_retry)).assertIsDisplayed()
        screenshot("character-error")
        compose.onNodeWithText(text(R.string.common_retry)).performClick()
        compose.onNodeWithText("Character description").assertIsDisplayed()
        screenshot("character-detail")
        compose.onNodeWithTag("character-share").performClick()
        compose.waitUntil(10000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("intentresolver") == true }
        screenshot("character-native-share", false)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
        compose.waitUntil(10000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName }
        compose.onNodeWithContentDescription(text(android.R.string.cancel)).performClick()
        compose.onNodeWithTag("detail-tools").performClick()
        compose.onNodeWithTag("detail-character-finder").performClick()
        compose.onNodeWithTag("finder-character-1").performClick()
        compose.onNodeWithText("Character description").assertIsDisplayed()
        assertEquals(2, characterRequests)
    }
    @Test fun fourNewScheduleFiltersSelectCombineAndReset() {
        var filter by mutableStateOf(ScheduleFilter())
        show(true) { ScheduleFilterSheet(filter, emptyList(), emptyList(), false, {}, {}, {}, {}, { filter = ScheduleFilter() }, {},
            onPremieresChange = { filter = filter.copy(premieresOnly = it) },
            onMinimumScoreChange = { filter = filter.copy(minimumScore = it) },
            onReleaseChange = { filter = filter.copy(release = it) },
            onTimeOfDayChange = { filter = filter.copy(timeOfDay = it) }) }
        fun pick(tag: String) { compose.onNodeWithTag("schedule-filter-list").performScrollToNode(hasTestTag(tag)); compose.onNodeWithTag(tag).performClick() }
        pick("schedule-premieres"); compose.onNodeWithTag("schedule-premieres").assertIsOn()
        pick("schedule-release-AIRING"); compose.onNodeWithTag("schedule-release-AIRING").assertIsSelected()
        pick("schedule-score-80"); compose.onNodeWithTag("schedule-score-80").assertIsSelected()
        pick("schedule-time-EVENING"); compose.onNodeWithTag("schedule-time-EVENING").assertIsSelected()
        assertTrue(filter.premieresOnly); assertEquals(80, filter.minimumScore)
        assertEquals(com.owlcoder.animeschedule.presentation.screens.discovery.ReleaseFilter.AIRING, filter.release)
        assertEquals(ScheduleTimeOfDay.EVENING, filter.timeOfDay)
        screenshot("new-schedule-filters")
        compose.onNodeWithText(text(R.string.filter_reset)).performClick(); assertEquals(ScheduleFilter(), filter)
    }
    @Test fun reminderPickerChangesExportChoiceAndBackKeepsOneSheet() {
        var reminder by mutableStateOf(CalendarReminder.NONE)
        var dismissed = false
        show { ScheduleAgendaSheet(days, today, {}, {}, {}, { dismissed = true }, reminder, { reminder = it }) }
        compose.onNodeWithTag("agenda-reminder").performClick()
        compose.onNodeWithTag("agenda-reminder-THIRTY").performClick()
        assertEquals(CalendarReminder.THIRTY, reminder)
        compose.onNodeWithTag("agenda-reminder").performClick()
        compose.onNodeWithTag("agenda-reminder-THIRTY").assertIsSelected()
        screenshot("calendar-reminders")
        Espresso.pressBack(); compose.waitForIdle()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithTag("agenda-export").assertIsDisplayed(); assertFalse(dismissed)
    }
    @Test fun relatedFinderFiltersAndOpensLoadedAnimeInOneSheet() {
        var picked = 0
        val relations = listOf(RelatedAnime(2,"Alpha Returns",null,"TV","FINISHED","SEQUEL"), RelatedAnime(3,"Alpha Origins",null,"MOVIE","FINISHED","PREQUEL"), RelatedAnime(4,"Alpha Manga",null,null,null,"ADAPTATION","MANGA"))
        show(true) { DetailToolsSheet(detail.copy(relations = relations), {}, {}, {}, onRelated = { picked = it }) }
        compose.onNodeWithTag("detail-related-finder").performClick()
        compose.onNodeWithTag("related-type-SEQUEL").performClick().assertIsSelected()
        val relatedInput = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("related-search")))
        relatedInput.performTextInput("alpha"); relatedInput.performImeAction()
        compose.onNodeWithTag("finder-related-3").assertDoesNotExist(); compose.onNodeWithTag("finder-related-4").assertDoesNotExist()
        screenshot("related-finder")
        compose.onNodeWithTag("finder-related-2").performClick(); assertEquals(2,picked)
        // Native IME dismissal finishes after Compose becomes idle; Back must reach the sheet.
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        Espresso.pressBack(); compose.waitForIdle()
        screenshot("related-after-back")
        compose.onNodeWithTag("detail-related-finder").assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
    }
    @Test fun synopsisIsPlainTextAndSharedProgressContainsOnlyPublicListFields() {
        var copied = ""; var shared = ""
        show { DetailToolsSheet(detail.copy(description = "<b>Alpha</b> &amp; Beta<br>Next"), {}, {}, {},
            onCopySynopsis = { copied = it }, onShareProgress = { shared = it }) }
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-copy-synopsis"))
        compose.onNodeWithTag("detail-copy-synopsis").performClick()
        assertEquals("Alpha & Beta\nNext",copied)
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-share-progress"))
        compose.onNodeWithTag("detail-share-progress").performClick()
        assertTrue(shared.contains("4/12")); assertTrue(shared.contains("8/10")); assertTrue(shared.contains("https://myanimelist.net/anime/101"))
        screenshot("expanded-detail-tools")
    }

    @Test fun agendaEstimateUsesPersonalDurationsAndNextDaySkipsEmptyDates() {
        var selected by mutableStateOf(today)
        val tools = WatchTools(episodeMinutes = 24, durationOverrides = mapOf(101 to 48))
        show(true) { CompositionLocalProvider(LocalWatchTools provides WatchToolsActions(data = tools)) {
            ScheduleAgendaSheet(days, selected, { selected = it }, {}, {}, {})
        } }
        compose.onNodeWithTag("agenda-estimate").assertTextContains("1 h 36 min", substring = true)
        compose.onNodeWithTag("agenda-days").performScrollToNode(hasTestTag("agenda-next-day"))
        compose.onNodeWithTag("agenda-next-day").performClick()
        assertEquals(today.plusDays(1),selected)
        compose.onNodeWithTag("agenda-next-day").assertIsNotEnabled()
        screenshot("weekly-time-and-next-day")
    }
    @Test fun weeklySharingOpensNativeChooserEvenForAnEmptySelectedDay() {
        val vm = scheduleVm(); vm.setOpenOverlay(ScheduleOverlay.Agenda)
        show { ScheduleScreen({}, viewModel = vm) }
        compose.waitUntil(5000) { vm.uiState.value.weekDays.isNotEmpty() }
        compose.onNodeWithTag("agenda-days").performScrollToNode(hasTestTag("agenda-day-${today.plusDays(6)}"))
        compose.onNodeWithTag("agenda-day-${today.plusDays(6)}").performClick()
        compose.onNodeWithTag("shortcut-WEEK_OVERVIEW").performClick()
        compose.onNodeWithTag("agenda-share").assertIsNotEnabled()
        compose.onNodeWithTag("agenda-days").performScrollToNode(hasTestTag("agenda-share-week"))
        compose.onNodeWithTag("agenda-share-week").performClick()
        compose.waitUntil(10000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("intentresolver") == true }
        screenshot("weekly-native-share", false)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
        compose.waitUntil(10000) { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == instrumentation.targetContext.packageName }
        assertEquals(ScheduleOverlay.None,vm.openOverlay.value)
    }
    @Test fun synopsisReaderPreservesTextAndCopyLinkUsesCanonicalCatalogId() {
        var copied = "";var dismissed = false
        val description = "<p>Alpha &amp; Beta</p><p>" + "Long description. ".repeat(120) + "The end.</p>"
        show { DetailToolsSheet(detail.copy(description = description), { dismissed = true }, {}, {}, onCopyLink = { copied = it }) }
        compose.onNodeWithTag("detail-read-synopsis").performClick()
        compose.onNodeWithTag("synopsis-text").assertTextContains("Alpha & Beta",substring=true)
        compose.onNodeWithTag("synopsis-text").assertTextContains("The end.",substring=true)
        compose.onNodeWithTag("synopsis-reader").performTouchInput { swipeUp() }
        screenshot("synopsis-reader")
        Espresso.pressBack();compose.waitForIdle();assertFalse(dismissed)
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithTag("detail-tools-menu").performScrollToNode(hasTestTag("detail-copy-link"))
        compose.onNodeWithTag("detail-copy-link").performClick();assertEquals("https://anilist.co/anime/1",copied)
        screenshot("aligned-detail-actions")
    }
    @Test fun characterSharingUsesTheLoadedPublicProfile() {
        var shared = ""
        show(true) { CharacterOverlaySheet(CharacterOverlayState(isVisible=true,detail=CharacterDetail(7,"Alpha","アルファ",null,"<b>Alpha</b> &amp; Beta")), {}, {}, { shared = it.shareText() }) }
        compose.onNodeWithText("Alpha & Beta").assertIsDisplayed()
        compose.onNodeWithTag("character-share").performClick()
        assertEquals("Alpha\nアルファ\nhttps://anilist.co/character/7",shared)
        screenshot("character-profile-share")
    }

}
