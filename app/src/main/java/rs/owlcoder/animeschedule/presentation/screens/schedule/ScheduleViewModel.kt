package rs.owlcoder.animeschedule.presentation.screens.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.work.AiringNotificationWorker
import rs.owlcoder.animeschedule.domain.model.AiringEpisode
import rs.owlcoder.animeschedule.domain.model.ScheduleDay
import rs.owlcoder.animeschedule.domain.repository.SettingsRepository
import rs.owlcoder.animeschedule.domain.model.MalListUpdate
import rs.owlcoder.animeschedule.domain.usecase.GetTodayScheduleUseCase
import rs.owlcoder.animeschedule.domain.usecase.GetTomorrowScheduleUseCase
import rs.owlcoder.animeschedule.domain.usecase.GetWeekScheduleUseCase
import rs.owlcoder.animeschedule.domain.usecase.IncrementEpisodeUseCase
import rs.owlcoder.animeschedule.domain.usecase.RefreshScheduleUseCase
import rs.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import java.time.LocalDate
import javax.inject.Inject

enum class ScheduleTab { TODAY, TOMORROW, WEEK }

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
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val selectedTab: ScheduleTab = ScheduleTab.TODAY,
    val selectedWeekDay: LocalDate = LocalDate.now(),
    val filter: ScheduleFilter = ScheduleFilter(),
    val availableGenres: List<String> = emptyList(),
    val availableFormats: List<String> = emptyList()
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ScheduleTab.TODAY)
    private val _isLoading = MutableStateFlow(true)
    private val _selectedWeekDay = MutableStateFlow(LocalDate.now())
    private val _filter = MutableStateFlow(ScheduleFilter())

    val uiState: StateFlow<ScheduleUiState> = settingsRepository.userPreferencesFlow
        .flatMapLatest { prefs ->
            val zoneId = settingsRepository.getEffectiveZoneId(prefs)
            combine(
                getTodayScheduleUseCase(zoneId),
                getTomorrowScheduleUseCase(zoneId),
                getWeekScheduleUseCase(zoneId),
                _selectedTab,
                _isLoading
            ) { today, tomorrow, week, tab, loading ->
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
                    selectedTab = tab,
                    selectedWeekDay = _selectedWeekDay.value,
                    availableGenres = genres,
                    availableFormats = formats
                )
            }
            .combine(_selectedWeekDay) { state, day -> state.copy(selectedWeekDay = day) }
            .combine(_filter) { state, f ->
                state.copy(
                    filter = f,
                    todayEpisodes = state.todayEpisodes.applyFilter(f),
                    tomorrowEpisodes = state.tomorrowEpisodes.applyFilter(f),
                    weekDays = state.weekDays.applyFilterToWeek(f)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    init { refresh() }

    fun selectTab(tab: ScheduleTab) = _selectedTab.update { tab }

    fun selectWeekDay(date: LocalDate) = _selectedWeekDay.update { date }

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
            refreshScheduleUseCase(settingsRepository.getEffectiveZoneId(prefs))
            _isLoading.value = false
            AiringNotificationWorker.runNow(context)
        }
    }

    fun incrementEpisode(animeId: Int) {
        viewModelScope.launch { incrementEpisodeUseCase(animeId) }
    }

    fun updateEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch { updateMalListEntryUseCase(animeId, update) }
    }
}
