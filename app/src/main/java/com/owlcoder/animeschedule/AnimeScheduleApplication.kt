package com.owlcoder.animeschedule

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import com.owlcoder.animeschedule.data.work.AiringNotificationWorker
import com.owlcoder.animeschedule.data.work.AnimeScheduleImageLoader
import javax.inject.Inject

@HiltAndroidApp
class AnimeScheduleApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun newImageLoader(context: Context): ImageLoader =
        AnimeScheduleImageLoader.create(context)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        com.owlcoder.animeschedule.data.work.WorkManagerScheduler.schedule(this)
        AiringNotificationWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AiringNotificationWorker.CHANNEL_ID,
                getString(com.owlcoder.animeschedule.R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
