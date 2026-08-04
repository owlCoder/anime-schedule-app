package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.iosTween

private enum class SeasonalContentMode { Loading, Error, Empty, Grid }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalOverlay(
    onAnimeClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SeasonalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val motion = LocalMotionPolicy.current
    var showFilterSheet by remember { mutableStateOf(false) }

    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        showCloseButton = false,
        showDragHandle = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
        ) {
            AppInlineHeader(
                title = "${stringResource(uiState.season.labelRes())} ${uiState.year}",
                modifier = Modifier.padding(bottom = 2.dp),
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
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            )

            val mode = when {
                uiState.isLoading -> SeasonalContentMode.Loading
                uiState.errorRes != null -> SeasonalContentMode.Error
                uiState.filteredItems.isEmpty() -> SeasonalContentMode.Empty
                else -> SeasonalContentMode.Grid
            }
            val contentKey = Triple(uiState.year, uiState.season, mode)

            AnimatedContent(
                targetState = contentKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    val direction = if (targetState.first >= initialState.first) 1 else -1
                    (fadeIn(animationSpec = motion.iosTween(IosMotion.Standard)) +
                        slideInHorizontally(
                            animationSpec = motion.iosTween(IosMotion.Standard),
                            initialOffsetX = { if (motion.animationsEnabled) direction * it / 12 else 0 },
                        )) togetherWith
                        (fadeOut(animationSpec = motion.iosTween(IosMotion.Quick)) +
                            slideOutHorizontally(
                                animationSpec = motion.iosTween(IosMotion.Quick),
                                targetOffsetX = { if (motion.animationsEnabled) -direction * it / 16 else 0 },
                            ))
                },
                label = "seasonal-content",
            ) { (_, _, contentMode) ->
                when (contentMode) {
                    SeasonalContentMode.Loading -> SeasonalLoadingState()
                    SeasonalContentMode.Error -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.Tune,
                            title = stringResource(R.string.seasonal_error_title),
                            subtitle = uiState.errorRes?.let { stringResource(it) }.orEmpty(),
                            actionLabel = stringResource(R.string.common_retry),
                            onAction = viewModel::load,
                        )
                    }
                    SeasonalContentMode.Empty -> Box(
                        modifier = Modifier.fillMaxSize(),
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
                    SeasonalContentMode.Grid -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
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
private fun SeasonalLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
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
