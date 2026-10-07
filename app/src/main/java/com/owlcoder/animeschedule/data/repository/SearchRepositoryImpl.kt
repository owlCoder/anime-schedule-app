package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.alternative.AlternativeAnimeDataSource
import com.owlcoder.animeschedule.data.api.alternative.CatalogPage
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.datastore.RecentSearchesDataStore
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.offline.OfflineCatalogDataSource
import com.owlcoder.animeschedule.data.provider.ProviderCall
import com.owlcoder.animeschedule.data.provider.ProviderOperation
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import com.owlcoder.animeschedule.data.provider.ProviderResult
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.SearchPage
import com.owlcoder.animeschedule.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import com.owlcoder.animeschedule.data.api.alternative.toInternalId
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toSearchResult
import com.owlcoder.animeschedule.data.provider.requireProviderData
import com.owlcoder.animeschedule.data.mapper.toDetailEntity

private const val MIN_QUERY_LENGTH = 2

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val aniListDataSource: AniListRemoteDataSource,
    private val malListEntryDao: MalListEntryDao,
    private val animeDetailDao: AnimeDetailDao,
    private val alternativeDataSource: AlternativeAnimeDataSource,
    private val providerOrchestrator: ProviderOrchestrator,
    private val offlineCatalogDataSource: OfflineCatalogDataSource,
    private val recentSearchesDataStore: RecentSearchesDataStore
) : SearchRepository {

    override val recentSearches: Flow<List<String>> = recentSearchesDataStore.recentSearchesFlow

    override suspend fun saveRecentSearch(query: String) {
        val normalized = query.trim()
        if (normalized.length >= MIN_QUERY_LENGTH) recentSearchesDataStore.save(normalized)
    }

    override suspend fun clearRecentSearches() = recentSearchesDataStore.clear()
    override suspend fun removeRecentSearch(query: String) = recentSearchesDataStore.remove(query)

    override suspend fun searchAnime(query: String, page: Int): AppResult<SearchPage> {
        val normalized = query.trim()
        if (normalized.length < MIN_QUERY_LENGTH) {
            return AppResult.Success(SearchPage(emptyList(), hasNextPage = false))
        }
        val listEntries = malListEntryDao.getAll().first().associate { it.malId to it.toDomain() }

        val result = providerOrchestrator.firstSuccessful(
            operation = ProviderOperation.SEARCH,
            calls = listOf(
                ProviderCall("AniList") {
                    val response = aniListDataSource.searchAnime(normalized, page + 1)
                        .requireProviderData("AniList")
                    SearchPage(
                        results = response.media.map { medium ->
                            medium.toSearchResult(medium.idMal?.let(listEntries::get))
                        },
                        hasNextPage = response.hasNextPage
                    )
                },
                ProviderCall("Kitsu", isUsable = { it.results.isNotEmpty() }) {
                    alternativeDataSource.searchKitsu(normalized, page + 1).toSearchPageAndCache(listEntries)
                },
                ProviderCall("AnimeSchedule", isUsable = { it.results.isNotEmpty() }) {
                    alternativeDataSource.searchAnimeSchedule(normalized, page + 1)
                        .toSearchPageAndCache(listEntries)
                }
            )
        )

        return when (result) {
            is ProviderResult.Success -> AppResult.Success(result.value)
            is ProviderResult.Exhausted -> searchCachedTitles(normalized, page, result)
        }
    }

    /**
     * Last resort when every provider failed or came back empty: titles this device has already
     * seen. With nothing cached, a provider failure is reported rather than a misleading
     * "no results".
     */
    private suspend fun searchCachedTitles(
        query: String,
        page: Int,
        exhausted: ProviderResult.Exhausted
    ): AppResult<SearchPage> {
        val cached = (offlineCatalogDataSource.search(query, page) as? AppResult.Success)?.data
        return when {
            cached != null && cached.results.isNotEmpty() -> AppResult.Success(cached)
            exhausted.failures.isNotEmpty() ->
                AppResult.Error(AppError.Network(exhausted.failures.last().message))
            else -> AppResult.Success(SearchPage(emptyList(), hasNextPage = false))
        }
    }

    private suspend fun CatalogPage.toSearchPageAndCache(
        listEntries: Map<Int, MalListEntry>
    ): SearchPage {
        val now = Instant.now().epochSecond
        animeDetailDao.insertIfAbsent(items.map { it.toDetailEntity(it.toInternalId(), now) })
        return SearchPage(
            results = items.map { item -> item.toSearchResult(item.malId?.let(listEntries::get)) },
            hasNextPage = hasNextPage
        )
    }
}
