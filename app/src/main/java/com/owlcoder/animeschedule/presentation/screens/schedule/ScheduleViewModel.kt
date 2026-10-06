package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.core.time.currentDateFlow
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.domain.repository.NotificationRepository
import com.owlcoder.animeschedule.domain.repository.ScheduleRepository
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.repository.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.domain.model.effectiveZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

data class ScheduleFilter(
    val onlyMyList: Boolean = false,
    val query: String = "",
    val upcomingOnly: Boolean = false,
    val genres: Set<String> = emptySet(),
    val formats: Set<String> = emptySet(),
) {
    val isActive: Boolean get() = onlyMyList || query.isNotBlank() || upcomingOnly || genres.isNotEmpty() || formats.isNotEmpty()
}

enum class ScheduleSection { TODAY, TOMORROW, WEEK }

sealed interface ScheduleOverlay {
    data object None : ScheduleOverlay
    data object Filter : ScheduleOverlay
    data object Notifications : ScheduleOverlay
    data object Seasonal : ScheduleOverlay
    data class SeeAll(val section: ScheduleSection) : ScheduleOverlay
}

data class ScheduleUiState(
    /** The zone every date and time on the schedule is shown in. */
    val zoneId: ZoneId = ZoneId.systemDefault(),
    /** Today in [zoneId]; rolls over at local midnight while the screen stays open. */
    val today: LocalDate = LocalDate.now(),
    val todayEpisodes: List<AiringEpisode> = emptyList(),
    val tomorrowEpisodes: List<AiringEpisode> = emptyList(),
    val weekDays: List<ScheduleDay> = emptyList(),
    val isLoading: Boolean = true,
    val isInitialLoad: Boolean = true,
    @StringRes val errorRes: Int? = null,
    val isLoggedIn: Boolean = false,
    val filter: ScheduleFilter = ScheduleFilter(),
    val availableGenres: List<String> = emptyList(),
    val availableFormats: List<String> = emptyList(),
    val pendingIncrementIds: Set<Int> = emptySet(),
    val unreadNotificationCount: Int = 0,
    val recentlyChangedEntries: List<MalListEntry> = emptyList(),
) {
    /** Episodes airing on [date], in the order the filter left them. */
    fun episodesForDate(date: LocalDate): List<AiringEpisode> = when (date) {
        today -> todayEpisodes
        today.plusDays(1) -> tomorrowEpisodes
        else -> weekDays.firstOrNull { it.date == date }?.episodes.orEmpty()
    }
}

/** Everything that depends only on the cached schedule and the user's preferences. */
private data class ScheduleSnapshot(
    val zoneId: ZoneId,
    val today: LocalDate,
    val days: List<ScheduleDay>,
    val availableGenres: List<String>,
    val availableFormats: List<String>,
    val isLoggedIn: Boolean,
)

private data class RefreshStatus(
    val isRefreshing: Boolean = true,
    val hasLoadedOnce: Boolean = false,
    val failed: Boolean = false,
)

