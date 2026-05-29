package rs.owlcoder.animeschedule

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import rs.owlcoder.animeschedule.data.work.AiringNotificationWorker
import javax.inject.Inject

@HiltAndroidApp
class AnimeScheduleApplication : Application() {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager manually with HiltWorkerFactory AFTER Hilt has injected
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        )
        createNotificationChannels()
        rs.owlcoder.animeschedule.data.work.WorkManagerScheduler.schedule(this)
        AiringNotificationWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AiringNotificationWorker.CHANNEL_ID,
                "Nove epizode",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
