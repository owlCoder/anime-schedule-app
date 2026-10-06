package com.owlcoder.animeschedule.domain.model

import java.time.ZoneId

import kotlinx.serialization.Serializable

@Serializable
enum class ThemePalette { CLASSIC, MIDNIGHT, SAKURA, FOREST, SAND }

@Serializable
data class ThemeOptions(
    val palette: ThemePalette = ThemePalette.CLASSIC,
    val dynamicColors: Boolean = false,
    val amoled: Boolean = true,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
)

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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeOptions: ThemeOptions = ThemeOptions(),
    val notificationsEnabled: Boolean = true,
    val notificationOffsetMinutes: Int = 0,
    val accentColor: AccentColor = AccentColor.TELEGRAM_BLUE,
    val onboardingDone: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    /** How long temporary Room/image cache data should be retained. */
    val cacheRetentionDays: Int = CacheRetentionPolicy.DEFAULT_RETENTION_DAYS
)

/**
 * The zone all schedule days and times are shown in: the user's override when it is a valid
 * zone id, otherwise the device zone.
 */
val UserPreferences.effectiveZoneId: ZoneId
    get() = timezoneId.takeIf { it.isNotEmpty() }
        ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
        ?: ZoneId.systemDefault()
