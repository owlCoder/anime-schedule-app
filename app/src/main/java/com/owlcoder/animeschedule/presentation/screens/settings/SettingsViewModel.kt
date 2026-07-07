package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.usecase.GetMalUserListUseCase
import javax.inject.Inject

/** Average anime episode runtime, used to turn a raw episode count into an approximate
 *  watch-time stat — same convention MyAnimeList itself uses for "Days" stats. */
private const val AVG_EPISODE_MINUTES = 24

data class ProfileStats(
    val entryCount: Int = 0,
    val episodesWatched: Int = 0,
    val hoursWatched: Int = 0
)

data class SettingsUiState(
    val timezoneId: String = "",
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val avatarUrl: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val notificationOffsetMinutes: Int = 0,
    val accentColor: AccentColor = AccentColor.TELEGRAM_BLUE,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val profileStats: ProfileStats = ProfileStats()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
    getMalUserListUseCase: GetMalUserListUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userPreferencesFlow,
        authRepository.isLoggedIn,
        authRepository.username,
        authRepository.avatarUrl,
        getMalUserListUseCase()
    ) { prefs, loggedIn, username, avatarUrl, listResult ->
        val entries = (listResult as? AppResult.Success)?.data ?: emptyList()
        val episodesWatched = entries.sumOf { it.episodesWatched }
        SettingsUiState(
            timezoneId = prefs.timezoneId,
            isLoggedIn = loggedIn,
            username = username,
            avatarUrl = avatarUrl,
            themeMode = prefs.themeMode,
            notificationsEnabled = prefs.notificationsEnabled,
            notificationOffsetMinutes = prefs.notificationOffsetMinutes,
            accentColor = prefs.accentColor,
            appLanguage = prefs.appLanguage,
            profileStats = ProfileStats(
                entryCount = entries.size,
                episodesWatched = episodesWatched,
                hoursWatched = (episodesWatched * AVG_EPISODE_MINUTES) / 60
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTimezone(timezoneId: String) {
        viewModelScope.launch { settingsRepository.setTimezoneId(timezoneId) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun setNotificationOffset(minutes: Int) {
        viewModelScope.launch { settingsRepository.setNotificationOffset(minutes) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { settingsRepository.setAccentColor(color) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setAppLanguage(language) }
    }
}
