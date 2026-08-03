package com.owlcoder.animeschedule.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {
    private const val SYNC_WORK_NAME = "schedule_sync"
    private const val PENDING_UPDATES_WORK_NAME = "pending_mal_updates"
    private const val CACHE_CLEANUP_WORK_NAME = "cache_cleanup"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ScheduleSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        // Cache cleanup is deliberately unconstrained: it only touches local Room/Coil data.
        val cacheCleanupRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CACHE_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cacheCleanupRequest
        )
    }

    fun scheduleFlushPendingUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<PendingUpdatesWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            PENDING_UPDATES_WORK_NAME,
            // KEEP: if a flush is already waiting for connectivity it will pick up the newly
            // queued rows too — no need to reset its backoff.
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
