package com.owlcoder.animeschedule.data.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/** No network or list writes in onReceive; WorkManager owns the durable action. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.takeIf { it == INCREMENT || it == SNOOZE } ?: return
        val id = intent.getIntExtra(ID, 0).takeIf { it > 0 } ?: return
        val generation = intent.getIntExtra(GENERATION, -1).takeIf { it >= 0 } ?: return
        val token = intent.getStringExtra(TOKEN)?.takeIf { it.isNotEmpty() } ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WorkManager.getInstance(context).enqueueUniqueWork("notification-action-$id-$token-$generation-$action", ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<NotificationActionWorker>()
                        .setInputData(workDataOf(ID to id, GENERATION to generation, ACTION to action, TOKEN to token)).build())
                    .result.get(8, TimeUnit.SECONDS)
            } catch (_: Exception) {
                // Keep the notification available for retry if durable enqueueing failed.
            } finally { pending.finish() }
        }
    }
    companion object {
        const val INCREMENT = "com.owlcoder.animeschedule.INCREMENT"
        const val SNOOZE = "com.owlcoder.animeschedule.SNOOZE"
        const val ID = "notification_id"
        const val GENERATION = "generation"
        const val TOKEN = "action_token"
        const val ACTION = "notification_action"
    }
}
