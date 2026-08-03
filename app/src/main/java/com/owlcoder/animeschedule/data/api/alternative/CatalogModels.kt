package com.owlcoder.animeschedule.data.api.alternative

/** Provider-neutral anime record used only when AniList is unavailable. */
data class CatalogAnime(
    val providerId: String,
    val anilistId: Int?,
    val malId: Int?,
    val title: String,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val coverImageUrl: String?,
    val bannerImageUrl: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val status: String? = null,
    val format: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val nextAiringAt: Long? = null,
    val siteUrl: String? = null
)

data class CatalogPage(
    val items: List<CatalogAnime>,
    val hasNextPage: Boolean
)

/** Keeps alternative-provider records addressable by the existing Int-only detail route. */
fun CatalogAnime.toInternalId(): Int {
    val value = anilistId ?: (providerId.hashCode() and Int.MAX_VALUE)
    return if (anilistId != null) value else -value.coerceAtLeast(1)
}
