package rs.owlcoder.animeschedule.data.local.datastore

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val timezoneId: String = "",
    val malLoggedIn: Boolean = false,
    val malUsername: String = "",
    val lastScheduleSyncEpoch: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true
)
