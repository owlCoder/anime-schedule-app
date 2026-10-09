package com.owlcoder.animeschedule.data.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.owlcoder.animeschedule.MainActivity
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.data.local.db.NotificationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPoster @Inject constructor(@ApplicationContext private val context: Context) {
    fun build(row: NotificationEntity, generation: Int = 0, canIncrement: Boolean = true, cover: Bitmap? = null, message: String? = null, actionToken: String = ""): android.app.Notification {
        val open = Intent(Intent.ACTION_VIEW, "com.owlcoder.animeschedule://detail/${row.animeId}".toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val content = message ?: context.getString(R.string.notif_content_text, row.episode)
        val builder = NotificationCompat.Builder(context, AiringNotificationWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(row.title)
            .setContentText(content)
            .setSubText(context.getString(R.string.notif_episode_label, row.episode))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(android.graphics.Color.rgb(36, 121, 236))
            // A reminder is delivered now; its history row can be much older.
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(row.title).bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).setOnlyAlertOnce(true)
            .setContentIntent(PendingIntent.getActivity(context, row.id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        if (canIncrement && actionToken.isNotEmpty()) builder.addAction(android.R.drawable.ic_input_add, context.getString(R.string.notification_increment), action(row.id, generation, NotificationActionReceiver.INCREMENT, actionToken))
        if (actionToken.isNotEmpty()) builder.addAction(android.R.drawable.ic_lock_idle_alarm, context.getString(R.string.notification_snooze), action(row.id, generation, NotificationActionReceiver.SNOOZE, actionToken))
        if (cover != null) builder.setLargeIcon(cover)
        return builder.build()
    }

    private fun action(id: Int, generation: Int, action: String, token: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).setAction(action)
            .setData("com.owlcoder.animeschedule://notification-action/$id/$token/$generation/$action".toUri())
            .putExtra(NotificationActionReceiver.TOKEN, token)
            .putExtra(NotificationActionReceiver.ID, id).putExtra(NotificationActionReceiver.GENERATION, generation)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun post(row: NotificationEntity, generation: Int = 0, canIncrement: Boolean = true, cover: Bitmap? = null, message: String? = null, actionToken: String = "") {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        try { manager.notify(row.id, build(row, generation, canIncrement, cover, message, actionToken)) }
        catch (_: SecurityException) { /* Permission can be revoked between check and post. */ }
    }
    fun cancel(id: Int) { NotificationManagerCompat.from(context).cancel(id) }
}