private data class Auxiliary(
    val pendingIncrementIds: Set<Int>,
    val unreadNotificationCount: Int,
    val recentlyChanged: List<MalListEntry>,
    val refresh: RefreshStatus,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
    private val malRepository: MalRepository,
    notificationRepository: NotificationRepository,
    private val workScheduler: WorkScheduler,
) : ViewModel() {

    private val _refreshStatus = MutableStateFlow(RefreshStatus())
    private val _filter = MutableStateFlow(ScheduleFilter())
    private val _pendingIncrementIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _openOverlay = MutableStateFlow<ScheduleOverlay>(ScheduleOverlay.None)
    val openOverlay: StateFlow<ScheduleOverlay> = _openOverlay

    sealed interface IncrementEvent {
        data object Success : IncrementEvent
        data object Updated : IncrementEvent
        data object Removed : IncrementEvent
        data object Error : IncrementEvent
    }

    private val _incrementEvent = Channel<IncrementEvent>(Channel.BUFFERED)
    val incrementEvent = _incrementEvent.receiveAsFlow()

    private var refreshJob: Job? = null

    private val snapshot: Flow<ScheduleSnapshot> = settingsRepository.userPreferencesFlow
        .map { it.effectiveZoneId to it.malLoggedIn }
        .distinctUntilChanged()
        .flatMapLatest { (zoneId, isLoggedIn) ->
            currentDateFlow(zoneId).flatMapLatest { today ->
                scheduleRepository.getWeekSchedule(zoneId, today).map { days ->
                    buildSnapshot(zoneId, today, days, isLoggedIn)
                }
            }
        }

    private val recentlyChanged: Flow<List<MalListEntry>> = malRepository.getUserList()
        .map { entries ->
            // Parse each timestamp once; sorting by a computed key would re-parse per comparison.
            entries.map { it to (it.updatedAt.toInstantOrNull() ?: Instant.EPOCH) }
                .sortedByDescending { (_, updatedAt) -> updatedAt }
                .take(RECENTLY_CHANGED_COUNT)
                .map { (entry, _) -> entry }
        }

    private val auxiliary: Flow<Auxiliary> = combine(
        _pendingIncrementIds,
        notificationRepository.getUnreadCount(),
        recentlyChanged,
        _refreshStatus,
    ) { pending, unread, recent, refresh -> Auxiliary(pending, unread, recent, refresh) }

    // Run the time filter only while it is enabled and the screen is subscribed.
    private val filterClock = _filter.map { it.upcomingOnly }.distinctUntilChanged().flatMapLatest { enabled ->
        if (!enabled) flowOf(0L) else flow {
            while (true) {
                emit(Instant.now().epochSecond)
                delay(60_000L)
            }
        }
    }

    val uiState: StateFlow<ScheduleUiState> = combine(snapshot, _filter, auxiliary, filterClock) { snapshot, filter, aux, now ->
        val byDate = snapshot.days.associateBy { it.date }
        ScheduleUiState(
            zoneId = snapshot.zoneId,
            today = snapshot.today,
            todayEpisodes = byDate[snapshot.today]?.episodes.orEmpty().applyFilter(filter, now),
            tomorrowEpisodes = byDate[snapshot.today.plusDays(1)]?.episodes.orEmpty().applyFilter(filter, now),
            weekDays = snapshot.days.map { day -> day.copy(episodes = day.episodes.applyFilter(filter, now)) },
            isLoading = aux.refresh.isRefreshing,
            isInitialLoad = aux.refresh.isRefreshing && !aux.refresh.hasLoadedOnce,
            // A failed refresh only matters when there is nothing cached to show instead.
            errorRes = R.string.error_load_schedule.takeIf {
                aux.refresh.failed && snapshot.days.isEmpty()
            },
            isLoggedIn = snapshot.isLoggedIn,
            filter = filter,
            availableGenres = snapshot.availableGenres,
            availableFormats = snapshot.availableFormats,
            pendingIncrementIds = aux.pendingIncrementIds,
            unreadNotificationCount = aux.unreadNotificationCount,
            recentlyChangedEntries = aux.recentlyChanged,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    init {
        refresh()
    }

    fun setScheduleQuery(query: String) = _filter.update { it.copy(query = query) }

    fun setUpcomingOnly(enabled: Boolean) = _filter.update { it.copy(upcomingOnly = enabled) }

    fun setOnlyMyList(enabled: Boolean) = _filter.update { it.copy(onlyMyList = enabled) }

    fun toggleGenre(genre: String) = _filter.update { filter ->
        filter.copy(genres = filter.genres.toggled(genre))
    }

    fun toggleFormat(format: String) = _filter.update { filter ->
        filter.copy(formats = filter.formats.toggled(format))
    }

    fun clearFilter() = _filter.update { ScheduleFilter() }

    fun setOpenOverlay(overlay: ScheduleOverlay) {
        _openOverlay.value = overlay
    }

    /** Pulls a fresh schedule. A refresh already in flight is reused instead of duplicated. */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _refreshStatus.update { it.copy(isRefreshing = true) }
            var failed = true
            try {
                val zoneId = settingsRepository.userPreferencesFlow.first().effectiveZoneId
                val result = withTimeoutOrNull(SCHEDULE_REFRESH_TIMEOUT_MS) {
                    scheduleRepository.refreshSchedule(zoneId)
                }
                failed = result !is AppResult.Success
                if (!failed) workScheduler.checkAiringNotifications()
            } finally {
                _refreshStatus.value = RefreshStatus(isRefreshing = false, hasLoadedOnce = true, failed = failed)
            }
        }
    }

    fun incrementEpisode(malId: Int) {
        if (malId in _pendingIncrementIds.value) return
        _pendingIncrementIds.update { it + malId }
        viewModelScope.launch {
            try {
                val result = malRepository.incrementEpisode(malId)
                _incrementEvent.send(
                    if (result is AppResult.Success) IncrementEvent.Success else IncrementEvent.Error,
                )
            } finally {
                _pendingIncrementIds.update { it - malId }
            }
        }
    }

    fun updateEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch {
            val result = malRepository.updateListEntry(animeId, update)
            _incrementEvent.send(
                if (result is AppResult.Success) IncrementEvent.Updated else IncrementEvent.Error,
            )
        }
    }

    fun removeEntry(animeId: Int) {
        viewModelScope.launch {
            val result = malRepository.removeListEntry(animeId)
            _incrementEvent.send(
                if (result is AppResult.Success) IncrementEvent.Removed else IncrementEvent.Error,
            )
        }
    }

    private companion object {
        const val SCHEDULE_REFRESH_TIMEOUT_MS = 12_000L
        const val RECENTLY_CHANGED_COUNT = 15
    }
}

private fun buildSnapshot(
    zoneId: ZoneId,
    today: LocalDate,
    days: List<ScheduleDay>,
    isLoggedIn: Boolean,
): ScheduleSnapshot {
    val visibleDays = days
        .map { day -> day.copy(episodes = day.episodes.filter { it.malListEntry?.status != WatchStatus.DROPPED }) }
        .filter { it.episodes.isNotEmpty() }
    val episodes = visibleDays.flatMap { it.episodes }
    return ScheduleSnapshot(
        zoneId = zoneId,
        today = today,
        days = visibleDays,
        availableGenres = episodes.flatMap { it.genres }.distinct().sorted(),
        availableFormats = episodes.mapNotNull { it.format }.distinct().sorted(),
        isLoggedIn = isLoggedIn,
    )
}

internal fun List<AiringEpisode>.applyFilter(filter: ScheduleFilter, nowEpochSeconds: Long): List<AiringEpisode> {
    if (!filter.isActive) return this
    val query = filter.query.trim()
    return filter { episode ->
        (query.isEmpty() || episode.title.contains(query, ignoreCase = true) || episode.titleRomaji?.contains(query, ignoreCase = true) == true) &&
            (!filter.upcomingOnly || episode.airingAtEpochSeconds > nowEpochSeconds) &&
            (!filter.onlyMyList || episode.malListEntry != null) &&
            (filter.genres.isEmpty() || episode.genres.any { it in filter.genres }) &&
            (filter.formats.isEmpty() || episode.format in filter.formats)
    }
}

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item

private fun String?.toInstantOrNull(): Instant? {
    if (isNullOrBlank()) return null
    return try {
        Instant.parse(this)
    } catch (_: DateTimeParseException) {
        try {
            java.time.OffsetDateTime.parse(this).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
