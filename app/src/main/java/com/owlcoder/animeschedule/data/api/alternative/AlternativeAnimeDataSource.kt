package com.owlcoder.animeschedule.data.api.alternative

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import com.owlcoder.animeschedule.data.local.db.AnimeDetailEntity
import android.util.Log
import kotlinx.coroutines.CancellationException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class AlternativeAnimeDataSource @Inject constructor(
    private val kitsuApi: KitsuApiService,
    private val animeScheduleApi: AnimeScheduleApiService
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun searchAnime(query: String, page: Int): CatalogPage {
        tryOrNull("Kitsu search") { searchKitsuAnime(query, page) }
            ?.takeIf { it.items.isNotEmpty() || page > 1 }
            ?.let { return it }
        return searchAnimeScheduleAnime(query, page)
    }

    suspend fun getSeasonalAnime(season: String, year: Int): List<CatalogAnime> {
        tryOrNull("Kitsu season") { getKitsuSeasonalAnime(season, year) }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        return tryOrNull("AnimeSchedule season") { getAnimeScheduleSeasonalAnime(season, year) }
            ?: emptyList()
    }

    suspend fun getByAniListId(id: Int): CatalogAnime? = getByAnimeScheduleAniListId(id)

    suspend fun getByMalId(id: Int): CatalogAnime? = getByAnimeScheduleMalId(id)

    suspend fun searchKitsuAnime(query: String, page: Int): CatalogPage = searchKitsu(query, page)

    suspend fun searchAnimeScheduleAnime(query: String, page: Int): CatalogPage {
        val response = animeScheduleApi.searchAnime(query = query, page = page)
        return CatalogPage(
            items = response.anime.map { it.toCatalogAnime() },
            hasNextPage = page * ANIME_SCHEDULE_PAGE_SIZE < response.totalAmount
        )
    }

    suspend fun getKitsuSeasonalAnime(season: String, year: Int): List<CatalogAnime> =
        getKitsuSeason(season, year)

    suspend fun getAnimeScheduleSeasonalAnime(season: String, year: Int): List<CatalogAnime> =
        animeScheduleApi.getSeasonalAnime(year = year, season = season.lowercase())
            .anime.map { it.toCatalogAnime() }

    suspend fun getByAnimeScheduleAniListId(id: Int): CatalogAnime? =
        animeScheduleApi.getByAniListId(id).anime.firstOrNull()?.toCatalogAnime()

    suspend fun getByAnimeScheduleMalId(id: Int): CatalogAnime? =
        animeScheduleApi.getByMalId(id).anime.firstOrNull()?.toCatalogAnime()

    suspend fun getKitsuById(id: String): CatalogAnime? = tryOrNull("Kitsu detail") {
        val response = kitsuApi.getAnime(id)
        response.data.toCatalogAnime(emptyList(), json)
    }

    private suspend fun searchKitsu(query: String, page: Int): CatalogPage {
        val response = kitsuApi.searchAnime(
            query = query,
            limit = KITSU_PAGE_SIZE,
            offset = (page - 1) * KITSU_PAGE_SIZE
        )
        val items = response.data.map { resource ->
            resource.toCatalogAnime(response.included, json)
        }
        val total = response.meta?.count ?: ((page - 1) * KITSU_PAGE_SIZE + items.size)
        return CatalogPage(items, hasNextPage = page * KITSU_PAGE_SIZE < total)
    }

    private suspend fun getKitsuSeason(season: String, year: Int): List<CatalogAnime> {
        val all = mutableListOf<CatalogAnime>()
        var offset = 0
        while (offset < KITSU_MAX_SEASON_RESULTS) {
            val response = kitsuApi.getSeasonalAnime(
                season = season.lowercase(),
                year = year,
                limit = KITSU_PAGE_SIZE,
                offset = offset
            )
            val page = response.data.map { it.toCatalogAnime(response.included, json) }
            all += page
            offset += KITSU_PAGE_SIZE
            if (page.size < KITSU_PAGE_SIZE || offset >= (response.meta?.count ?: offset)) break
        }
        return all
    }

    private fun KitsuAnimeResource.toCatalogAnime(
        included: List<KitsuIncludedResource> = emptyList(),
        json: Json
    ): CatalogAnime {
        val attributes = attributes ?: KitsuAnimeAttributes()
        val mappingIds = relationships?.mappings?.data.orEmpty().map { it.id }.toSet()
        val mappings = included.asSequence()
            .filter { it.id in mappingIds && it.attributes != null }
            .mapNotNull { resource ->
                runCatching {
                    json.decodeFromJsonElement<KitsuMappingAttributes>(resource.attributes!!)
                }.getOrNull()
            }
            .toList()
        val anilistId = mappings.firstExternalId("anilist/anime")
            ?: mappings.firstExternalId("anilist")
        val malId = mappings.firstExternalId("myanimelist/anime")
            ?: mappings.firstExternalId("myanimelist")
        val romaji = attributes.titles["en_jp"] ?: attributes.titles["ja_jp"]
        val english = attributes.titles["en"] ?: attributes.titles["en_us"]
        val title = attributes.canonicalTitle ?: english ?: romaji ?: "Unknown"
        return CatalogAnime(
            providerId = "kitsu:$id",
            anilistId = anilistId,
            malId = malId,
            title = title,
            titleRomaji = romaji,
            titleEnglish = english,
            coverImageUrl = attributes.posterImage?.large ?: attributes.posterImage?.original,
            bannerImageUrl = attributes.coverImage?.large,
            description = attributes.synopsis ?: attributes.description,
            averageScore = attributes.averageRating?.toDoubleOrNull()?.roundToInt(),
            episodes = attributes.episodeCount,
            duration = attributes.episodeLength,
            status = attributes.status,
            format = attributes.subtype,
            nextAiringAt = attributes.nextRelease.toEpochSecondsOrNull(),
            siteUrl = "https://kitsu.io/anime/$id"
        )
    }

    private fun AnimeScheduleAnime.toCatalogAnime(): CatalogAnime {
        val anilistId = websites?.aniList?.extractId("anilist.co/anime")
        val malId = websites?.mal?.extractId("myanimelist.net/anime")
        val romaji = names?.romaji
        val english = names?.english
        val title = title ?: english ?: romaji ?: "Unknown"
        return CatalogAnime(
            providerId = "animeschedule:$id",
            anilistId = anilistId,
            malId = malId,
            title = title,
            titleRomaji = romaji,
            titleEnglish = english,
            coverImageUrl = imageVersionRoute?.let {
                "https://img.animeschedule.net/production/assets/public/img/$it"
            },
            description = description,
            genres = genres.mapNotNull { it.name },
            averageScore = stats?.averageScore?.roundToInt(),
            episodes = episodes,
            duration = lengthMin,
            status = status,
            format = mediaTypes.firstOrNull()?.name,
            season = season?.season,
            seasonYear = season?.year?.toIntOrNull(),
            siteUrl = route?.let { "https://animeschedule.net/anime/$it" }
        )
    }

    fun CatalogAnime.toDetailEntity(internalId: Int, nowEpoch: Long): AnimeDetailEntity =
        AnimeDetailEntity(
            animeId = internalId,
            malId = malId,
            titleRomaji = titleRomaji,
            titleEnglish = titleEnglish,
            titleNative = null,
            coverImageUrl = coverImageUrl,
            coverColor = null,
            bannerImageUrl = bannerImageUrl,
            description = description,
            genres = genres,
            averageScore = averageScore,
            meanScore = averageScore,
            episodes = episodes,
            duration = duration,
            status = status,
            format = format,
            season = season,
            seasonYear = seasonYear,
            nextAiringEpisode = null,
            nextAiringAt = nextAiringAt,
            studiosJson = null,
            charactersJson = null,
            relationsJson = null,
            trailerSite = null,
            trailerId = null,
            siteUrl = siteUrl,
            cachedAtEpochSeconds = nowEpoch
        )

    private fun List<KitsuMappingAttributes>.firstExternalId(site: String): Int? =
        firstOrNull { it.externalSite == site }?.externalId?.toIntOrNull()

    private fun String?.extractId(prefix: String): Int? {
        if (this == null) return null
        val match = Regex("$prefix/(\\d+)").find(this) ?: Regex("/(\\d+)").find(this)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun String?.toEpochSecondsOrNull(): Long? = runCatching {
        if (this.isNullOrBlank()) null else Instant.parse(this).epochSecond
    }.getOrNull()

    private suspend fun <T> tryOrNull(label: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "$label failed", e)
        null
    }

    private companion object {
        const val TAG = "AlternativeAnime"
        const val KITSU_PAGE_SIZE = 20
        const val KITSU_MAX_SEASON_RESULTS = 100
        const val ANIME_SCHEDULE_PAGE_SIZE = 18
    }
}
