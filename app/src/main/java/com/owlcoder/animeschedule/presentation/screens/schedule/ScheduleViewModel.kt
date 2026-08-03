package com.owlcoder.animeschedule.presentation.screens.schedule

import android.content.Context
import androidx.annotation.StringRes
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
import kotlinx.coroutines.withTimeoutOrNull
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.data.work.AiringNotificationWorker
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.model.MalListUpdate
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

/** Parses MAL's ISO-8601 updated_at (offset form, e.g. "2026-07-07T10:15:00+00:00") or our own
 *  locally-stamped Instant.toString() form — returns null if unparseable/absent rather than
 *  throwing, since this only drives a "recently changed" sort, not anything critical. */
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
    val formats: Set<String> = emptySet()
) {
    val isActive: Boolean get() = onlyMyList || genres.isNotEmpty() || formats.isNotEmpty()
}

/** Which schedule overlay (bottom sheet) is currently open, if any — kept in the ViewModel
 *  (survives navigating to Detail and back) instead of `remember` in the Composable, which
 *  gets torn down and reset to "closed" whenever ScheduleScreen leaves composition. */
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
    /** True only while the very first load is in flight (drives the animated splash);
     *  a pull-to-refresh sets [isLoading] but NOT this, so it shows the refresh spinner
     *  over existing content instead of the full-screen splash. */
    val isInitialLoad: Boolean = true,
    @StringRes val errorRes: Int? = null,
    val isLoggedIn: Boolean = false,
    val filter: ScheduleFilter = ScheduleFilter(),
    val availableGenres: List<String> = emptyList(),
    val availableFormats: List<String> = emptyList(),
    val pendingIncrementIds: Set<Int> = emptySet(),
    val unreadNotificationCount: Int = 0,
    /** Watching-status MAL entries, most-recently-updated first — surfaces "you were just
     *  watching this" titles on the Schedule home even when they're not airing today/tomorrow. */
    val recentlyChangedEntries: List<MalListEntry> = emptyList()
)

/** Dropped titles shouldn't clutter Today/Tomorrow/This week, and "+1" makes no sense on
 *  an entry the user already dropped — so these sections exclude them entirely rather than
 *  just disabling the increment action. */
private fun List<AiringEpisode>.excludeDropped(): List<AiringEpisode> =
    filter { it.malListEntry?.status != WatchStatus.DROPPED }

private fun List<ScheduleDay>.excludeDroppedFromWeek(): List<ScheduleDay> =
    map { day -> day.copy(episodes = day.episodes.excludeDropped()) }

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
    private val removeMalListEntryUseCase: RemoveMalListEntryUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val getMalUserListUseCase: GetMalUserListUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    // Flips to true after the first refresh completes; stays true forever after, so subsequent
    // refreshes (pull-to-refresh) never re-show the full-screen animated splash.
    private val _hasLoadedOnce = MutableStateFlow(false)
    private val _filter = MutableStateFlow(ScheduleFilter())
    private val _pendingIncrementIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _openOverlay = MutableStateFlow<ScheduleOverlay>(ScheduleOverlay.None)
    val openOverlay: StateFlow<ScheduleOverlay> = _openOverlay

    sealed interface IncrementEvent {
        data object Success : IncrementEvent
        /** A status-sheet save (not a "+1") went through — toasts "saved" instead of
         *  "episode marked" and skips the finale-prompt logic tied to [Success]. */
        data object Updated : IncrementEvent
        data object Removed : IncrementEvent
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
            .combine(getMalUserListUseCase()) { state, listResult ->
                val entries = (listResult as? AppResult.Success)?.data ?: emptyList()
                val recentlyChanged = entries
                    .filter { it.status == WatchStatus.WATCHING }
                    .sortedByDescending { it.updatedAt.toInstantOrNull() ?: Instant.EPOCH }
                    .take(10)
                state.copy(recentlyChangedEntries = recentlyChanged)
            }
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

    fun setOpenOverlay(overlay: ScheduleOverlay) { _openOverlay.value = overlay }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val prefs = settingsRepository.userPreferencesFlow.stateIn(viewModelScope).value
                val zoneId = settingsRepository.getEffectiveZoneId(prefs)
                // The repository has its own provider fallbacks. This outer guard covers DNS,
                // a wedged HTTP stack, or a provider interceptor that never returns.
                withTimeoutOrNull(SCHEDULE_REFRESH_TIMEOUT_MS) {
                    refreshScheduleUseCase(zoneId)
                }
                // Room emits the current cache immediately. We only need to wait for that
                // first snapshot; a failed refresh must not keep the animated splash forever.
                combine(
                    getTodayScheduleUseCase(zoneId),
                    getTomorrowScheduleUseCase(zoneId),
                    getWeekScheduleUseCase(zoneId)
                ) { today, tomorrow, week -> Triple(today, tomorrow, week) }.first()
            } finally {
                _isLoading.value = false
                _hasLoadedOnce.value = true
                runCatching { AiringNotificationWorker.runNow(context) }
            }
        }
    }

    private companion object {
        const val SCHEDULE_REFRESH_TIMEOUT_MS = 12_000L
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
            _incrementEvent.send(
                if (result is AppResult.Success) IncrementEvent.Updated else IncrementEvent.Error
            )
        }
    }

    fun removeEntry(animeId: Int) {
        viewModelScope.launch {
            val result = removeMalListEntryUseCase(animeId)
            _incrementEvent.send(
                if (result is AppResult.Success) IncrementEvent.Removed else IncrementEvent.Error
            )
        }
    }
}
