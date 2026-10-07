package com.owlcoder.animeschedule.presentation.screens.settings

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AccentColor
import com.owlcoder.animeschedule.domain.model.AppLanguage
import com.owlcoder.animeschedule.domain.model.CacheRetentionPolicy
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.domain.model.AppearancePreset
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.datastore.PersonalBackupStore
import com.owlcoder.animeschedule.data.work.CacheMaintenance
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.repository.MalRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val AVG_EPISODE_MINUTES = 24

data class ProfileStats(
    val entryCount: Int = 0,
    val episodesWatched: Int = 0,
    val hoursWatched: Int = 0,
)

data class SettingsUiState(
    val timezoneId: String = "",
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val avatarUrl: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appearancePresets: List<AppearancePreset> = emptyList(),
    val themeOptions: com.owlcoder.animeschedule.domain.model.ThemeOptions = com.owlcoder.animeschedule.domain.model.ThemeOptions(),
    val notificationsEnabled: Boolean = true,
    val notificationOffsetMinutes: Int = 0,
    val accentColor: AccentColor = AccentColor.TELEGRAM_BLUE,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val cacheRetentionDays: Int = CacheRetentionPolicy.DEFAULT_RETENTION_DAYS,
    val profileStats: ProfileStats = ProfileStats(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
    malRepository: MalRepository,
    private val cacheMaintenance: CacheMaintenance,
    val personalBackupStore: PersonalBackupStore,
    private val preferencesStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache

    private val _cacheActionMessageRes = MutableStateFlow<Int?>(null)

    /** String resource describing the outcome of the last "clear cache" action, if any. */
    @get:StringRes
    val cacheActionMessageRes: StateFlow<Int?> = _cacheActionMessageRes

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userPreferencesFlow,
        authRepository.isLoggedIn,
        authRepository.username,
        authRepository.avatarUrl,
        malRepository.getUserList(),
    ) { preferences, loggedIn, username, avatarUrl, entries ->
        val episodesWatched = entries.sumOf { it.episodesWatched }
        SettingsUiState(
            timezoneId = preferences.timezoneId,
            isLoggedIn = loggedIn,
            username = username,
            avatarUrl = avatarUrl,
            themeMode = preferences.themeMode,
            themeOptions = preferences.themeOptions,
            appearancePresets = preferences.appearancePresets,
            notificationsEnabled = preferences.notificationsEnabled,
            notificationOffsetMinutes = preferences.notificationOffsetMinutes,
            accentColor = preferences.accentColor,
            appLanguage = preferences.appLanguage,
            cacheRetentionDays = preferences.cacheRetentionDays,
            profileStats = ProfileStats(
                entryCount = entries.size,
                episodesWatched = episodesWatched,
                hoursWatched = episodesWatched * AVG_EPISODE_MINUTES / 60,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshCacheSize()
    }

    fun setTimezone(timezoneId: String) {
        viewModelScope.launch { settingsRepository.setTimezoneId(timezoneId) }
    }

    fun saveAppearancePreset(preset: AppearancePreset) { viewModelScope.launch { preferencesStore.saveAppearancePreset(preset) } }
    fun applyAppearancePreset(preset: AppearancePreset) { viewModelScope.launch { preferencesStore.applyAppearancePreset(preset) } }
    fun deleteAppearancePreset(name: String) { viewModelScope.launch { preferencesStore.deleteAppearancePreset(name) } }

    fun setThemeOptions(options: com.owlcoder.animeschedule.domain.model.ThemeOptions) {
        viewModelScope.launch { settingsRepository.setThemeOptions(options) }
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

    fun setCacheRetentionDays(days: Int) {
        viewModelScope.launch { settingsRepository.setCacheRetentionDays(days) }
    }

    fun clearCacheNow() {
        if (_isClearingCache.value) return
        viewModelScope.launch {
            _isClearingCache.value = true
            _cacheActionMessageRes.value = null
            _cacheActionMessageRes.value = try {
                cacheMaintenance.run(clearImageCacheNow = true)
                _cacheSizeBytes.value = cacheMaintenance.cacheSizeBytes()
                R.string.settings_cache_cleared
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.w(TAG, "Clearing the cache failed", e)
                R.string.settings_cache_clear_failed
            } finally {
                _isClearingCache.value = false
            }
        }
    }

    private fun refreshCacheSize() {
        viewModelScope.launch { _cacheSizeBytes.value = cacheMaintenance.cacheSizeBytes() }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
