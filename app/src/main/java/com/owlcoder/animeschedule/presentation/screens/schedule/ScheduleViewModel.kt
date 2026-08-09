package com.owlcoder.animeschedule.presentation.screens.schedule

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.work.AiringNotificationWorker
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.usecase.GetMalUserListUseCase
import com.owlcoder.animeschedule.domain.usecase.GetTodayScheduleUseCase
import com.owlcoder.animeschedule.domain.usecase.GetTomorrowScheduleUseCase
import com.owlcoder.animeschedule.domain.usecase.GetUnreadCountUseCase
import com.owlcoder.animeschedule.domain.usecase.GetWeekScheduleUseCase
import com.owlcoder.animeschedule.domain.usecase.IncrementEpisodeUseCase
import com.owlcoder.animeschedule.domain.usecase.RefreshScheduleUseCase
import com.owlcoder.animeschedule.domain.usecase.RemoveMalListEntryUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject

private fun String?.toInstantOrNull(): Instant? {
    if (this.isNullOrBlank()) return null
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

data class ScheduleFilter(
    val onlyMyList: Boolean = false,
    val genres: Set<String> = emptySet(),
    val formats: Set<String> = emptySet(),
) {
    val isActive: Boolean get() = onlyMyList || genres.isNotEmpty() || formats.isNotEmpty()
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
)

private fun List<AiringEpisode>.excludeDropped(): List<AiringEpisode> =
    filter { it.malListEntry?.status != WatchStatus.DROPPED }

private fun List<ScheduleDay>.excludeDroppedFromWeek(): List<ScheduleDay> =
    map { day -> day.copy(episodes = day.episodes.excludeDropped()) }

private fun List<AiringEpisode>.applyFilter(filter: ScheduleFilter): List<AiringEpisode> {
    if (!filter.isActive) return this
    return filter { episode ->
        (!filter.onlyMyList || episode.malListEntry != null) &&
            (filter.genres.isEmpty() || episode.genres.any { it in filter.genres }) &&
            (filter.formats.isEmpty() || episode.format in filter.formats)
    }
}

private fun List<ScheduleDay>.applyFilterToWeek(filter: ScheduleFilter): List<ScheduleDay> {
    if (!filter.isActive) return this
    return map { day -> day.copy(episodes = day.episodes.applyFilter(filter)) }
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTodayScheduleUseCase: GetTodayScheduleUseCase,
    private val getTomorrowScheduleUseCase: GetTomorrowScheduleUseCase,
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val refreshScheduleUseCase: RefreshScheduleUseCase,
    private val incrementEpisodeUseCase: IncrementEpisodeUseCase,
    private val updateMalListEntryUseCase: UpdateMalListEntryUseCase,
    private val removeMalListEntryUseCase: RemoveMalListEntryUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val getMalUserListUseCase: GetMalUserListUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _hasLoadedOnce = MutableStateFlow(false)
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

    val uiState: StateFlow<ScheduleUiState> = settingsRepository.userPreferencesFlow
        .flatMapLatest { preferences ->
            val zoneId = settingsRepository.getEffectiveZoneId(preferences)
            combine(
                getTodayScheduleUseCase(zoneId),
                getTomorrowScheduleUseCase(zoneId),
                getWeekScheduleUseCase(zoneId),
                _isLoading,
            ) { today, tomorrow, week, loading ->
                val todayList = ((today as? AppResult.Success)?.data ?: emptyList()).excludeDropped()
                val tomorrowList = ((tomorrow as? AppResult.Success)?.data ?: emptyList()).excludeDropped()
                val weekList = ((week as? AppResult.Success)?.data ?: emptyList()).excludeDroppedFromWeek()
                val allEpisodes = todayList + tomorrowList + weekList.flatMap { it.episodes }
                val genres = allEpisodes.flatMap { it.genres }.distinct().sorted()
                val formats = allEpisodes.mapNotNull { it.format }.distinct().sorted()
                ScheduleUiState(
                    todayEpisodes = todayList,
                    tomorrowEpisodes = tomorrowList,
                    weekDays = weekList,
                    isLoading = loading,
                    errorRes = if (today is AppResult.Error) R.string.error_load_schedule else null,
                    isLoggedIn = preferences.malLoggedIn,
                    availableGenres = genres,
                    availableFormats = formats,
                )
            }
                .combine(_filter) { state, filter ->
                    state.copy(
                        filter = filter,
                        todayEpisodes = state.todayEpisodes.applyFilter(filter),
                        tomorrowEpisodes = state.tomorrowEpisodes.applyFilter(filter),
                        weekDays = state.weekDays.applyFilterToWeek(filter),
                    )
                }
                .combine(_pendingIncrementIds) { state, pending ->
                    state.copy(pendingIncrementIds = pending)
                }
                .combine(getUnreadCountUseCase()) { state, unread ->
                    state.copy(unreadNotificationCount = unread)
                }
                .combine(getMalUserListUseCase()) { state, listResult ->
                    val entries = (listResult as? AppResult.Success)?.data ?: emptyList()
                    val recentlyChanged = entries
                        .sortedByDescending { it.updatedAt.toInstantOrNull() ?: Instant.EPOCH }
                        .take(15)
                    state.copy(recentlyChangedEntries = recentlyChanged)
                }
                .combine(_hasLoadedOnce) { state, loadedOnce ->
                    state.copy(isInitialLoad = state.isLoading && !loadedOnce)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    init {
        refresh()
    }

    fun setOnlyMyList(enabled: Boolean) = _filter.update { it.copy(onlyMyList = enabled) }

    fun toggleGenre(genre: String) = _filter.update { filter ->
        val genres = if (genre in filter.genres) filter.genres - genre else filter.genres + genre
        filter.copy(genres = genres)
    }

    fun toggleFormat(format: String) = _filter.update { filter ->
        val formats = if (format in filter.formats) filter.formats - format else filter.formats + format
        filter.copy(formats = formats)
    }

    fun clearFilter() = _filter.update { ScheduleFilter() }

    fun setOpenOverlay(overlay: ScheduleOverlay) {
        _openOverlay.value = overlay
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val preferences = settingsRepository.userPreferencesFlow.stateIn(viewModelScope).value
                val zoneId = settingsRepository.getEffectiveZoneId(preferences)
                withTimeoutOrNull(SCHEDULE_REFRESH_TIMEOUT_MS) {
                    refreshScheduleUseCase(zoneId)
                }
                combine(
                    getTodayScheduleUseCase(zoneId),
                    getTomorrowScheduleUseCase(zoneId),
                    getWeekScheduleUseCase(zoneId),
                ) { today, tomorrow, week -> Triple(today, tomorrow, week) }.first()
            } finally {
                _isLoading.value = false
                _hasLoadedOnce.value = true
                runCatching { AiringNotificationWorker.runNow(context) }
            }
        }
    }

    fun incrementEpisode(malId: Int) {
        if (malId in _pendingIncrementIds.value) return
        _pendingIncrementIds.update { it + malId }
        viewModelScope.launch {
            try {
                val result = incrementEpisodeUseCase(malId)
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
            val result = updateMalListEntryUseCase(animeId, update)
            _incrementEvent.send(
                if (result is AppResult.Success) IncrementEvent.Updated else IncrementEvent.Error,
            )
        }
    }

    fun removeEntry(animeId: Int) {
        viewModelScope.launch {
            val result = removeMalListEntryUseCase(animeId)
            _incrementEvent.send(
                if (result is AppResult.Success) IncrementEvent.Removed else IncrementEvent.Error,
            )
        }
    }

    private companion object {
        const val SCHEDULE_REFRESH_TIMEOUT_MS = 12_000L
    }
}
