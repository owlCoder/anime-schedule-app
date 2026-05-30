package rs.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.domain.model.AnimeSeason
import rs.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import rs.owlcoder.animeschedule.domain.usecase.GetSeasonalAnimeUseCase
import java.time.LocalDate
import java.time.Month
import javax.inject.Inject

enum class SeasonalSortOrder(val label: String) {
    POPULARITY("Popularnost"),
    SCORE("Ocena"),
    TITLE("Naziv")
}

data class SeasonalFilter(
    val genres: Set<String> = emptySet(),
    val formats: Set<String> = emptySet(),
    val sortOrder: SeasonalSortOrder = SeasonalSortOrder.POPULARITY
) {
    val isActive: Boolean get() = genres.isNotEmpty() || formats.isNotEmpty()
}

data class SeasonalUiState(
    val season: AnimeSeason = currentSeason(),
    val year: Int = LocalDate.now().year,
    val allItems: List<SeasonalAnimeItem> = emptyList(),
    val filteredItems: List<SeasonalAnimeItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filter: SeasonalFilter = SeasonalFilter(),
    val availableGenres: List<String> = emptyList(),
    val availableFormats: List<String> = emptyList()
)

private fun currentSeason(): AnimeSeason {
    return when (LocalDate.now().month) {
        Month.JANUARY, Month.FEBRUARY, Month.MARCH -> AnimeSeason.WINTER
        Month.APRIL, Month.MAY, Month.JUNE -> AnimeSeason.SPRING
        Month.JULY, Month.AUGUST, Month.SEPTEMBER -> AnimeSeason.SUMMER
        else -> AnimeSeason.FALL
    }
}

private fun List<SeasonalAnimeItem>.applyFilter(filter: SeasonalFilter): List<SeasonalAnimeItem> {
    var result = this
    if (filter.genres.isNotEmpty()) result = result.filter { item -> item.genres.any { it in filter.genres } }
    if (filter.formats.isNotEmpty()) result = result.filter { item -> item.format in filter.formats }
    return when (filter.sortOrder) {
        SeasonalSortOrder.POPULARITY -> result
        SeasonalSortOrder.SCORE -> result.sortedByDescending { it.averageScore ?: it.meanScore ?: 0 }
        SeasonalSortOrder.TITLE -> result.sortedBy { it.title }
    }
}

@HiltViewModel
class SeasonalViewModel @Inject constructor(
    private val getSeasonalAnimeUseCase: GetSeasonalAnimeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeasonalUiState())
    val uiState: StateFlow<SeasonalUiState> = _uiState.asStateFlow()

    init { load() }

    fun load(season: AnimeSeason? = null, year: Int? = null) {
        val targetSeason = season ?: _uiState.value.season
        val targetYear = year ?: _uiState.value.year
        _uiState.update { it.copy(season = targetSeason, year = targetYear, isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getSeasonalAnimeUseCase(targetSeason, targetYear)) {
                is AppResult.Success -> {
                    val items = result.data
                    val genres = items.flatMap { it.genres }.distinct().sorted()
                    val formats = items.mapNotNull { it.format }.distinct().sorted()
                    val currentFilter = _uiState.value.filter
                    _uiState.update { state ->
                        state.copy(
                            allItems = items,
                            filteredItems = items.applyFilter(currentFilter),
                            isLoading = false,
                            availableGenres = genres,
                            availableFormats = formats
                        )
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = "Greška pri učitavanju sezone") }
            }
        }
    }

    fun setSeason(season: AnimeSeason, year: Int) = load(season, year)

    fun toggleGenre(genre: String) = updateFilter { f ->
        f.copy(genres = if (genre in f.genres) f.genres - genre else f.genres + genre)
    }

    fun toggleFormat(format: String) = updateFilter { f ->
        f.copy(formats = if (format in f.formats) f.formats - format else f.formats + format)
    }

    fun setSortOrder(order: SeasonalSortOrder) = updateFilter { it.copy(sortOrder = order) }

    fun clearFilter() = updateFilter { SeasonalFilter(sortOrder = it.sortOrder) }

    private fun updateFilter(transform: (SeasonalFilter) -> SeasonalFilter) {
        _uiState.update { state ->
            val newFilter = transform(state.filter)
            state.copy(filter = newFilter, filteredItems = state.allItems.applyFilter(newFilter))
        }
    }
}
