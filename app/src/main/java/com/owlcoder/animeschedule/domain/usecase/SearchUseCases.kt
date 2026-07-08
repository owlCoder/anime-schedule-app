package com.owlcoder.animeschedule.domain.usecase

import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.local.datastore.RecentSearchesDataStore
import com.owlcoder.animeschedule.domain.model.SearchPage
import com.owlcoder.animeschedule.domain.repository.SearchRepository
import javax.inject.Inject

class SearchAnimeUseCase @Inject constructor(private val searchRepository: SearchRepository) {
    suspend operator fun invoke(query: String, page: Int = 0): AppResult<SearchPage> {
        if (query.length < 2) return AppResult.Success(SearchPage(emptyList(), hasNextPage = false))
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
