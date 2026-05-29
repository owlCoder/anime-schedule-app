package rs.owlcoder.animeschedule.presentation.screens.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.domain.model.MalListEntry
import rs.owlcoder.animeschedule.domain.model.MalListUpdate
import rs.owlcoder.animeschedule.domain.model.WatchStatus
import rs.owlcoder.animeschedule.domain.repository.AuthRepository
import rs.owlcoder.animeschedule.domain.usecase.GetMalUserListUseCase
import rs.owlcoder.animeschedule.domain.usecase.IncrementEpisodeUseCase
import rs.owlcoder.animeschedule.domain.usecase.RefreshMalListUseCase
import rs.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import javax.inject.Inject

data class MyListUiState(
    val entries: List<MalListEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val activeFilter: WatchStatus = WatchStatus.WATCHING
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
            activeFilter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyListUiState())

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
        viewModelScope.launch { updateMalListEntryUseCase(animeId, update) }
    }

    fun incrementEpisode(animeId: Int) {
        viewModelScope.launch { incrementEpisodeUseCase(animeId) }
    }
}
