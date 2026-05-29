package rs.owlcoder.animeschedule.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "airing_episodes")
data class AiringEpisodeEntity(
    @PrimaryKey val airingId: Int,
    val animeId: Int,
    val malId: Int? = null,
    val episode: Int,
    val airingAtEpochSeconds: Long,
    val title: String,
    val titleRomaji: String?,
    val coverImageUrl: String?,
    val coverColor: String?,
    val genres: List<String>,
    val averageScore: Int?,
    val totalEpisodes: Int?,
    val status: String?,
    val format: String?,
    val cachedAtEpochSeconds: Long,
    val source: String
)
