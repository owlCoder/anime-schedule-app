package rs.owlcoder.animeschedule.domain.usecase

import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.local.datastore.RecentSearchesDataStore
import rs.owlcoder.animeschedule.domain.model.AnimeSearchResult
import rs.owlcoder.animeschedule.domain.repository.SearchRepository
import javax.inject.Inject

class SearchAnimeUseCase @Inject constructor(private val searchRepository: SearchRepository) {
    suspend operator fun invoke(query: String, page: Int = 0): AppResult<List<AnimeSearchResult>> {
        if (query.length < 2) return AppResult.Success(emptyList())
        return searchRepository.searchAnime(query, page)
    }
}

class GetRecentSearchesUseCase @Inject constructor(private val dataStore: RecentSearchesDataStore) {
    operator fun invoke() = dataStore.recentSearchesFlow
}

class SaveRecentSearchUseCase @Inject constructor(private val dataStore: RecentSearchesDataStore) {
    suspend operator fun invoke(query: String) = dataStore.save(query)
}

class ClearRecentSearchesUseCase @Inject constructor(private val dataStore: RecentSearchesDataStore) {
    suspend operator fun invoke() = dataStore.clear()
}
