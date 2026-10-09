package com.owlcoder.animeschedule.domain.model

import java.time.ZoneId

import kotlinx.serialization.Serializable

@Serializable
enum class ThemePalette { CLASSIC, MIDNIGHT, SAKURA, FOREST, SAND, OCEAN, LAVENDER, EMBER, ICE, COFFEE, NEON, RUBY, AMETHYST, MINT, GOLD }

@Serializable
data class ThemeOptions(
    val palette: ThemePalette = ThemePalette.CLASSIC,
    val amoled: Boolean = true,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val scheduled: Boolean = false,
    val darkStartHour: Int = 22,
    val darkEndHour: Int = 7,
    val compactLayout: Boolean = false,
)

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
    val quietHours: QuietHours = QuietHours(),
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

/** Translate old Classic accents once; an explicit palette always wins. */
fun ThemeOptions.withLegacyAccent(accent: String?): ThemeOptions = if (palette != ThemePalette.CLASSIC) this else copy(
    palette = when (accent) {
        "PURPLE" -> ThemePalette.AMETHYST
        "GREEN" -> ThemePalette.FOREST
        "ORANGE" -> ThemePalette.EMBER
        "PINK" -> ThemePalette.SAKURA
        "RED" -> ThemePalette.RUBY
        "CYAN" -> ThemePalette.OCEAN
        "INDIGO" -> ThemePalette.MIDNIGHT
        "TEAL" -> ThemePalette.MINT
        "YELLOW" -> ThemePalette.GOLD
        "DEEP_PURPLE" -> ThemePalette.LAVENDER
        else -> ThemePalette.CLASSIC
    },
)
