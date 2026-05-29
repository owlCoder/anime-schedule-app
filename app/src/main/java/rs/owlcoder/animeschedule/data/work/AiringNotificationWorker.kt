package rs.owlcoder.animeschedule.data.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import rs.owlcoder.animeschedule.MainActivity
import rs.owlcoder.animeschedule.R
import rs.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import rs.owlcoder.animeschedule.data.local.db.MalListEntryDao
import rs.owlcoder.animeschedule.data.local.db.NotificationDao
import rs.owlcoder.animeschedule.data.local.db.NotificationEntity
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import java.util.concurrent.TimeUnit

@HiltWorker
class AiringNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationDao: NotificationDao,
    private val airingEpisodeDao: AiringEpisodeDao,
    private val malListEntryDao: MalListEntryDao,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = userPreferencesDataStore.userPreferencesFlow.first()
            if (!prefs.notificationsEnabled) return Result.success()

            val now = System.currentTimeMillis() / 1000L
            val windowStart = now - 900L // 15 minutes ago

            val recentEpisodes = airingEpisodeDao
                .getAiringEpisodesInRange(windowStart, now)
                .first()

            if (recentEpisodes.isEmpty()) return Result.success()

            val malEntries = malListEntryDao.getAll().first()
            val malIds = malEntries.map { it.malId }.toSet()

            val existingIds = notificationDao.getAllIds().toSet()

            for (episode in recentEpisodes) {
                val episodeMalId = episode.malId ?: continue
                if (episodeMalId !in malIds) continue
                if (episode.airingId in existingIds) continue

                notificationDao.upsert(
                    NotificationEntity(
                        id = episode.airingId,
                        animeId = episode.animeId,
                        title = episode.title,
                        episode = episode.episode,
                        coverImageUrl = episode.coverImageUrl,
                        airingAtEpochSeconds = episode.airingAtEpochSeconds,
                        isRead = false,
                        createdAtEpochSeconds = now
                    )
                )

                sendSystemNotification(
                    id = episode.airingId,
                    title = episode.title,
                    episode = episode.episode
                )
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun sendSystemNotification(id: Int, title: String, episode: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Epizoda $episode je dostupna")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "airing_episodes"
        private const val WORK_NAME = "airing_notification_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AiringNotificationWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
