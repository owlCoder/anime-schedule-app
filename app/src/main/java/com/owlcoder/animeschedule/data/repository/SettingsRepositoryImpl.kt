package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import com.owlcoder.animeschedule.domain.model.AccentColor
import com.owlcoder.animeschedule.domain.model.AppLanguage
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.domain.model.UserPreferences
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefsDataStore: UserPreferencesDataStore
) : SettingsRepository {

    override val userPreferencesFlow: Flow<UserPreferences> = prefsDataStore.userPreferencesFlow

    override suspend fun setTimezoneId(timezoneId: String) {
        prefsDataStore.setTimezoneId(timezoneId)
    }

    override suspend fun setThemeOptions(options: com.owlcoder.animeschedule.domain.model.ThemeOptions) {
        prefsDataStore.setThemeOptions(options)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefsDataStore.setThemeMode(mode)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefsDataStore.setNotificationsEnabled(enabled)
    }

    override suspend fun setNotificationOffset(minutes: Int) {
        prefsDataStore.setNotificationOffset(minutes)
    }

    override suspend fun setAccentColor(color: AccentColor) {
        prefsDataStore.setAccentColor(color)
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        prefsDataStore.setAppLanguage(language)
    }

    override suspend fun setCacheRetentionDays(days: Int) {
        prefsDataStore.setCacheRetentionDays(days)
    }
}
