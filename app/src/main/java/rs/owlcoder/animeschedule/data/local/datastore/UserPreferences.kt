package rs.owlcoder.animeschedule.data.local.datastore

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentColor {
    TELEGRAM_BLUE,
    PURPLE,
    GREEN,
    ORANGE,
    PINK,
    RED,
    CYAN,
    INDIGO,
    TEAL,
    YELLOW,
    DEEP_PURPLE
}

data class UserPreferences(
    val timezoneId: String = "",
    val malLoggedIn: Boolean = false,
    val malUsername: String = "",
    val malAvatarUrl: String = "",
    val lastScheduleSyncEpoch: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val notificationOffsetMinutes: Int = 0,
    val accentColor: AccentColor = AccentColor.TELEGRAM_BLUE,
    val onboardingDone: Boolean = false
)
