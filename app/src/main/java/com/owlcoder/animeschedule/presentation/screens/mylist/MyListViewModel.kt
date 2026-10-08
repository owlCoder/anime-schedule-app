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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import com.owlcoder.animeschedule.domain.model.LocalTextQuery
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import com.owlcoder.animeschedule.domain.model.WatchTools
import com.owlcoder.animeschedule.domain.model.SmartListFilter
import com.owlcoder.animeschedule.domain.model.SavedListView
import com.owlcoder.animeschedule.domain.model.matchesSmart
import com.owlcoder.animeschedule.domain.model.matchesRating
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
    val smartFilter: SmartListFilter = SmartListFilter.ALL,
    val scoreRange: Pair<Int, Int> = 0 to 10,
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
    val smartFilter: SmartListFilter = SmartListFilter.ALL,
    val scoreRange: Pair<Int, Int> = 0 to 10,
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
    private val _smartFilter = MutableStateFlow(SmartListFilter.ALL)
    private val _scoreRange = MutableStateFlow(0 to 10)
    private val _quickFilters = MutableStateFlow(false to false)
    private val _isLoading = MutableStateFlow(true)
    private val _pendingIncrementIds = MutableStateFlow<Set<Int>>(emptySet())
    private var syncJob: Job? = null

    sealed interface UpdateEvent {
        data object Success : UpdateEvent
        data object Removed : UpdateEvent
        data object PersonalSaved : UpdateEvent
        data object Error : UpdateEvent
    }

    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    private data class LibrarySnapshot(val entries: List<MalListEntry>, val counts: Map<WatchStatus, Int>, val insights: MyListInsights)
    // Typing and sorting do not recalculate statistics for the entire library.
    private val library = malRepository.getUserList().map { entries ->
        LibrarySnapshot(entries, entries.groupingBy { it.status }.eachCount(), entries.insights())
    }.flowOn(Dispatchers.Default)
    private val baseContent = combine(
        library,
        _searchQuery,
        _activeFilter,
        _sortOrder,
    ) { snapshot, query, filter, sortOrder ->
        val allEntries = snapshot.entries
        val filteredEntries = allEntries.filter { entry ->
            filter == null || entry.status == filter
        }
        MyListContent(
            entries = filteredEntries,
            allEntries = allEntries,
            searchQuery = query,
            activeFilter = filter,
            statusCounts = snapshot.counts,
            sortOrder = sortOrder,
            insights = snapshot.insights,
        )
    }.flowOn(Dispatchers.Default)

    private val listContent = combine(
        baseContent, _quickFilters, toolsStore?.data ?: flowOf(WatchTools()), _tagFilter, combine(_smartFilter, _scoreRange) { smart, scores -> smart to scores },
    ) { content, quick, tools, tag, filters ->
        val (smart, scores) = filters
        val query = LocalTextQuery(content.searchQuery)
        val entries = content.entries.filter { entry ->
            val id = entry.animeId
            val matchesQuery = query.matches(entry.title, tools.notes[id], tools.tags[id]?.joinToString(" "))
            matchesQuery && (!quick.first || id in tools.favorites) &&
                (!quick.second || entry.score == 0) &&
                (tag == null || tools.tags[id].orEmpty().any { it.equals(tag, ignoreCase = true) }) &&
                entry.matchesSmart(smart, tools) && entry.matchesRating(scores.first, scores.second)
        }.sortedFor(content.sortOrder, tools, pinsFirst = true)
        content.copy(
            entries = entries, favoritesOnly = quick.first, unratedOnly = quick.second,
            tools = tools, activeTag = tag, smartFilter = smart, scoreRange = scores,
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
            smartFilter = content.smartFilter,
            scoreRange = content.scoreRange,
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

    fun clearQuickFilters() { _searchQuery.value = ""; _quickFilters.value = false to false; _tagFilter.value = null; _smartFilter.value = SmartListFilter.ALL; _scoreRange.value = 0 to 10 }
    fun setTagFilter(tag: String?) { _tagFilter.value = tag }
    fun toggleFavorites() = _quickFilters.update { !it.first to it.second }
    fun showFavorites() { clearQuickFilters(); _activeFilter.value = null; _quickFilters.value = true to false }
    fun toggleUnrated() = _quickFilters.update { it.first to !it.second }
    fun setWeeklyGoal(goal: Int) { viewModelScope.launch { toolsStore?.setWeeklyGoal(goal) } }
    fun clearActivity() { viewModelScope.launch { toolsStore?.clearActivity() } }
    fun setSmartFilter(filter: SmartListFilter) {
        _smartFilter.value = filter
        if (filter == SmartListFilter.COMPLETED_UNRATED) _activeFilter.value = WatchStatus.COMPLETED
    }
    fun setScoreRange(minimum: Int, maximum: Int) { val min = minimum.coerceIn(0, 10); _scoreRange.value = min to maximum.coerceIn(min, 10) }
    fun setMarkers(ids: Set<Int>, favorite: Boolean? = null, pin: Boolean? = null) {
        viewModelScope.launch {
            try { toolsStore?.setMarkers(ids, favorite, pin); _updateEvent.send(UpdateEvent.PersonalSaved) }
            catch (_: java.io.IOException) { _updateEvent.send(UpdateEvent.Error) }
        }
    }
    fun renameTag(old: String, replacement: String?) {
        viewModelScope.launch {
            try {
                toolsStore?.renameTag(old, replacement)
                if (_tagFilter.value.equals(old, true)) _tagFilter.value = replacement?.trim()?.take(24)
                _updateEvent.send(UpdateEvent.PersonalSaved)
            } catch (_: java.io.IOException) { _updateEvent.send(UpdateEvent.Error) }
        }
    }
    fun setDailyGoal(goal: Int) { viewModelScope.launch { toolsStore?.setDailyGoal(goal) } }
    fun saveView(name: String) {
        val view = SavedListView(name, _searchQuery.value, _activeFilter.value, _quickFilters.value.first, _quickFilters.value.second, _tagFilter.value, _smartFilter.value, _sortOrder.value.name, _scoreRange.value.first, _scoreRange.value.second)
        viewModelScope.launch { toolsStore?.saveView(view) }
    }
    fun deleteView(name: String) { viewModelScope.launch { toolsStore?.deleteView(name) } }
    fun renameView(old: String, name: String) { viewModelScope.launch { toolsStore?.renameView(old, name) } }
    fun moveView(name: String, offset: Int) { viewModelScope.launch { toolsStore?.moveView(name, offset) } }
    fun applyView(view: SavedListView) {
        val value = view.normalized()
        _searchQuery.value = value.query; _activeFilter.value = value.status
        _quickFilters.value = value.favoritesOnly to value.unratedOnly; _tagFilter.value = value.tag
        _smartFilter.value = value.smartFilter
        _scoreRange.value = value.minimumScore to value.maximumScore
        _sortOrder.value = runCatching { MyListSortOrder.valueOf(value.sort) }.getOrDefault(MyListSortOrder.RECENT)
    }

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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _updateEvent.send(UpdateEvent.Error)
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
