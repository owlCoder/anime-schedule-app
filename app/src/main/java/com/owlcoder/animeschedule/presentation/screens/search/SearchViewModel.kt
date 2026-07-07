package com.owlcoder.animeschedule.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.usecase.ClearRecentSearchesUseCase
import com.owlcoder.animeschedule.domain.usecase.GetRecentSearchesUseCase
import com.owlcoder.animeschedule.domain.usecase.SaveRecentSearchUseCase
import com.owlcoder.animeschedule.domain.usecase.SearchAnimeUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<AnimeSearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val noResults: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAnimeUseCase: SearchAnimeUseCase,
    private val updateMalListEntryUseCase: UpdateMalListEntryUseCase,
    getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveRecentSearchUseCase: SaveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _retryTrigger = MutableStateFlow(0)

    sealed interface UpdateEvent {
        data object Error : UpdateEvent
    }
    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    val recentSearches: StateFlow<List<String>> = getRecentSearchesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SearchUiState> = _query
        .debounce(300L)
        .distinctUntilChanged()
        .combine(_retryTrigger) { query, _ -> query }
        .flatMapLatest { query ->
            flow {
                if (query.length < 2) {
                    emit(SearchUiState(query = query, recentSearches = recentSearches.value))
                    return@flow
                }
                _isLoading.value = true
                emit(SearchUiState(query = query, isLoading = true, recentSearches = recentSearches.value))
                val result = searchAnimeUseCase(query)
                _isLoading.value = false
                when (result) {
                    is AppResult.Success -> emit(
                        SearchUiState(
                            query = query,
                            results = result.data,
                            noResults = result.data.isEmpty(),
                            recentSearches = recentSearches.value
                        )
                    )
                    is AppResult.Error -> emit(
                        SearchUiState(query = query, error = "Greška pri pretrazi", recentSearches = recentSearches.value)
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun setQuery(query: String) = _query.update { query }

    fun onSearchSubmit(query: String) {
        viewModelScope.launch { if (query.length >= 2) saveRecentSearchUseCase(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { clearRecentSearchesUseCase() }
    }

    fun updateListEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch {
            val result = updateMalListEntryUseCase(animeId, update)
            if (result is AppResult.Error) {
                _updateEvent.send(UpdateEvent.Error)
            }
        }
    }

    fun retrySearch() = _retryTrigger.update { it + 1 }
}
