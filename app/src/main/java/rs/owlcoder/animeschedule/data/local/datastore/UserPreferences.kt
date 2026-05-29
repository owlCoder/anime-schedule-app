package rs.owlcoder.animeschedule.data.local.datastore

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentColor {
    TELEGRAM_BLUE,
    PURPLE,
    GREEN,
    ORANGE,
    PINK,
    RED
}

data class UserPreferences(
    val timezoneId: String = "",
    val malLoggedIn: Boolean = false,
    val malUsername: String = "",
    val malAvatarUrl: String = "",
    val lastScheduleSyncEpoch: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val accentColor: AccentColor = AccentColor.TELEGRAM_BLUE
)
