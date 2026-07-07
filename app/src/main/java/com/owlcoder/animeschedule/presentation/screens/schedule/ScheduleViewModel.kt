package com.owlcoder.animeschedule.presentation.screens.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.data.work.AiringNotificationWorker
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.usecase.GetTodayScheduleUseCase
import com.owlcoder.animeschedule.domain.usecase.GetTomorrowScheduleUseCase
import com.owlcoder.animeschedule.domain.usecase.GetUnreadCountUseCase
import com.owlcoder.animeschedule.domain.usecase.GetWeekScheduleUseCase
import com.owlcoder.animeschedule.domain.usecase.IncrementEpisodeUseCase
import com.owlcoder.animeschedule.domain.usecase.RefreshScheduleUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import javax.inject.Inject

data class ScheduleFilter(
    val onlyMyList: Boolean = false,
    val genres: Set<String> = emptySet(),
    val formats: Set<String> = emptySet()
) {
    val isActive: Boolean get() = onlyMyList || genres.isNotEmpty() || formats.isNotEmpty()
}

data class ScheduleUiState(
    val todayEpisodes: List<AiringEpisode> = emptyList(),
    val tomorrowEpisodes: List<AiringEpisode> = emptyList(),
    val weekDays: List<ScheduleDay> = emptyList(),
    val isLoading: Boolean = true,
    /** True only while the very first load is in flight (drives the animated splash);
     *  a pull-to-refresh sets [isLoading] but NOT this, so it shows the refresh spinner
     *  over existing content instead of the full-screen splash. */
    val isInitialLoad: Boolean = true,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val filter: ScheduleFilter = ScheduleFilter(),
    val availableGenres: List<String> = emptyList(),
    val availableFormats: List<String> = emptyList(),
    val pendingIncrementIds: Set<Int> = emptySet(),
    val unreadNotificationCount: Int = 0
)

private fun List<AiringEpisode>.applyFilter(filter: ScheduleFilter): List<AiringEpisode> {
    if (!filter.isActive) return this
    return filter { ep ->
        (!filter.onlyMyList || ep.malListEntry != null) &&
        (filter.genres.isEmpty() || ep.genres.any { it in filter.genres }) &&
        (filter.formats.isEmpty() || ep.format in filter.formats)
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
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    // Flips to true after the first refresh completes; stays true forever after, so subsequent
    // refreshes (pull-to-refresh) never re-show the full-screen animated splash.
    private val _hasLoadedOnce = MutableStateFlow(false)
    private val _filter = MutableStateFlow(ScheduleFilter())
    private val _pendingIncrementIds = MutableStateFlow<Set<Int>>(emptySet())

    sealed interface IncrementEvent {
        data object Success : IncrementEvent
        data object Error : IncrementEvent
    }
    private val _incrementEvent = Channel<IncrementEvent>(Channel.BUFFERED)
    val incrementEvent = _incrementEvent.receiveAsFlow()

    val uiState: StateFlow<ScheduleUiState> = settingsRepository.userPreferencesFlow
        .flatMapLatest { prefs ->
            val zoneId = settingsRepository.getEffectiveZoneId(prefs)
            combine(
                getTodayScheduleUseCase(zoneId),
                getTomorrowScheduleUseCase(zoneId),
                getWeekScheduleUseCase(zoneId),
                _isLoading
            ) { today, tomorrow, week, loading ->
                val todayList = (today as? AppResult.Success)?.data ?: emptyList()
                val tomorrowList = (tomorrow as? AppResult.Success)?.data ?: emptyList()
                val weekList = (week as? AppResult.Success)?.data ?: emptyList()
                val allEpisodes = todayList + tomorrowList + weekList.flatMap { it.episodes }
                val genres = allEpisodes.flatMap { it.genres }.distinct().sorted()
                val formats = allEpisodes.mapNotNull { it.format }.distinct().sorted()
                ScheduleUiState(
                    todayEpisodes = todayList,
                    tomorrowEpisodes = tomorrowList,
                    weekDays = weekList,
                    isLoading = loading,
                    error = if (today is AppResult.Error) "Greška pri učitavanju rasporeda" else null,
                    isLoggedIn = prefs.malLoggedIn,
                    availableGenres = genres,
                    availableFormats = formats
                )
            }
            .combine(_filter) { state, f ->
                state.copy(
                    filter = f,
                    todayEpisodes = state.todayEpisodes.applyFilter(f),
                    tomorrowEpisodes = state.tomorrowEpisodes.applyFilter(f),
                    weekDays = state.weekDays.applyFilterToWeek(f)
                )
            }
            .combine(_pendingIncrementIds) { state, pending -> state.copy(pendingIncrementIds = pending) }
            .combine(getUnreadCountUseCase()) { state, unread -> state.copy(unreadNotificationCount = unread) }
            .combine(_hasLoadedOnce) { state, loadedOnce ->
                state.copy(isInitialLoad = state.isLoading && !loadedOnce)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    init { refresh() }

    fun setOnlyMyList(enabled: Boolean) = _filter.update { it.copy(onlyMyList = enabled) }

    fun toggleGenre(genre: String) = _filter.update { f ->
        val updated = if (genre in f.genres) f.genres - genre else f.genres + genre
        f.copy(genres = updated)
    }

    fun toggleFormat(format: String) = _filter.update { f ->
        val updated = if (format in f.formats) f.formats - format else f.formats + format
        f.copy(formats = updated)
    }

    fun clearFilter() = _filter.update { ScheduleFilter() }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val prefs = settingsRepository.userPreferencesFlow.stateIn(viewModelScope).value
            val zoneId = settingsRepository.getEffectiveZoneId(prefs)
            refreshScheduleUseCase(zoneId)
            // A freshly-subscribed Room Flow always queries current DB state on its first
            // emission, so awaiting one here guarantees the write above has landed before we
            // flip isInitialLoad off — otherwise the splash could clear a beat before the
            // long-lived uiState combine (subscribed earlier) re-emits with the fresh rows.
            combine(
                getTodayScheduleUseCase(zoneId),
                getTomorrowScheduleUseCase(zoneId),
                getWeekScheduleUseCase(zoneId)
            ) { today, tomorrow, week -> Triple(today, tomorrow, week) }.first()
            _isLoading.value = false
            _hasLoadedOnce.value = true
            AiringNotificationWorker.runNow(context)
        }
    }

    fun incrementEpisode(malId: Int) {
        if (malId in _pendingIncrementIds.value) return
        _pendingIncrementIds.update { it + malId }
        viewModelScope.launch {
            try {
                val result = incrementEpisodeUseCase(malId)
                _incrementEvent.send(
                    if (result is AppResult.Success) IncrementEvent.Success else IncrementEvent.Error
                )
            } finally {
                _pendingIncrementIds.update { it - malId }
            }
        }
    }

    fun updateEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch {
            val result = updateMalListEntryUseCase(animeId, update)
            if (result is AppResult.Error) {
                _incrementEvent.send(IncrementEvent.Error)
            }
        }
    }
}
