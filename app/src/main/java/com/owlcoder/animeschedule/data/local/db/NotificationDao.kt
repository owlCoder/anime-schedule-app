package com.owlcoder.animeschedule.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAtEpochSeconds DESC")
    fun getAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Upsert
    suspend fun upsert(entity: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications WHERE isRead = 1 AND createdAtEpochSeconds < :cutoff")
    suspend fun deleteReadOlderThan(cutoff: Long)

    @Query("DELETE FROM notifications WHERE isRead = 0 AND createdAtEpochSeconds < :cutoff")
    suspend fun deleteUnreadOlderThan(cutoff: Long)

    @Query("SELECT id FROM notifications")
    suspend fun getAllIds(): List<Int>
}
