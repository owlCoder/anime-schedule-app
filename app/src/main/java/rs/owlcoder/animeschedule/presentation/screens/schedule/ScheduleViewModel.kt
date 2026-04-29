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
import rs.owlcoder.animeschedule.domain.usecase.GetTodayScheduleUseCase
import rs.owlcoder.animeschedule.domain.usecase.GetTomorrowScheduleUseCase
import rs.owlcoder.animeschedule.domain.usecase.GetWeekScheduleUseCase
import rs.owlcoder.animeschedule.domain.usecase.IncrementEpisodeUseCase
import rs.owlcoder.animeschedule.domain.usecase.RefreshScheduleUseCase
import javax.inject.Inject

enum class ScheduleTab(val label: String) { TODAY("Danas"), TOMORROW("Sutra"), WEEK("Ova nedelja") }

data class ScheduleUiState(
    val todayEpisodes: List<AiringEpisode> = emptyList(),
    val tomorrowEpisodes: List<AiringEpisode> = emptyList(),
    val weekDays: List<ScheduleDay> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val selectedTab: ScheduleTab = ScheduleTab.TODAY
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getTodayScheduleUseCase: GetTodayScheduleUseCase,
    private val getTomorrowScheduleUseCase: GetTomorrowScheduleUseCase,
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val refreshScheduleUseCase: RefreshScheduleUseCase,
    private val incrementEpisodeUseCase: IncrementEpisodeUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ScheduleTab.TODAY)
    private val _isLoading = MutableStateFlow(true)

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
                    selectedTab = tab
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    init { refresh() }

    fun selectTab(tab: ScheduleTab) = _selectedTab.update { tab }

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
}
