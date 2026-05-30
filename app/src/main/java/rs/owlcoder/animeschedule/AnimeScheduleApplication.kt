package rs.owlcoder.animeschedule

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import rs.owlcoder.animeschedule.data.work.AiringNotificationWorker
import javax.inject.Inject

@HiltAndroidApp
class AnimeScheduleApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        rs.owlcoder.animeschedule.data.work.WorkManagerScheduler.schedule(this)
        AiringNotificationWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AiringNotificationWorker.CHANNEL_ID,
                getString(rs.owlcoder.animeschedule.R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
