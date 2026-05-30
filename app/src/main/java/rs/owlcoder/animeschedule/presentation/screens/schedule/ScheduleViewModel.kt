package rs.owlcoder.animeschedule.presentation.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.owlcoder.animeschedule.core.result.AppResult
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

data class ScheduleUiState(
    val todayEpisodes: List<AiringEpisode> = emptyList(),
    val tomorrowEpisodes: List<AiringEpisode> = emptyList(),
    val weekDays: List<ScheduleDay> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val selectedTab: ScheduleTab = ScheduleTab.TODAY,
    val selectedWeekDay: LocalDate = LocalDate.now()
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
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
                ScheduleUiState(
                    todayEpisodes = (today as? AppResult.Success)?.data ?: emptyList(),
                    tomorrowEpisodes = (tomorrow as? AppResult.Success)?.data ?: emptyList(),
                    weekDays = (week as? AppResult.Success)?.data ?: emptyList(),
                    isLoading = loading,
                    error = if (today is AppResult.Error) "Greška pri učitavanju rasporeda" else null,
                    isLoggedIn = prefs.malLoggedIn,
                    selectedTab = tab,
                    selectedWeekDay = _selectedWeekDay.value
                )
            }.combine(_selectedWeekDay) { state, day -> state.copy(selectedWeekDay = day) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    init { refresh() }

    fun selectTab(tab: ScheduleTab) = _selectedTab.update { tab }

    fun selectWeekDay(date: LocalDate) = _selectedWeekDay.update { date }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val prefs = settingsRepository.userPreferencesFlow.stateIn(viewModelScope).value
            refreshScheduleUseCase(settingsRepository.getEffectiveZoneId(prefs))
            _isLoading.value = false
        }
    }

    fun incrementEpisode(animeId: Int) {
        viewModelScope.launch { incrementEpisodeUseCase(animeId) }
    }

    fun updateEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch { updateMalListEntryUseCase(animeId, update) }
    }
}
