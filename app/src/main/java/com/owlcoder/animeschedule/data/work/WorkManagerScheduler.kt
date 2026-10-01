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
import com.owlcoder.animeschedule.domain.repository.WorkScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every request here uses a unique work name, so enqueueing is idempotent: calling it on each
 * app start, refresh or edit can never pile up duplicate jobs.
 */
@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : WorkScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    /** Registers the recurring jobs. Safe to call on every launch; existing schedules are kept. */
    fun schedulePeriodicWork() {
        val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScheduleSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(connected)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        )

        // Cache cleanup only touches local Room/Coil data, so it needs no constraints.
        workManager.enqueueUniquePeriodicWork(
            CACHE_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CacheCleanupWorker>(1, TimeUnit.DAYS).build()
        )

        // Reads only the local database; the network is touched solely for the cover image.
        workManager.enqueueUniquePeriodicWork(
            NOTIFICATION_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AiringNotificationWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    override fun scheduleFlushPendingUpdates() {
        val request = OneTimeWorkRequestBuilder<PendingUpdatesWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        // KEEP: a flush already waiting for connectivity will also pick up newly queued rows,
        // so there is no need to reset its backoff.
        workManager.enqueueUniqueWork(PENDING_UPDATES_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    override fun checkAiringNotifications() {
        workManager.enqueueUniqueWork(
            NOTIFICATION_NOW_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AiringNotificationWorker>().build()
        )
    }

    private companion object {
        const val SYNC_WORK_NAME = "schedule_sync"
        const val PENDING_UPDATES_WORK_NAME = "pending_mal_updates"
        const val CACHE_CLEANUP_WORK_NAME = "cache_cleanup"
        const val NOTIFICATION_CHECK_WORK_NAME = "airing_notification_worker"
        const val NOTIFICATION_NOW_WORK_NAME = "airing_notification_now"
    }
}
