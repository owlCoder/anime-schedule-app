package com.owlcoder.animeschedule.domain.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PersonalBackupTest {
    @Test fun `night interval includes start and excludes end across midnight`() {
        val options = ThemeOptions(darkStartHour = 22, darkEndHour = 7)
        assertTrue(options.isDarkAt(22)); assertTrue(options.isDarkAt(0)); assertTrue(options.isDarkAt(6))
        assertFalse(options.isDarkAt(7)); assertFalse(options.isDarkAt(21))
    }
    @Test fun `day interval and equal hours are deterministic`() {
        val options = ThemeOptions(darkStartHour = 8, darkEndHour = 17)
        assertTrue(options.isDarkAt(8)); assertFalse(options.isDarkAt(17)); assertFalse(options.isDarkAt(0))
        for (hour in 0..23) assertTrue(options.copy(darkStartHour = 17).isDarkAt(hour))
    }
    @Test fun `theme schedule hours are bounded on restore`() {
        val options = ThemeOptions(darkStartHour = 90, darkEndHour = -2).normalized()
        assertEquals(23, options.darkStartHour); assertEquals(0, options.darkEndHour)
    }
    @Test fun `tags deduplicate without losing case and enforce bounded size`() {
        assertEquals(setOf("Akcija", "Drama"), normalizedTags(" Akcija, akcija, ,Drama, drama "))
        assertEquals(8, normalizedTags((1..15).joinToString(",")).size)
        assertEquals(24, normalizedTags("x".repeat(100)).single().length)
    }
    @Test fun `personal backup round trip preserves Unicode tags activity and current appearance`() {
        val backup = PersonalBackup(tools = WatchTools(favorites = setOf(5), notes = mapOf(5 to "Završna scena"), tags = mapOf(5 to setOf("Akcija")), episodeMinutes = 45, activity = listOf(WatchActivity(5, "Šuma", "2026-10-06", 2, 7))), appearance = AppearanceSettings(ThemeMode.DARK, options = ThemeOptions(palette = ThemePalette.OCEAN, compactLayout = true, scheduled = true)))
        assertEquals(backup, PersonalBackup.decode(backup.encode()))
        assertFalse(backup.encode().contains("token")); assertFalse(backup.encode().contains("username"))
    }
    @Test fun `unrelated JSON and unsupported versions cannot clear personal data`() {
        for (bad in listOf("{}", "[]", "{\"tools\":{}}", "{\"schemaVersion\":3,\"tools\":{}}", "{\"schemaVersion\":1,\"tools\":{},\"appearance\":[]}", "not json")) {
            assertTrue(bad, runCatching { PersonalBackup.decode(bad) }.isFailure)
        }
    }
    @Test fun `backup limit counts UTF8 bytes and tolerates future optional fields`() {
        assertTrue(runCatching { PersonalBackup.decode("č".repeat(1_000_001)) }.isFailure)
        assertEquals(WatchTools(), PersonalBackup.decode("{\"schemaVersion\":1,\"tools\":{},\"futureField\":true}").tools)
    }
    @Test fun `restored values are bounded and invalid history rows are removed`() {
        val value = PersonalBackup(tools = WatchTools(favorites = setOf(-1, 2), notes = mapOf(2 to " x ", -1 to "bad"), tags = mapOf(2 to setOf("A", "a")), weeklyGoal = 900, episodeMinutes = 0, activity = listOf(WatchActivity(2, "A", "bad-date", 2, 2)))).normalized()
        assertEquals(setOf(2), value.tools.favorites); assertEquals(mapOf(2 to "x"), value.tools.notes)
        assertEquals(setOf("A"), value.tools.tags[2]); assertEquals(100, value.tools.weeklyGoal); assertEquals(1, value.tools.episodeMinutes)
        assertTrue(value.tools.activity.isEmpty())
    }
    @Test fun `history search combines case insensitive title and Monday based week`() {
        val rows = listOf(WatchActivity(1,"Alpha", "2026-10-05",1,1), WatchActivity(2,"ALPHA sequel", "2026-10-04",1,1), WatchActivity(3,"Beta", "2026-10-06",1,1), WatchActivity(4,"Alpha future", "2026-10-07",1,1))
        val tools = WatchTools(activity = rows)
        assertEquals(listOf(1), tools.filteredActivity(" alpha ", true, LocalDate.of(2026,10,6)).map { it.animeId })
        assertEquals(listOf(1,2,4), tools.filteredActivity("alpha", false, LocalDate.of(2026,10,6)).map { it.animeId })
        assertEquals(listOf(1,3), tools.filteredActivity("", true, LocalDate.of(2026,10,6)).map { it.animeId })
    }
}
