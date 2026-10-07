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
import kotlinx.coroutines.flow.update
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.domain.repository.SearchRepository
import javax.inject.Inject
import com.owlcoder.animeschedule.presentation.screens.discovery.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<AnimeSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val noResults: Boolean = false,
    val filter: SearchFilter = SearchFilter(),
    val loadedCount: Int = 0,
    val availableFormats: List<String> = emptyList(),
    val loadMoreError: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val malRepository: MalRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(SearchFilter())
    private var generation = 0
    private val _query = MutableStateFlow("")
    private val _search = MutableStateFlow(SearchUiState())
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var currentPage = 0

    sealed interface UpdateEvent {
        data object Success : UpdateEvent
        data object Removed : UpdateEvent
        data object Error : UpdateEvent
    }
    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    val recentSearches: StateFlow<List<String>> = searchRepository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Results are a snapshot from search time — re-derive each result's list entry from
    // the live local list so "on list" badges update right after a status edit.
    val uiState: StateFlow<SearchUiState> = combine(_search, malRepository.getUserList(), _filter) { state, entries, filter ->
            val byMalId = entries.associateBy { it.animeId }
            val live = state.results.map { r ->
                val fresh = r.malId?.let { byMalId[it] }
                if (fresh != r.userListEntry) r.copy(userListEntry = fresh) else r
            }
            state.copy(results = live.discover(filter), filter = filter, loadedCount = live.size,
                availableFormats = live.mapNotNull { it.type?.uppercase(java.util.Locale.ROOT) }.distinct().sorted())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    init {
        viewModelScope.launch {
            _query.debounce(300L).distinctUntilChanged().collect { query -> startSearch(query) }
        }
    }

    fun setQuery(query: String) {
        val normalized = query.trim()
        // Whitespace edits should not discard useful results or restart a catalog request.
        if (_query.value == normalized) return
        // Invalidate in-flight responses immediately, including the debounce window.
        generation++
        searchJob?.cancel()
        loadMoreJob?.cancel()
        _query.value = normalized
        _search.value = SearchUiState(query = normalized, isLoading = normalized.length >= MIN_QUERY_LENGTH)
    }
    fun setTracking(value: TrackingFilter) = _filter.update { it.copy(tracking = value) }
    fun toggleFormat(value: String) = _filter.update { it.copy(formats = if (value in it.formats) it.formats - value else it.formats + value) }
    fun setSort(value: SearchSort) = _filter.update { it.copy(sort = value) }
    fun clearFilter() { _filter.value = SearchFilter() }
    fun removeRecentSearch(query: String) { viewModelScope.launch { searchRepository.removeRecentSearch(query) } }

    fun onSearchSubmit(query: String) {
        viewModelScope.launch { searchRepository.saveRecentSearch(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { searchRepository.clearRecentSearches() }
    }

    fun retrySearch() = startSearch(_query.value)

    private fun startSearch(rawQuery: String) {
        val query = rawQuery.trim()
        val requestGeneration = ++generation
        searchJob?.cancel()
        loadMoreJob?.cancel()
        currentPage = 0
        if (query.length < MIN_QUERY_LENGTH) {
            _search.value = SearchUiState(query = query)
            return
        }
        _search.value = SearchUiState(query = query, isLoading = true)
        searchJob = viewModelScope.launch {
            val result = searchRepository.searchAnime(query, page = 0)
            if (requestGeneration != generation) return@launch
            when (result) {
                is AppResult.Success -> _search.update {
                    it.copy(
                        results = result.data.results.distinctBy { result -> result.anilistId },
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
        val requestGeneration = generation
        _search.update { it.copy(isLoadingMore = true, loadMoreError = false) }
        loadMoreJob = viewModelScope.launch {
            val result = searchRepository.searchAnime(state.query, page = currentPage + 1)
            if (requestGeneration != generation) return@launch
            when (result) {
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
                // Keep results available and expose an explicit retry after a page failure.
                is AppResult.Error -> _search.update { it.copy(isLoadingMore = false, loadMoreError = true) }
            }
        }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
    }

    fun updateListEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch {
            val result = malRepository.updateListEntry(animeId, update)
            _updateEvent.send(
                if (result is AppResult.Success) UpdateEvent.Success else UpdateEvent.Error
            )
        }
    }

    fun removeListEntry(animeId: Int) {
        viewModelScope.launch {
            val result = malRepository.removeListEntry(animeId)
            _updateEvent.send(
                if (result is AppResult.Success) UpdateEvent.Removed else UpdateEvent.Error
            )
        }
    }
}
