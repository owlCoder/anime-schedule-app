package rs.owlcoder.animeschedule.data.local.db

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
}
