package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.domain.repository.SeasonalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.Month
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SeasonalSortOrder(@androidx.annotation.StringRes val labelRes: Int) {
    POPULARITY(com.owlcoder.animeschedule.R.string.seasonal_sort_popularity),
    SCORE(com.owlcoder.animeschedule.R.string.seasonal_sort_score),
    TITLE(com.owlcoder.animeschedule.R.string.seasonal_sort_title),
}

data class SeasonalFilter(
    val query: String = "",
    val hideTracked: Boolean = false,
    val genres: Set<String> = emptySet(),
    val formats: Set<String> = emptySet(),
    val sortOrder: SeasonalSortOrder = SeasonalSortOrder.POPULARITY,
) {
    val isActive: Boolean get() = query.isNotBlank() || hideTracked || genres.isNotEmpty() || formats.isNotEmpty()
}

data class SeasonalUiState(
    val season: AnimeSeason = currentSeason(),
    val year: Int = LocalDate.now().year,
    val allItems: List<SeasonalAnimeItem> = emptyList(),
    val filteredItems: List<SeasonalAnimeItem> = emptyList(),
    val malEntriesById: Map<Int, MalListEntry> = emptyMap(),
    val isLoading: Boolean = false,
    @androidx.annotation.StringRes val errorRes: Int? = null,
    val filter: SeasonalFilter = SeasonalFilter(),
    val availableGenres: List<String> = emptyList(),
    val availableFormats: List<String> = emptyList(),
)

private fun currentSeason(): AnimeSeason {
    return when (LocalDate.now().month) {
        Month.JANUARY, Month.FEBRUARY, Month.MARCH -> AnimeSeason.WINTER
        Month.APRIL, Month.MAY, Month.JUNE -> AnimeSeason.SPRING
        Month.JULY, Month.AUGUST, Month.SEPTEMBER -> AnimeSeason.SUMMER
        else -> AnimeSeason.FALL
    }
}

internal fun List<SeasonalAnimeItem>.applyFilter(filter: SeasonalFilter, trackedIds: Set<Int> = emptySet()): List<SeasonalAnimeItem> {
    val query = filter.query.trim()
    var result = filter { item ->
        (query.isBlank() || item.title.contains(query, ignoreCase = true)) && (!filter.hideTracked || item.malId !in trackedIds)
    }
    if (filter.genres.isNotEmpty()) {
        result = result.filter { item -> item.genres.any { it in filter.genres } }
    }
    if (filter.formats.isNotEmpty()) {
        result = result.filter { item -> item.format in filter.formats }
    }
    return when (filter.sortOrder) {
        SeasonalSortOrder.POPULARITY -> result
        SeasonalSortOrder.SCORE -> result.sortedByDescending {
            it.averageScore ?: it.meanScore ?: 0
        }
        SeasonalSortOrder.TITLE -> result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }
}

@HiltViewModel
class SeasonalViewModel @Inject constructor(
    private val seasonalRepository: SeasonalRepository,
    private val malRepository: MalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeasonalUiState())
    val uiState: StateFlow<SeasonalUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        observeMalList()
        load()
    }

    fun load(season: AnimeSeason? = null, year: Int? = null) {
        val targetSeason = season ?: _uiState.value.season
        val targetYear = year ?: _uiState.value.year
        _uiState.update {
            it.copy(
                season = targetSeason,
                year = targetYear,
                isLoading = true,
                errorRes = null,
            )
        }
        // Switching seasons quickly must not let a slow earlier response overwrite a later one.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            when (val result = seasonalRepository.getSeasonalAnime(targetSeason, targetYear)) {
                is AppResult.Success -> {
                    val items = result.data
                    val genres = items.flatMap { it.genres }.distinct().sorted()
                    val formats = items.mapNotNull { it.format }.distinct().sorted()
                    val currentFilter = _uiState.value.filter
                    _uiState.update { state ->
                        state.copy(
                            allItems = items,
                            filteredItems = items.applyFilter(currentFilter, state.malEntriesById.keys),
                            isLoading = false,
                            availableGenres = genres,
                            availableFormats = formats,
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorRes = com.owlcoder.animeschedule.R.string.error_load_season,
                    )
                }
            }
        }
    }

    fun setSeason(season: AnimeSeason, year: Int) = load(season, year)

    fun toggleGenre(genre: String) = updateFilter { filter ->
        filter.copy(
            genres = if (genre in filter.genres) {
                filter.genres - genre
            } else {
                filter.genres + genre
            },
        )
    }

    fun toggleFormat(format: String) = updateFilter { filter ->
        filter.copy(
            formats = if (format in filter.formats) {
                filter.formats - format
            } else {
                filter.formats + format
            },
        )
    }

    fun setQuery(query: String) = updateFilter { it.copy(query = query) }
    fun toggleHideTracked() = updateFilter { it.copy(hideTracked = !it.hideTracked) }

    fun setSortOrder(order: SeasonalSortOrder) = updateFilter { it.copy(sortOrder = order) }

    fun clearFilter() = updateFilter { SeasonalFilter(sortOrder = it.sortOrder) }

    private fun observeMalList() {
        viewModelScope.launch {
            malRepository.getUserList().collect { entries ->
                val entriesById = entries.associateBy { it.animeId }
                _uiState.update { it.copy(malEntriesById = entriesById, filteredItems = it.allItems.applyFilter(it.filter, entriesById.keys)) }
            }
        }
    }

    private fun updateFilter(transform: (SeasonalFilter) -> SeasonalFilter) {
        _uiState.update { state ->
            val newFilter = transform(state.filter)
            state.copy(
                filter = newFilter,
                filteredItems = state.allItems.applyFilter(newFilter, state.malEntriesById.keys),
            )
        }
    }
}
