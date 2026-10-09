package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.discovery.DiscoveryChip
import com.owlcoder.animeschedule.presentation.screens.search.SearchResultCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalOverlay(
    onAnimeClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SeasonalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val leaveSearch = { focus.clearFocus(); keyboard?.hide(); Unit }

    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.seasonal_title),
        trailingContent = {
            GlassToolbarGroup {
                GlassToolbarButton(
                    icon = if (uiState.listLayout) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = stringResource(if (uiState.listLayout) R.string.discovery_grid else R.string.discovery_list),
                    onClick = { leaveSearch(); viewModel.toggleLayout() },
                    modifier = Modifier.testTag("season-layout"),
                )
                GlassToolbarButton(
                    icon = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.seasonal_filter_title),
                    onClick = { leaveSearch(); showFilterSheet = true },
                    selected = uiState.filter.isActive,
                )
            }
        },
    ) {
        // Keep search in a stable lazy item as loading/filter results change. Scrolling the
        // controls with the results also leaves room for posters on compact and enlarged UIs.
        val contentModifier = Modifier.fillMaxWidth().fillMaxHeight(.88f)
        if (uiState.listLayout) {
            LazyColumn(
                modifier = contentModifier.testTag("season-results-list"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "season-controls", contentType = "controls") {
                    SeasonalControls(uiState, viewModel, onAnimeClick)
                }
                if (uiState.isLoading || uiState.errorRes != null || uiState.filteredItems.isEmpty()) {
                    item(key = "season-state") { SeasonalResultState(uiState, viewModel) }
                } else {
                    items(uiState.filteredItems, key = { it.anilistId }, contentType = { "anime" }) { item ->
                        SearchResultCard(
                            AnimeSearchResult(
                                item.anilistId, item.malId, item.title, null, item.coverImageUrl,
                                item.format, item.seasonYear?.toString(),
                                (item.averageScore ?: item.meanScore)?.toDouble(), item.episodes,
                                item.malId?.let(uiState.malEntriesById::get),
                            ),
                            { onAnimeClick(item.anilistId) }, null, showDivider = false,
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = contentModifier.testTag("season-results-grid"),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "season-controls", span = { GridItemSpan(maxLineSpan) }, contentType = "controls") {
                    SeasonalControls(uiState, viewModel, onAnimeClick)
                }
                if (uiState.isLoading || uiState.errorRes != null || uiState.filteredItems.isEmpty()) {
                    item(key = "season-state", span = { GridItemSpan(maxLineSpan) }) {
                        SeasonalResultState(uiState, viewModel)
                    }
                } else {
                    items(uiState.filteredItems, key = { "season:${it.anilistId}" }, contentType = { "poster" }) { item ->
                        SeasonalAnimePosterTile(
                            item = item,
                            userListEntry = item.malId?.let(uiState.malEntriesById::get),
                            onClick = { onAnimeClick(item.anilistId) },
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
            onRelease = viewModel::setRelease,
            onLength = viewModel::setLength,
            onScore = viewModel::setMinimumScore,
            onClear = viewModel::clearFilter,
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun SeasonalControls(uiState: SeasonalUiState, viewModel: SeasonalViewModel, onAnimeClick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            GlassToolbarButton(
                Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.discovery_previous_season),
                { viewModel.moveSeason(-1) }, Modifier.testTag("season-previous"),
                enabled = uiState.year > 1940 || uiState.season.ordinal > 0,
            )
            Text(uiState.year.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            GlassToolbarButton(
                Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.discovery_next_season),
                { viewModel.moveSeason(1) }, Modifier.testTag("season-next"),
                enabled = uiState.year < LocalDate.now().year + 2 || uiState.season.ordinal < 3,
            )
        }
        SeasonTabRow(uiState.season, uiState.year, { season, year -> viewModel.setSeason(season, year) })
        AppSearchField(
            value = uiState.filter.query, onValueChange = viewModel::setQuery,
            placeholder = stringResource(R.string.seasonal_search), leadingIcon = Icons.Default.Search,
            onClear = { viewModel.setQuery("") }, modifier = Modifier.fillMaxWidth().testTag("season-search-field"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DiscoveryChip(
                stringResource(R.string.seasonal_hide_tracked), Icons.Default.VisibilityOff,
                uiState.filter.hideTracked, viewModel::toggleHideTracked, Modifier.weight(1f), multiple = true,
            )
            GlassToolbarButton(
                Icons.Default.Shuffle, stringResource(R.string.discovery_random_hint),
                { viewModel.randomAnime()?.let { onAnimeClick(it.anilistId) } }, Modifier.testTag("season-random"),
                enabled = !uiState.isLoading && uiState.errorRes == null && uiState.filteredItems.isNotEmpty(),
            )
            if (uiState.filter.isActive) {
                GlassToolbarButton(Icons.Default.RestartAlt, stringResource(R.string.seasonal_filter_reset), viewModel::clearFilter)
            }
        }
        if (!uiState.isLoading && uiState.errorRes == null) {
            val countLabel = if (uiState.filter.isActive) {
                stringResource(R.string.seasonal_filtered_results_count, uiState.filteredItems.size, uiState.allItems.size)
            } else stringResource(R.string.seasonal_results_count, uiState.filteredItems.size)
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(countLabel, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(uiState.filter.sortOrder.labelRes), style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SeasonalResultState(uiState: SeasonalUiState, viewModel: SeasonalViewModel) {
    when {
        uiState.isLoading -> SeasonalLoadingState(Modifier.fillMaxWidth().height(320.dp))
        uiState.errorRes != null -> AppErrorState(
            title = stringResource(R.string.seasonal_error_title), message = stringResource(uiState.errorRes),
            retryLabel = stringResource(R.string.common_retry), onRetry = viewModel::load,
            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
        )
        else -> Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Outlined.Tune, title = stringResource(R.string.seasonal_empty_title),
                subtitle = stringResource(R.string.seasonal_empty_subtitle),
                actionLabel = stringResource(R.string.seasonal_filter_reset), onAction = viewModel::clearFilter,
            )
        }
    }
}

@Composable
private fun SeasonalLoadingState(modifier: Modifier) {
    Box(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 94.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
        ) {
            items(count = 4) {
                Box(Modifier.fillMaxWidth().aspectRatio(.68f).clip(ContinuousRoundedShape(15.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer))
            }
        }
        AppLoadingState(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            label = stringResource(R.string.seasonal_title), message = stringResource(R.string.seasonal_loading_message),
        )
    }
}
