package com.owlcoder.animeschedule.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDetailDao {
    @Query("SELECT * FROM anime_details WHERE animeId = :id")
    fun getById(id: Int): Flow<AnimeDetailEntity?>

    @Upsert
    suspend fun upsert(entity: AnimeDetailEntity)

    @Query("SELECT cachedAtEpochSeconds FROM anime_details WHERE animeId = :id")
    suspend fun getCacheTime(id: Int): Long?

    @Query("SELECT * FROM anime_details WHERE animeId = :id")
    suspend fun getByIdOnce(id: Int): AnimeDetailEntity?

    @Query("SELECT * FROM anime_details WHERE malId = :malId LIMIT 1")
    suspend fun getByMalId(malId: Int): AnimeDetailEntity?

    /** Small offline search index for titles visited while a remote provider is unavailable. */
    @Query(
        "SELECT * FROM anime_details " +
            "WHERE titleRomaji LIKE '%' || :query || '%' " +
            "OR titleEnglish LIKE '%' || :query || '%' " +
            "OR titleNative LIKE '%' || :query || '%' " +
            "ORDER BY COALESCE(meanScore, 0) DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun searchByTitle(
        query: String,
        limit: Int = 20,
        offset: Int = 0
    ): List<AnimeDetailEntity>

    /** Remove stale cache rows while preserving anything in the user's MAL/offline queue. */
    @Query(
        "DELETE FROM anime_details " +
            "WHERE cachedAtEpochSeconds < :cutoff " +
            "AND NOT EXISTS (" +
            "SELECT 1 FROM mal_list_entries WHERE mal_list_entries.animeId = anime_details.animeId" +
            ") " +
            "AND NOT EXISTS (" +
            "SELECT 1 FROM pending_list_updates WHERE pending_list_updates.animeId = anime_details.animeId" +
            ")"
    )
    suspend fun deleteStaleUnreferenced(cutoff: Long)

    /** Seasonal rows are only those already fetched and persisted by a remote provider. */
    @Query(
        "SELECT * FROM anime_details " +
            "WHERE seasonYear = :year " +
            "AND LOWER(COALESCE(season, '')) = LOWER(:season) " +
            "ORDER BY COALESCE(meanScore, 0) DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun getCachedSeason(
        season: String,
        year: Int,
        limit: Int = 50,
        offset: Int = 0
    ): List<AnimeDetailEntity>

    @Query("SELECT cachedAtEpochSeconds FROM anime_details WHERE malId = :malId LIMIT 1")
    suspend fun getCacheTimeByMalId(malId: Int): Long?
}
