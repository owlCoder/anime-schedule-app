package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.first
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.alternative.AlternativeAnimeDataSource
import com.owlcoder.animeschedule.data.api.alternative.CatalogPage
import com.owlcoder.animeschedule.data.api.alternative.toInternalId
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryEntity
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.offline.OfflineCatalogDataSource
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toSearchResult
import com.owlcoder.animeschedule.data.provider.ProviderCall
import com.owlcoder.animeschedule.data.provider.ProviderOperation
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import com.owlcoder.animeschedule.data.provider.ProviderResult
import com.owlcoder.animeschedule.data.provider.requireProviderData
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.SearchPage
import com.owlcoder.animeschedule.domain.repository.SearchRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val aniListDataSource: AniListRemoteDataSource,
    private val malListEntryDao: MalListEntryDao,
    private val animeDetailDao: AnimeDetailDao,
    private val alternativeDataSource: AlternativeAnimeDataSource,
    private val providerOrchestrator: ProviderOrchestrator,
    private val offlineCatalogDataSource: OfflineCatalogDataSource
) : SearchRepository {

    override suspend fun searchAnime(query: String, page: Int): AppResult<SearchPage> {
        if (query.length < 2) return AppResult.Success(SearchPage(emptyList(), hasNextPage = false))
        val malByMalId = malListEntryDao.getAll().first().associateBy { it.malId }

        val result = providerOrchestrator.firstSuccessful(
            operation = ProviderOperation.SEARCH,
            calls = listOf(
                ProviderCall("AniList") {
                    val response = aniListDataSource.searchAnime(query, page + 1)
                        .requireProviderData("AniList")
                    SearchPage(
                        results = response.media.map { medium ->
                            medium.toSearchResult(medium.idMal?.let { malByMalId[it]?.toDomain() })
                        },
                        hasNextPage = response.hasNextPage
                    )
                },
                ProviderCall("Kitsu", isUsable = { it.results.isNotEmpty() }) {
                    alternativeDataSource.searchKitsuAnime(query, page + 1)
                        .toSearchPageAndCache(malByMalId)
                },
                ProviderCall("AnimeSchedule", isUsable = { it.results.isNotEmpty() }) {
                    alternativeDataSource.searchAnimeScheduleAnime(query, page + 1)
                        .toSearchPageAndCache(malByMalId)
                }
            )
        )

        return when (result) {
            is ProviderResult.Success -> AppResult.Success(result.value)
            is ProviderResult.Exhausted -> {
                val cached = animeDetailDao.searchByTitle(query)
                if (cached.isNotEmpty()) {
                    AppResult.Success(
                        SearchPage(
                            results = cached.map { entity ->
                                val entry = entity.malId?.let { malByMalId[it]?.toDomain() }
                                AnimeSearchResult(
                                    anilistId = entity.animeId,
                                    malId = entity.malId,
                                    title = entity.titleEnglish ?: entity.titleRomaji
                                        ?: entity.titleNative ?: "Unknown",
                                    titleEnglish = entity.titleEnglish,
                                    coverImageUrl = entity.coverImageUrl,
                                    type = entity.format,
                                    year = entity.seasonYear?.toString(),
                                    meanScore = entity.meanScore?.toDouble(),
                                    totalEpisodes = entity.episodes,
                                    userListEntry = entry
                                )
                            },
                            hasNextPage = false
                        )
                    )
                } else {
                    when (val offline = offlineCatalogDataSource.search(query, page)) {
                        is AppResult.Success -> AppResult.Success(offline.data)
                        is AppResult.Error -> AppResult.Error(
                            result.failures.lastOrNull()?.message?.let(AppError::Network)
                                ?: offline.error
                        )
                    }
                }
            }
        }
    }

    private suspend fun CatalogPage.toSearchPageAndCache(
        malByMalId: Map<Int, MalListEntryEntity>
    ): SearchPage {
        val now = Instant.now().epochSecond
        val results = items.map { item ->
            val internalId = item.toInternalId()
            animeDetailDao.upsert(alternativeDataSource.run { item.toDetailEntity(internalId, now) })
            AnimeSearchResult(
                anilistId = internalId,
                malId = item.malId,
                title = item.title,
                titleEnglish = item.titleEnglish,
                coverImageUrl = item.coverImageUrl,
                type = item.format,
                year = item.seasonYear?.toString(),
                meanScore = item.averageScore?.toDouble(),
                totalEpisodes = item.episodes,
                userListEntry = item.malId?.let { malByMalId[it]?.toDomain() }
            )
        }
        return SearchPage(results, hasNextPage)
    }
}
