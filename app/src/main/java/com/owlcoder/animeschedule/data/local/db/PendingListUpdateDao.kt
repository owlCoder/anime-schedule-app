package com.owlcoder.animeschedule.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PendingListUpdateDao {
    @Query("SELECT * FROM pending_list_updates ORDER BY queuedAtEpochMs ASC")
    suspend fun getAll(): List<PendingListUpdateEntity>

    @Query("SELECT * FROM pending_list_updates WHERE animeId = :animeId")
    suspend fun getByAnimeId(animeId: Int): PendingListUpdateEntity?

    @Upsert
    suspend fun upsert(entity: PendingListUpdateEntity)

    @Query("DELETE FROM pending_list_updates WHERE animeId = :animeId")
    suspend fun deleteByAnimeId(animeId: Int)

    @Query("DELETE FROM pending_list_updates")
    suspend fun deleteAll()
}
