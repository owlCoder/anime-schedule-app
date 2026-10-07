package com.owlcoder.animeschedule.presentation.screens.schedule

import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.domain.model.AccentColor
import com.owlcoder.animeschedule.domain.model.AppLanguage
import com.owlcoder.animeschedule.domain.model.UserPreferences
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.domain.repository.NotificationRepository
import com.owlcoder.animeschedule.domain.repository.ScheduleRepository
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.repository.WorkScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    private class FakeScheduleRepository : ScheduleRepository {
        var days: (LocalDate) -> List<ScheduleDay> = { emptyList() }
        var refreshCalls = 0
        var refreshGate: CompletableDeferred<AppResult<Unit>> = CompletableDeferred(AppResult.Success(Unit))
        var lastZone: ZoneId? = null

        override fun getWeekSchedule(zoneId: ZoneId, today: LocalDate): Flow<List<ScheduleDay>> {
            lastZone = zoneId
            return flowOf(days(today))
        }

        override suspend fun refreshSchedule(zoneId: ZoneId): AppResult<Unit> {
            refreshCalls++
            return refreshGate.await()
        }
    }

    private class FakeSettings(zone: String = "UTC") : SettingsRepository {
        val prefs = MutableStateFlow(UserPreferences(timezoneId = zone))
        override val userPreferencesFlow: Flow<UserPreferences> = prefs
        override suspend fun setTimezoneId(timezoneId: String) = Unit
        override suspend fun setThemeOptions(options: com.owlcoder.animeschedule.domain.model.ThemeOptions) = Unit
        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setNotificationOffset(minutes: Int) = Unit
        override suspend fun setAccentColor(color: AccentColor) = Unit
        override suspend fun setAppLanguage(language: AppLanguage) = Unit
        override suspend fun setCacheRetentionDays(days: Int) = Unit
    }

    private class FakeMal(val entries: List<MalListEntry> = emptyList()) : MalRepository {
        override fun getUserList(): Flow<List<MalListEntry>> = flowOf(entries)
        override suspend fun updateListEntry(animeId: Int, update: MalListUpdate) = AppResult.Success(Unit)
        override suspend fun incrementEpisode(animeId: Int) = AppResult.Success(Unit)
        override suspend fun removeListEntry(animeId: Int) = AppResult.Success(Unit)
        override suspend fun refreshUserList(force: Boolean) = true
        override suspend fun flushPendingUpdates() = true
    }

    private object FakeNotifications : NotificationRepository {
        override fun getAll(): Flow<List<AppNotification>> = flowOf(emptyList())
        override fun getUnreadCount(): Flow<Int> = flowOf(0)
        override suspend fun markRead(id: Int) = Unit
        override suspend fun markAllRead() = Unit
        override suspend fun deleteRead() = 0
    }

    private class FakeWork : WorkScheduler {
        var notificationChecks = 0
        override fun scheduleFlushPendingUpdates() = Unit
        override fun checkAiringNotifications() { notificationChecks++ }
    }

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun episode(id: Int, status: WatchStatus? = null, genres: List<String> = emptyList()) = AiringEpisode(
        airingId = id, animeId = id, malId = id, episode = 1, airingAtEpochSeconds = 1_000L + id,
        title = "Show $id", titleRomaji = null, coverImageUrl = null, coverColor = null,
        genres = genres, averageScore = null, totalEpisodes = null, status = null, format = "TV",
        malListEntry = status?.let {
            MalListEntry(animeId = id, status = it, episodesWatched = 0, score = 0, totalEpisodes = 12)
        },
    )

    private fun TestScope.viewModel(
        schedule: FakeScheduleRepository,
        settings: FakeSettings = FakeSettings(),
        work: FakeWork = FakeWork(),
    ): ScheduleViewModel {
        val vm = ScheduleViewModel(schedule, settings, FakeMal(), FakeNotifications, work)
        // Keep the WhileSubscribed pipeline running, like a visible screen would.
        backgroundScope.launch { vm.uiState.collect { } }
        return vm
    }

    @Test
    fun `today and tomorrow are taken from the week by the zone's date and dropped shows are hidden`() = runTest {
        val repo = FakeScheduleRepository().apply {
            days = { today ->
                listOf(
                    ScheduleDay(today, listOf(episode(1), episode(2, WatchStatus.DROPPED))),
                    ScheduleDay(today.plusDays(1), listOf(episode(3, genres = listOf("Action")))),
                    ScheduleDay(today.plusDays(4), listOf(episode(4))),
                )
            }
        }
        val vm = viewModel(repo, FakeSettings(zone = "Pacific/Kiritimati"))
        runCurrent()

        val state = vm.uiState.value
        assertEquals(ZoneId.of("Pacific/Kiritimati"), state.zoneId)
        assertEquals(ZoneId.of("Pacific/Kiritimati"), repo.lastZone)
        assertEquals(listOf(1), state.todayEpisodes.map { it.airingId })
        assertEquals(listOf(3), state.tomorrowEpisodes.map { it.airingId })
        assertEquals(listOf(4), state.episodesForDate(state.today.plusDays(4)).map { it.airingId })
        assertEquals(listOf("Action"), state.availableGenres)
    }

    @Test
    fun `concurrent refresh requests share one network call`() = runTest {
        val repo = FakeScheduleRepository().apply { refreshGate = CompletableDeferred() }
        val vm = viewModel(repo)
        runCurrent() // init triggers the first refresh

        vm.refresh()
        vm.refresh()
        runCurrent()

        assertEquals(1, repo.refreshCalls)
        assertTrue(vm.uiState.value.isLoading)

        repo.refreshGate.complete(AppResult.Success(Unit))
        runCurrent()
        assertTrue(!vm.uiState.value.isLoading)

        vm.refresh()
        runCurrent()
        assertEquals("a refresh after completion is allowed", 2, repo.refreshCalls)
    }

    @Test
    fun `successful refresh asks for a notification check exactly once`() = runTest {
        val work = FakeWork()
        viewModel(FakeScheduleRepository(), work = work)
        runCurrent()

        assertEquals(1, work.notificationChecks)
    }

    @Test
    fun `a failed refresh is only an error when there is nothing cached to show`() = runTest {
        val failing = FakeScheduleRepository().apply {
            refreshGate = CompletableDeferred(AppResult.Error(AppError.Network("offline")))
        }
        val emptyVm = viewModel(failing)
        runCurrent()
        assertEquals(R.string.error_load_schedule, emptyVm.uiState.value.errorRes)

        val withCache = FakeScheduleRepository().apply {
            refreshGate = CompletableDeferred(AppResult.Error(AppError.Network("offline")))
            days = { today -> listOf(ScheduleDay(today, listOf(episode(1)))) }
        }
        val cachedVm = viewModel(withCache)
        runCurrent()
        assertNull(cachedVm.uiState.value.errorRes)
        assertEquals(1, cachedVm.uiState.value.todayEpisodes.size)
    }

    @Test
    fun `filters apply to every day and keep the available options stable`() = runTest {
        val repo = FakeScheduleRepository().apply {
            days = { today ->
                listOf(ScheduleDay(today, listOf(episode(1, genres = listOf("Action")), episode(2, genres = listOf("Drama")))))
            }
        }
        val vm = viewModel(repo)
        runCurrent()

        vm.toggleGenre("Drama")
        runCurrent()

        val state = vm.uiState.value
        assertEquals(listOf(2), state.todayEpisodes.map { it.airingId })
        assertEquals(listOf("Action", "Drama"), state.availableGenres)
    }
    @Test fun `cached schedule is visible while the initial network request is still pending`() = runTest {
        val repo=FakeScheduleRepository().apply {
            refreshGate=CompletableDeferred()
            days={ today -> listOf(ScheduleDay(today.plusDays(1),listOf(episode(7)))) }
        }
        val vm=viewModel(repo);runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(false,vm.uiState.value.isInitialLoad)
        assertEquals(7,vm.uiState.value.tomorrowEpisodes.single().airingId)
    }
    @Test fun `a first launch without cache keeps the content loader until refresh finishes`() = runTest {
        val repo=FakeScheduleRepository().apply { refreshGate=CompletableDeferred() }
        val vm=viewModel(repo);runCurrent()
        assertTrue(vm.uiState.value.isInitialLoad)
        repo.refreshGate.complete(AppResult.Error(AppError.Network("offline")));runCurrent()
        assertEquals(false,vm.uiState.value.isInitialLoad)
        assertEquals(R.string.error_load_schedule,vm.uiState.value.errorRes)
    }
}
