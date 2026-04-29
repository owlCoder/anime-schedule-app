package rs.owlcoder.animeschedule.data.api.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanScheduleResponse(
    val data: List<JikanAnime>
)

@Serializable
data class JikanAnime(
    @SerialName("mal_id") val malId: Int,
    val title: String,
    @SerialName("title_english") val titleEnglish: String? = null,
    val images: JikanImages? = null,
    val genres: List<JikanGenre> = emptyList(),
    val score: Double? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val type: String? = null,
    val broadcast: JikanBroadcast? = null
)

@Serializable
data class JikanImages(
    val jpg: JikanImageUrls? = null,
    val webp: JikanImageUrls? = null
)

@Serializable
data class JikanImageUrls(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null
)

@Serializable
data class JikanGenre(
    val name: String
)

@Serializable
data class JikanBroadcast(
    val day: String? = null,
    val time: String? = null,
    val timezone: String? = null
)
