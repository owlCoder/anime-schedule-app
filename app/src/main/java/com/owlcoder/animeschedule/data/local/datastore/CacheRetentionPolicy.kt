package com.owlcoder.animeschedule.data.local.datastore

/** Shared retention rules for temporary local cache data. */
object CacheRetentionPolicy {
    const val DEFAULT_RETENTION_DAYS = 10
    const val READ_NOTIFICATION_RETENTION_DAYS = 30
    const val UNREAD_NOTIFICATION_RETENTION_DAYS = 90
    const val SECONDS_PER_DAY = 24L * 60L * 60L

    val supportedRetentionDays: List<Int> = listOf(7, 10, 14, 30)

    fun normalizeRetentionDays(value: Int): Int =
        value.takeIf { it in supportedRetentionDays } ?: DEFAULT_RETENTION_DAYS

    fun cutoffEpochSeconds(nowEpochSeconds: Long, retentionDays: Int): Long =
        nowEpochSeconds - normalizeRetentionDays(retentionDays) * SECONDS_PER_DAY
}
