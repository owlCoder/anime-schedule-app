package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.AppInlineHeader
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.GlassIconButton
import com.owlcoder.animeschedule.ui.theme.AppDensity
import androidx.compose.material3.rememberModalBottomSheetState

/** Seasonal discovery stays content-first: one header, one selector, one poster grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalOverlay(
    onAnimeClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SeasonalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            BrowseSeasonHeader(
                season = uiState.season,
                year = uiState.year,
                filterActive = uiState.filter.isActive,
                onDismiss = onDismiss,
                onPrevious = {
                    val (season, year) = prevSeason(uiState.season, uiState.year)
                    viewModel.setSeason(season, year)
                },
                onNext = {
                    val (season, year) = nextSeason(uiState.season, uiState.year)
                    viewModel.setSeason(season, year)
                },
                onFilter = { showFilterSheet = true },
            )

            Column(
                modifier = Modifier.padding(horizontal = AppDensity.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SeasonTabRow(
                    currentSeason = uiState.season,
                    currentYear = uiState.year,
                    onSelect = { season, year -> viewModel.setSeason(season, year) },
                )
            }

            when {
                uiState.isLoading -> SeasonalLoadingState()
                uiState.errorRes != null -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.Tune,
                        title = stringResource(R.string.seasonal_error_title),
                        subtitle = stringResource(uiState.errorRes!!),
                        actionLabel = stringResource(R.string.common_retry),
                        onAction = viewModel::load,
                    )
                }
                uiState.filteredItems.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.Tune,
                        title = stringResource(R.string.seasonal_empty_title),
                        subtitle = stringResource(R.string.seasonal_empty_subtitle),
                        actionLabel = stringResource(R.string.seasonal_filter_reset),
                        onAction = viewModel::clearFilter,
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = AppDensity.screenHorizontal,
                        vertical = 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(
                        items = uiState.filteredItems,
                        key = { "season:${it.anilistId}" },
                        contentType = { "seasonal_poster" },
                    ) { item ->
                        SeasonalAnimeCard(
                            item = item,
                            onClick = { onAnimeClick(item.anilistId) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        SeasonalFilterSheet(
            filter = uiState.filter,
            availableGenres = uiState.availableGenres,
            availableFormats = uiState.availableFormats,
            onGenreToggle = viewModel::toggleGenre,
            onFormatToggle = viewModel::toggleFormat,
            onSortChange = viewModel::setSortOrder,
            onClear = viewModel::clearFilter,
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun BrowseSeasonHeader(
    season: com.owlcoder.animeschedule.domain.model.AnimeSeason,
    year: Int,
    filterActive: Boolean,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFilter: () -> Unit,
) {
    AppInlineHeader(
        title = stringResource(R.string.seasonal_title),
        onBack = onDismiss,
        backContentDescription = stringResource(R.string.common_cancel),
        modifier = Modifier.padding(horizontal = 4.dp),
        trailingContent = {
            GlassIconButton(
                icon = Icons.Outlined.Tune,
                contentDescription = stringResource(R.string.seasonal_filter_title),
                onClick = onFilter,
            )
        },
    )
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDensity.screenHorizontal, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${stringResource(season.labelRes())} $year",
                style = MaterialTheme.typography.headlineSmall,
            )
            if (filterActive) {
                Text(
                    text = stringResource(R.string.seasonal_filter_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.NavigateBefore,
            contentDescription = stringResource(R.string.seasonal_title),
            onClick = onPrevious,
        )
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = stringResource(R.string.seasonal_title),
            onClick = onNext,
        )
    }
}

@Composable
private fun ColumnScope.SeasonalLoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(Modifier.size(28.dp))
            Text(
                text = stringResource(R.string.seasonal_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
