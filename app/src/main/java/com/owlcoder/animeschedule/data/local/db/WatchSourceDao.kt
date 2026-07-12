package com.owlcoder.animeschedule.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchSourceDao {
    @Query("SELECT * FROM watch_sources ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<WatchSourceEntity>>

    @Insert
    suspend fun insert(entity: WatchSourceEntity): Long

    @Update
    suspend fun update(entity: WatchSourceEntity)

    @Delete
    suspend fun delete(entity: WatchSourceEntity)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM watch_sources")
    suspend fun nextSortOrder(): Int
}
