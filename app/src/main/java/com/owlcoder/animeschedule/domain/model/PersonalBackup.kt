package com.owlcoder.animeschedule.domain.model

import java.time.LocalDate
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppearancePreset(
    val name: String,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.TELEGRAM_BLUE,
    val options: ThemeOptions = ThemeOptions(),
)

fun ThemeOptions.normalized(): ThemeOptions = copy(
    darkStartHour = darkStartHour.coerceIn(0, 23),
    darkEndHour = darkEndHour.coerceIn(0, 23),
)

/** Equal start/end means a full day of dark mode; intervals may cross midnight. */
fun ThemeOptions.isDarkAt(hour: Int): Boolean {
    val start = darkStartHour.coerceIn(0, 23)
    val end = darkEndHour.coerceIn(0, 23)
    return when {
        start == end -> true
        start < end -> hour in start until end
        else -> hour >= start || hour < end
    }
}

fun normalizedTags(text: String): Set<String> = text.split(',').map { it.trim().take(24) }
    .filter { it.isNotEmpty() }.distinctBy { it.lowercase(Locale.ROOT) }.take(8).toSet()

fun WatchTools.normalized(): WatchTools = copy(
    favorites = favorites.filter { it > 0 }.take(20_000).toSet(),
    notes = notes.filterKeys { it > 0 }.entries.take(20_000)
        .associate { it.key to it.value.trim().take(2000) }.filterValues { it.isNotEmpty() },
    tags = tags.filterKeys { it > 0 }.entries.take(20_000)
        .associate { it.key to normalizedTags(it.value.joinToString(",")) }.filterValues { it.isNotEmpty() },
    activity = activity.filter { it.animeId > 0 && runCatching { LocalDate.parse(it.date) }.isSuccess }
        .take(300).map { it.copy(title = it.title.take(500), progress = it.progress.coerceIn(0, 99999), episodeDelta = it.episodeDelta.coerceIn(-99999, 99999)) },
    weeklyGoal = weeklyGoal.coerceIn(0, 100),
    episodeMinutes = episodeMinutes.coerceIn(1, 180),
    pinned = pinned.filter { it > 0 }.take(20_000).toSet(),
    dailyGoal = dailyGoal.coerceIn(0, 50),
    durationOverrides = durationOverrides.filterKeys { it > 0 }.entries.take(20_000).associate { it.key to it.value.coerceIn(1, 180) },
    savedViews = savedViews.normalizedViews(),
    mutedNotifications = mutedNotifications.filterKeys { it > 0 }.entries.take(20_000).associate { it.key to it.value.trim().take(500) },
)

/** Portable personal data only: no login tokens, credentials, API keys or MAL list mutations. */
@Serializable
data class PersonalBackup(
    val schemaVersion: Int = 1,
    val tools: WatchTools = WatchTools(),
    val appearance: AppearancePreset = AppearancePreset("Current"),
    val presets: List<AppearancePreset> = emptyList(),
) {
    fun normalized(): PersonalBackup {
        require(schemaVersion == 1) { "Unsupported backup version" }
        return copy(
            tools = tools.normalized(),
            appearance = appearance.copy(options = appearance.options.normalized()),
            presets = presets.take(8).map { it.copy(name = it.name.trim().take(32), options = it.options.normalized()) }
                .filter { it.name.isNotEmpty() }.distinctBy { it.name.lowercase(Locale.ROOT) },
        )
    }
    fun encode(): String = json.encodeToString(normalized())
    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
        fun decode(text: String): PersonalBackup {
            require(text.toByteArray(Charsets.UTF_8).size <= 2_000_000) { "Backup is too large" }
            // Empty or unrelated JSON must never silently replace the user's personal data.
            val root = json.parseToJsonElement(text)
            require(root is kotlinx.serialization.json.JsonObject && "schemaVersion" in root && "tools" in root) { "Invalid backup" }
            return json.decodeFromString<PersonalBackup>(text).normalized()
        }
    }
}
