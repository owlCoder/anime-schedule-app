package rs.owlcoder.animeschedule.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_details")
data class AnimeDetailEntity(
    @PrimaryKey val animeId: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val coverImageUrl: String?,
    val coverColor: String?,
    val bannerImageUrl: String?,
    val description: String?,
    val genres: List<String>,
    val averageScore: Int?,
    val meanScore: Int?,
    val episodes: Int?,
    val duration: Int?,
    val status: String?,
    val format: String?,
    val season: String?,
    val seasonYear: Int?,
    val nextAiringEpisode: Int?,
    val nextAiringAt: Long?,
    val studiosJson: String?,
    val relationsJson: String?,
    val trailerSite: String?,
    val trailerId: String?,
    val siteUrl: String?,
    val cachedAtEpochSeconds: Long
)
