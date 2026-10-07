package com.owlcoder.animeschedule

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.owlcoder.animeschedule.data.work.AiringNotificationWorker
import com.owlcoder.animeschedule.data.work.AnimeScheduleImageLoader
import com.owlcoder.animeschedule.data.work.WorkManagerScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class AnimeScheduleApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workScheduler: WorkManagerScheduler
    @Inject lateinit var pendingUpdates: com.owlcoder.animeschedule.data.local.db.PendingListUpdateDao
    @Inject lateinit var notificationActions: com.owlcoder.animeschedule.data.work.NotificationActions

    // Lives exactly as long as the process, so it is never cancelled.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun newImageLoader(context: Context): ImageLoader =
        AnimeScheduleImageLoader.create(context)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Opening WorkManager's own database is real I/O; keep it off the startup path.
        applicationScope.launch {
            workScheduler.schedulePeriodicWork()
            // Repair a process death between the local transaction and WorkManager enqueue.
            if (pendingUpdates.getAll().isNotEmpty()) workScheduler.scheduleFlushPendingUpdates()
            notificationActions.recoverSnoozes()
        }
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            AiringNotificationWorker.CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
