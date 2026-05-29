package rs.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import rs.owlcoder.animeschedule.data.local.datastore.AccentColor
import rs.owlcoder.animeschedule.data.local.datastore.ThemeMode
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferences
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import rs.owlcoder.animeschedule.domain.repository.SettingsRepository
import java.time.ZoneId
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

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefsDataStore.setThemeMode(mode)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefsDataStore.setNotificationsEnabled(enabled)
    }

    override suspend fun setAccentColor(color: AccentColor) {
        prefsDataStore.setAccentColor(color)
    }

    override fun getEffectiveZoneId(prefs: UserPreferences): ZoneId =
        if (prefs.timezoneId.isNotEmpty()) {
            runCatching { ZoneId.of(prefs.timezoneId) }.getOrElse { ZoneId.systemDefault() }
        } else {
            ZoneId.systemDefault()
        }
}
