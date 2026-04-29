package rs.owlcoder.animeschedule.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mal_list_entries")
data class MalListEntryEntity(
    @PrimaryKey val animeId: Int,
    val malId: Int,
    val title: String,
    val coverImageUrl: String?,
    val totalEpisodes: Int?,
    val status: String,
    val numEpisodesWatched: Int,
    val score: Int,
    val updatedAt: String?
)
