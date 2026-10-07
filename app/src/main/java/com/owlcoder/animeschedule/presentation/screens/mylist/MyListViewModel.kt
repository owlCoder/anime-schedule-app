package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.repository.MalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import com.owlcoder.animeschedule.domain.model.WatchTools
import com.owlcoder.animeschedule.data.local.datastore.WatchToolsStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MyListUiState(
    val entries: List<MalListEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: WatchStatus? = WatchStatus.WATCHING,
    val allEntries: List<MalListEntry> = emptyList(),
    val favoritesOnly: Boolean = false,
    val unratedOnly: Boolean = false,
    val activeTag: String? = null,
    val tools: WatchTools = WatchTools(),
    val pendingIncrementIds: Set<Int> = emptySet(),
    /** Count of list entries per status, independent of [searchQuery]/[activeFilter]. */
    val statusCounts: Map<WatchStatus, Int> = emptyMap(),
    val sortOrder: MyListSortOrder = MyListSortOrder.RECENT,
    val insights: MyListInsights = MyListInsights(),
)

private data class MyListContent(
    val entries: List<MalListEntry>,
    val searchQuery: String,
    val activeFilter: WatchStatus?,
    val allEntries: List<MalListEntry>,
    val favoritesOnly: Boolean = false,
    val unratedOnly: Boolean = false,
    val activeTag: String? = null,
    val tools: WatchTools = WatchTools(),
    val statusCounts: Map<WatchStatus, Int>,
    val sortOrder: MyListSortOrder,
    val insights: MyListInsights,
)

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val malRepository: MalRepository,
    authRepository: AuthRepository,
    private val toolsStore: WatchToolsStore? = null,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(MyListSortOrder.RECENT)
    private val _activeFilter = MutableStateFlow<WatchStatus?>(WatchStatus.WATCHING)
    private val _tagFilter = MutableStateFlow<String?>(null)
    private val _quickFilters = MutableStateFlow(false to false)
    private val _isLoading = MutableStateFlow(true)
    private val _pendingIncrementIds = MutableStateFlow<Set<Int>>(emptySet())
    private var syncJob: Job? = null

    sealed interface UpdateEvent {
        data object Success : UpdateEvent
        data object Removed : UpdateEvent
        data object Error : UpdateEvent
    }

    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    private val baseContent = combine(
        malRepository.getUserList(),
        _searchQuery,
        _activeFilter,
        _sortOrder,
    ) { allEntries, query, filter, sortOrder ->
        val filteredEntries = allEntries.filter { entry ->
            filter == null || entry.status == filter
        }
        MyListContent(
            entries = filteredEntries.sortedFor(sortOrder),
            allEntries = allEntries,
            searchQuery = query,
            activeFilter = filter,
            statusCounts = allEntries.groupingBy { it.status }.eachCount(),
            sortOrder = sortOrder,
            insights = allEntries.insights(),
        )
    }.flowOn(Dispatchers.Default)

    private val listContent = combine(baseContent, _quickFilters, toolsStore?.data ?: flowOf(WatchTools()), _tagFilter) { content, quick, tools, tag ->
        content.copy(
            entries = content.entries.filter { (content.searchQuery.trim().let { query -> query.isBlank() || it.title.contains(query, true) || tools.notes[it.animeId].orEmpty().contains(query, true) || tools.tags[it.animeId].orEmpty().any { tag -> tag.contains(query, true) } }) && (!quick.first || it.animeId in tools.favorites) && (!quick.second || it.score == 0) && (tag == null || tools.tags[it.animeId].orEmpty().any { value -> value.equals(tag, ignoreCase = true) }) },
            favoritesOnly = quick.first, unratedOnly = quick.second, tools = tools, activeTag = tag,
        )
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<MyListUiState> = combine(
        listContent,
        _isLoading,
        authRepository.isLoggedIn,
        _pendingIncrementIds,
    ) { content, loading, loggedIn, pending ->
        MyListUiState(
            entries = content.entries,
            allEntries = content.allEntries,
            favoritesOnly = content.favoritesOnly,
            unratedOnly = content.unratedOnly,
            activeTag = content.activeTag,
            tools = content.tools,
            isLoading = loading,
            isLoggedIn = loggedIn,
            searchQuery = content.searchQuery,
            activeFilter = content.activeFilter,
            pendingIncrementIds = pending,
            statusCounts = content.statusCounts,
            sortOrder = content.sortOrder,
            insights = content.insights,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        // Auth is restored asynchronously. Assume the existing session is valid until the first
        // repository emission arrives, so a logged-in user sees loading instead of a login flash.
        MyListUiState(isLoading = true, isLoggedIn = true),
    )

    init {
        refreshIfStale()
    }

    fun setSearchQuery(query: String) = _searchQuery.update { query }

    fun setSortOrder(order: MyListSortOrder) = _sortOrder.update { order }

    fun setFilter(status: WatchStatus?) = _activeFilter.update { status }

    fun clearQuickFilters() { _searchQuery.value = ""; _quickFilters.value = false to false; _tagFilter.value = null }
    fun setTagFilter(tag: String?) { _tagFilter.value = tag }
    fun toggleFavorites() = _quickFilters.update { !it.first to it.second }
    fun toggleUnrated() = _quickFilters.update { it.first to !it.second }
    fun setWeeklyGoal(goal: Int) { viewModelScope.launch { toolsStore?.setWeeklyGoal(goal) } }
    fun clearActivity() { viewModelScope.launch { toolsStore?.clearActivity() } }

    /** Forced refresh (pull-to-refresh). Joins a sync that is already running. */
    fun refresh() = sync(force = true)

    private fun refreshIfStale() = sync(force = false)

    private fun sync(force: Boolean) {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val synced = malRepository.refreshUserList(force)
                if (force && !synced) _updateEvent.send(UpdateEvent.Error)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEntry(animeId: Int, update: MalListUpdate) {
        viewModelScope.launch {
            val result = malRepository.updateListEntry(animeId, update)
            _updateEvent.send(
                if (result is AppResult.Success) UpdateEvent.Success else UpdateEvent.Error,
            )
        }
    }

    fun removeEntry(animeId: Int) {
        viewModelScope.launch {
            val result = malRepository.removeListEntry(animeId)
            _updateEvent.send(
                if (result is AppResult.Success) UpdateEvent.Removed else UpdateEvent.Error,
            )
        }
    }

    fun incrementEpisode(animeId: Int) {
        if (animeId in _pendingIncrementIds.value) return
        _pendingIncrementIds.update { it + animeId }
        viewModelScope.launch {
            try {
                val result = malRepository.incrementEpisode(animeId)
                if (result is AppResult.Error) {
                    _updateEvent.send(UpdateEvent.Error)
                }
            } finally {
                _pendingIncrementIds.update { it - animeId }
            }
        }
    }
}
