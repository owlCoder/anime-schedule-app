package com.owlcoder.animeschedule.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AiringEpisodeDao {
    @Query("SELECT * FROM airing_episodes WHERE airingAtEpochSeconds >= :from AND airingAtEpochSeconds <= :to ORDER BY airingAtEpochSeconds ASC")
    fun getAiringEpisodesInRange(from: Long, to: Long): Flow<List<AiringEpisodeEntity>>

    @Upsert
    suspend fun upsertAll(episodes: List<AiringEpisodeEntity>)

    /** Deletes only episodes that aired before the cutoff; future episodes are never touched. */
    @Query("DELETE FROM airing_episodes WHERE airingAtEpochSeconds < :olderThanEpoch")
    suspend fun deleteAiredBefore(olderThanEpoch: Long)

    @Query("SELECT MAX(cachedAtEpochSeconds) FROM airing_episodes WHERE airingAtEpochSeconds >= :from AND airingAtEpochSeconds <= :to")
    suspend fun getLastCacheTime(from: Long, to: Long): Long?
}
