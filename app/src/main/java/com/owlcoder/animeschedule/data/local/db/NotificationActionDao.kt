package com.owlcoder.animeschedule.data.local.db

import androidx.room.*

@Dao
interface NotificationActionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationActionEntity): Long
    @Query("SELECT * FROM notification_actions WHERE notificationId = :id")
    suspend fun get(id: Int): NotificationActionEntity?
    @Query("UPDATE notification_actions SET progressHandled = 1, snoozedUntilEpochMs = NULL WHERE notificationId = :id")
    suspend fun consumeProgress(id: Int)
    @Query("UPDATE notification_actions SET snoozeGeneration = snoozeGeneration + 1, snoozedUntilEpochMs = :until WHERE notificationId = :id AND snoozeGeneration = :generation AND snoozedUntilEpochMs IS NULL AND progressHandled = 0")
    suspend fun snooze(id: Int, generation: Int, until: Long): Int
    @Query("UPDATE notification_actions SET snoozedUntilEpochMs = NULL WHERE notificationId = :id AND snoozeGeneration = :generation")
    suspend fun finishSnooze(id: Int, generation: Int)
    @Query("SELECT * FROM notification_actions WHERE snoozedUntilEpochMs IS NOT NULL AND progressHandled = 0")
    suspend fun pendingSnoozes(): List<NotificationActionEntity>
    @Query("DELETE FROM notification_actions")
    suspend fun deleteAll()
}
