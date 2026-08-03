package com.owlcoder.animeschedule.data.local.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheRetentionPolicyTest {
    @Test
    fun unsupportedRetentionFallsBackToTenDays() {
        assertEquals(10, CacheRetentionPolicy.normalizeRetentionDays(3))
        assertEquals(10, CacheRetentionPolicy.normalizeRetentionDays(0))
        assertEquals(10, CacheRetentionPolicy.normalizeRetentionDays(10))
    }

    @Test
    fun supportedRetentionValuesProduceExpectedCutoff() {
        val now = 1_000_000L

        assertEquals(
            now - 7L * CacheRetentionPolicy.SECONDS_PER_DAY,
            CacheRetentionPolicy.cutoffEpochSeconds(now, 7)
        )
        assertEquals(
            now - 30L * CacheRetentionPolicy.SECONDS_PER_DAY,
            CacheRetentionPolicy.cutoffEpochSeconds(now, 30)
        )
    }
}
