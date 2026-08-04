package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.owlcoder.animeschedule.presentation.components.GlassToolbarButton
import com.owlcoder.animeschedule.presentation.components.GlassToolbarGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalOverlay(
    onAnimeClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SeasonalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppInlineHeader(
                title = "${stringResource(uiState.season.labelRes())} ${uiState.year}",
                onBack = onDismiss,
                backContentDescription = stringResource(R.string.common_cancel),
                trailingContent = {
                    GlassToolbarGroup {
                        GlassToolbarButton(
                            icon = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = "Previous season",
                            onClick = {
                                val result = prevSeason(uiState.season, uiState.year)
                                viewModel.setSeason(result.first, result.second)
                            },
                        )
                        GlassToolbarButton(
                            icon = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Next season",
                            onClick = {
                                val result = nextSeason(uiState.season, uiState.year)
                                viewModel.setSeason(result.first, result.second)
                            },
                        )
                        GlassToolbarButton(
                            icon = Icons.Outlined.Tune,
                            contentDescription = stringResource(R.string.seasonal_filter_title),
                            onClick = { showFilterSheet = true },
                            selected = uiState.filter.isActive,
                        )
                    }
                },
            )

            SeasonTabRow(
                currentSeason = uiState.season,
                currentYear = uiState.year,
                onSelect = { season, year -> viewModel.setSeason(season, year) },
                modifier = Modifier.padding(top = 5.dp),
            )

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
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 26.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
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
private fun ColumnScope.SeasonalLoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.seasonal_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
