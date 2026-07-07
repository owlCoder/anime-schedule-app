package com.owlcoder.animeschedule.data.local.datastore

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

enum class AppLanguage { SYSTEM, ENGLISH, SERBIAN_LATIN }

data class UserPreferences(
    val timezoneId: String = "",
    val malLoggedIn: Boolean = false,
    val malUsername: String = "",
    val malAvatarUrl: String = "",
    val lastScheduleSyncEpoch: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val notificationsEnabled: Boolean = true,
    val notificationOffsetMinutes: Int = 0,
    val accentColor: AccentColor = AccentColor.TELEGRAM_BLUE,
    val onboardingDone: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM
)
