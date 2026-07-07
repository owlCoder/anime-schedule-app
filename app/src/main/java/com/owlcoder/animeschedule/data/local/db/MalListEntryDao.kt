package com.owlcoder.animeschedule.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MalListEntryDao {
    @Query("SELECT * FROM mal_list_entries ORDER BY title ASC")
    fun getAll(): Flow<List<MalListEntryEntity>>

    @Query("SELECT * FROM mal_list_entries WHERE animeId = :animeId")
    suspend fun getByAnimeId(animeId: Int): MalListEntryEntity?

    @Query("SELECT * FROM mal_list_entries WHERE animeId = :animeId")
    fun observeByAnimeId(animeId: Int): Flow<MalListEntryEntity?>

    @Query("SELECT * FROM mal_list_entries WHERE malId = :malId")
    fun observeByMalId(malId: Int): Flow<MalListEntryEntity?>

    @Upsert
    suspend fun upsert(entity: MalListEntryEntity)

    @Upsert
    suspend fun upsertAll(entities: List<MalListEntryEntity>)

    @Query("DELETE FROM mal_list_entries")
    suspend fun deleteAll()
}
