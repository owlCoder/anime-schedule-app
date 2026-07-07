package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.usecase.GetMalUserListUseCase
import com.owlcoder.animeschedule.domain.usecase.IncrementEpisodeUseCase
import com.owlcoder.animeschedule.domain.usecase.RefreshMalListUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import javax.inject.Inject

data class MyListUiState(
    val entries: List<MalListEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val activeFilter: WatchStatus = WatchStatus.WATCHING,
    val pendingIncrementIds: Set<Int> = emptySet(),
    /** Count of list entries per status, independent of [searchQuery]/[activeFilter] —
     *  drives the small count badge on each status tab. */
    val statusCounts: Map<WatchStatus, Int> = emptyMap()
)

@HiltViewModel
class MyListViewModel @Inject constructor(
    getMalUserListUseCase: GetMalUserListUseCase,
    private val updateMalListEntryUseCase: UpdateMalListEntryUseCase,
    private val incrementEpisodeUseCase: IncrementEpisodeUseCase,
    private val refreshMalListUseCase: RefreshMalListUseCase,
    authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(WatchStatus.WATCHING)
    private val _isLoading = MutableStateFlow(false)
    private val _pendingIncrementIds = MutableStateFlow<Set<Int>>(emptySet())

    sealed interface UpdateEvent {
        data object Error : UpdateEvent
    }
    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    val uiState: StateFlow<MyListUiState> = combine(
        getMalUserListUseCase(),
        _searchQuery,
        _activeFilter,
        _isLoading,
        authRepository.isLoggedIn
    ) { result, query, filter, loading, loggedIn ->
        val allEntries = (result as? AppResult.Success)?.data ?: emptyList()
        val filtered = allEntries
            .filter { it.status == filter }
            .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }
        MyListUiState(
            entries = filtered,
            isLoading = loading,
            isLoggedIn = loggedIn,
            error = if (result is AppResult.Error) "Greška pri učitavanju liste" else null,
            searchQuery = query,
            activeFilter = filter,
            statusCounts = allEntries.groupingBy { it.status }.eachCount()
        )
    }.combine(_pendingIncrementIds) { state, pending -> state.copy(pendingIncrementIds = pending) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyListUiState())

    init {
        refreshIfStale()
    }

    fun setSearchQuery(query: String) = _searchQuery.update { query }
    fun setFilter(status: WatchStatus) = _activeFilter.update { status }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { refreshMalListUseCase(force = true) }
            _isLoading.value = false
        }
    }

    private fun refreshIfStale() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { refreshMalListUseCase(force = false) }
            _isLoading.value = false
        }
    }

    fun updateEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch {
            val result = updateMalListEntryUseCase(animeId, update)
            if (result is AppResult.Error) {
                _updateEvent.send(UpdateEvent.Error)
            }
        }
    }

    fun incrementEpisode(animeId: Int) {
        if (animeId in _pendingIncrementIds.value) return
        _pendingIncrementIds.update { it + animeId }
        viewModelScope.launch {
            try {
                val result = incrementEpisodeUseCase(animeId)
                if (result is AppResult.Error) {
                    _updateEvent.send(UpdateEvent.Error)
                }
            } finally {
                _pendingIncrementIds.update { it - animeId }
            }
        }
    }
}
