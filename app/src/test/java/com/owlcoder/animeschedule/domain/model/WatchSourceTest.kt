package com.owlcoder.animeschedule.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class WatchSourceTest {
    @Test
    fun `template needs the placeholder`() {
        assertTrue(isValidWatchSourceTemplate("https://example.com/search?q={query}"))
        assertFalse(isValidWatchSourceTemplate("https://example.com/search"))
    }

    @Test
    fun `template must be an http or https URL with a host`() {
        assertTrue(isValidWatchSourceTemplate("http://example.com/s/{query}"))
        assertFalse(isValidWatchSourceTemplate("javascript:alert({query})"))
        assertFalse(isValidWatchSourceTemplate("file:///sdcard/{query}"))
        assertFalse(isValidWatchSourceTemplate("intent://scan/{query}#Intent;end"))
        assertFalse(isValidWatchSourceTemplate("foo://example.com/{query}"))
        assertFalse(isValidWatchSourceTemplate("https:///{query}"))
        assertFalse(isValidWatchSourceTemplate("not a url {query}"))
    }

    @Test
    fun `web url check accepts only absolute http urls`() {
        assertTrue(isWebUrl("https://www.crunchyroll.com/search?q=one%20piece"))
        assertFalse(isWebUrl("tel:+123"))
        assertFalse(isWebUrl("/relative/path"))
        assertFalse(isWebUrl(""))
    }

    @Test
    fun `build url encodes the title`() {
        val source = WatchSource(name = "S", urlTemplate = "https://e.com/?q={query}", faviconUrl = null)

        assertEquals("https://e.com/?q=Re%3AZero+%26+more", source.buildUrl("Re:Zero & more"))
    }

    @Test
    fun `effective zone falls back to the device zone for blank or invalid ids`() {
        assertEquals(ZoneId.of("Asia/Tokyo"), UserPreferences(timezoneId = "Asia/Tokyo").effectiveZoneId)
        assertEquals(ZoneId.systemDefault(), UserPreferences(timezoneId = "").effectiveZoneId)
        assertEquals(ZoneId.systemDefault(), UserPreferences(timezoneId = "Mars/Olympus").effectiveZoneId)
    }
}
