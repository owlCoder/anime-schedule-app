package rs.owlcoder.animeschedule.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.owlcoder.animeschedule.data.local.datastore.AccentColor
import rs.owlcoder.animeschedule.data.local.datastore.ThemeMode
import rs.owlcoder.animeschedule.domain.repository.AuthRepository
import rs.owlcoder.animeschedule.domain.repository.SettingsRepository
import javax.inject.Inject

data class SettingsUiState(
    val timezoneId: String = "",
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val avatarUrl: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val accentColor: AccentColor = AccentColor.TELEGRAM_BLUE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userPreferencesFlow,
        authRepository.isLoggedIn,
        authRepository.username,
        authRepository.avatarUrl
    ) { prefs, loggedIn, username, avatarUrl ->
        SettingsUiState(
            timezoneId = prefs.timezoneId,
            isLoggedIn = loggedIn,
            username = username,
            avatarUrl = avatarUrl,
            themeMode = prefs.themeMode,
            notificationsEnabled = prefs.notificationsEnabled,
            accentColor = prefs.accentColor
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

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { settingsRepository.setAccentColor(color) }
    }
}
