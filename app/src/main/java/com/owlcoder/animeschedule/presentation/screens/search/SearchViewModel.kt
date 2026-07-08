package com.owlcoder.animeschedule.presentation.screens.search

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.usecase.ClearRecentSearchesUseCase
import com.owlcoder.animeschedule.domain.usecase.GetMalUserListUseCase
import com.owlcoder.animeschedule.domain.usecase.GetRecentSearchesUseCase
import com.owlcoder.animeschedule.domain.usecase.RemoveMalListEntryUseCase
import com.owlcoder.animeschedule.domain.usecase.SaveRecentSearchUseCase
import com.owlcoder.animeschedule.domain.usecase.SearchAnimeUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<AnimeSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val noResults: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAnimeUseCase: SearchAnimeUseCase,
    private val updateMalListEntryUseCase: UpdateMalListEntryUseCase,
    private val removeMalListEntryUseCase: RemoveMalListEntryUseCase,
    getMalUserListUseCase: GetMalUserListUseCase,
    getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveRecentSearchUseCase: SaveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _search = MutableStateFlow(SearchUiState())
    private var searchJob: Job? = null
    private var currentPage = 0

    sealed interface UpdateEvent {
        data object Success : UpdateEvent
        data object Removed : UpdateEvent
        data object Error : UpdateEvent
    }
    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    val recentSearches: StateFlow<List<String>> = getRecentSearchesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Results are a snapshot from search time — re-derive each result's list entry from
    // the live local list so "on list" badges update right after a status edit.
    val uiState: StateFlow<SearchUiState> = _search
        .combine(getMalUserListUseCase()) { state, listResult ->
            val entries = (listResult as? AppResult.Success)?.data ?: return@combine state
            val byMalId = entries.associateBy { it.animeId }
            state.copy(results = state.results.map { r ->
                val fresh = r.malId?.let { byMalId[it] }
                if (fresh != r.userListEntry) r.copy(userListEntry = fresh) else r
            })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    init {
        viewModelScope.launch {
            _query.debounce(300L).distinctUntilChanged().collect { query -> startSearch(query) }
        }
    }

    fun setQuery(query: String) = _query.update { query }

    fun onSearchSubmit(query: String) {
        viewModelScope.launch { if (query.length >= 2) saveRecentSearchUseCase(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { clearRecentSearchesUseCase() }
    }

    fun retrySearch() = startSearch(_query.value)

    private fun startSearch(query: String) {
        searchJob?.cancel()
        currentPage = 0
        if (query.length < 2) {
            _search.value = SearchUiState(query = query)
            return
        }
        _search.value = SearchUiState(query = query, isLoading = true)
        searchJob = viewModelScope.launch {
            when (val result = searchAnimeUseCase(query, page = 0)) {
                is AppResult.Success -> _search.update {
                    it.copy(
                        results = result.data.results,
                        isLoading = false,
                        hasNextPage = result.data.hasNextPage,
                        noResults = result.data.results.isEmpty()
                    )
                }
                is AppResult.Error -> _search.update {
                    it.copy(isLoading = false, errorRes = R.string.error_search)
                }
            }
        }
    }

    fun loadMore() {
        val state = _search.value
        if (state.isLoading || state.isLoadingMore || !state.hasNextPage) return
        _search.update { it.copy(isLoadingMore = true) }
        searchJob = viewModelScope.launch {
            when (val result = searchAnimeUseCase(state.query, page = currentPage + 1)) {
                is AppResult.Success -> {
                    currentPage++
                    _search.update {
                        it.copy(
                            // AniList pagination can repeat borderline items between pages —
                            // dedupe by id so LazyColumn keys stay unique.
                            results = (it.results + result.data.results).distinctBy { r -> r.anilistId },
                            isLoadingMore = false,
                            hasNextPage = result.data.hasNextPage
                        )
                    }
                }
                // Silent stop: the already-loaded results stay usable; the next scroll to the
                // end simply retries because hasNextPage is still true.
                is AppResult.Error -> _search.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun updateListEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch {
            val result = updateMalListEntryUseCase(animeId, update)
            _updateEvent.send(
                if (result is AppResult.Success) UpdateEvent.Success else UpdateEvent.Error
            )
        }
    }

    fun removeListEntry(animeId: Int) {
        viewModelScope.launch {
            val result = removeMalListEntryUseCase(animeId)
            _updateEvent.send(
                if (result is AppResult.Success) UpdateEvent.Removed else UpdateEvent.Error
            )
        }
    }
}
