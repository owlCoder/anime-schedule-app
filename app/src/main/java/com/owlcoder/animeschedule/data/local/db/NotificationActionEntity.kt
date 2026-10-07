package com.owlcoder.animeschedule.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

/** Durable receipt: replaying an old notification must never increment progress again. */
@Entity(tableName = "notification_actions")
data class NotificationActionEntity(
    @PrimaryKey val notificationId: Int,
    val malId: Int,
    val progressHandled: Boolean = false,
    val snoozeGeneration: Int = 0,
    val snoozedUntilEpochMs: Long? = null,
    @ColumnInfo(defaultValue = "''") val actionToken: String = UUID.randomUUID().toString(),
)
