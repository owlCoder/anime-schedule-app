package com.owlcoder.animeschedule.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.CacheRetentionPolicy
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.data.work.CacheMaintenance
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.usecase.GetMalUserListUseCase
import javax.inject.Inject

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
    val notificationsEnabled: Boolean = true,
    val notificationOffsetMinutes: Int = 0,
    val accentColor: AccentColor = AccentColor.TELEGRAM_BLUE,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val cacheRetentionDays: Int = CacheRetentionPolicy.DEFAULT_RETENTION_DAYS,
    val profileStats: ProfileStats = ProfileStats(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
    getMalUserListUseCase: GetMalUserListUseCase,
    private val cacheMaintenance: CacheMaintenance,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache

    private val _cacheActionMessage = MutableStateFlow<Int?>(null)
    val cacheActionMessage: StateFlow<Int?> = _cacheActionMessage

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userPreferencesFlow,
        authRepository.isLoggedIn,
        authRepository.username,
        authRepository.avatarUrl,
        getMalUserListUseCase(),
    ) { preferences, loggedIn, username, avatarUrl, listResult ->
        val entries = (listResult as? AppResult.Success)?.data ?: emptyList()
        val episodesWatched = entries.sumOf { it.episodesWatched }
        SettingsUiState(
            timezoneId = preferences.timezoneId,
            isLoggedIn = loggedIn,
            username = username,
            avatarUrl = avatarUrl,
            themeMode = preferences.themeMode,
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
            _cacheActionMessage.value = null
            runCatching {
                cacheMaintenance.run(clearImageCacheNow = true)
                refreshCacheSizeAndWait()
            }.onSuccess {
                _cacheActionMessage.value = R.string.settings_cache_cleared
            }.onFailure {
                _cacheActionMessage.value = R.string.settings_cache_clear_failed
            }
            _isClearingCache.value = false
        }
    }

    fun clearCacheActionMessage() {
        _cacheActionMessage.value = null
    }

    private fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = withContext(Dispatchers.IO) { calculateCacheSize() }
        }
    }

    private suspend fun refreshCacheSizeAndWait() {
        _cacheSizeBytes.value = withContext(Dispatchers.IO) { calculateCacheSize() }
    }

    private fun calculateCacheSize(): Long = context.cacheDir
        .walkTopDown()
        .filter { it.isFile }
        .sumOf { it.length() }
}
