package com.owlcoder.animeschedule.presentation.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.discovery.*
import com.owlcoder.animeschedule.domain.model.WatchStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SearchFilterSheet(filter: SearchFilter, formats: List<String>, onTracking: (TrackingFilter) -> Unit, onFormat: (String) -> Unit, onSort: (SearchSort) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit,
    years: List<Int> = emptyList(), onMinimumScore: (Int) -> Unit = {}, onLength: (EpisodeLength) -> Unit = {}, onYear: (Int?) -> Unit = {},
    onMaximumScore: (Int) -> Unit = {}, onWatchStatus: (WatchStatus?) -> Unit = {}) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.discovery_search_filters), trailingContent = {
        AppButton(stringResource(R.string.seasonal_filter_reset), onClear, enabled = filter.isActive, variant = AppButtonVariant.Plain, icon = Icons.Default.RestartAlt)
    }) {
        Column(Modifier.heightIn(max = 590.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).testTag("search-filter-list"), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
                item { Text(stringResource(R.string.discovery_loaded_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { DiscoverySection(stringResource(R.string.discovery_tracking)) }
                items(TrackingFilter.entries.size) { index ->
                    val value = TrackingFilter.entries[index]
                    AppChoiceRow(stringResource(value.labelRes), Icons.Default.Bookmark, filter.tracking == value, { onTracking(value) }, Modifier.testTag("search-tracking-${value.name}"))
                }
                item { DiscoverySection(stringResource(R.string.search_watch_status)) }
                item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscoveryChip(stringResource(R.string.list_all_statuses), Icons.Default.Bookmarks, filter.watchStatus == null, { onWatchStatus(null) }, Modifier.testTag("search-status-ALL"))
                    WatchStatus.entries.filter { it != WatchStatus.NOT_IN_LIST }.forEach { status ->
                        DiscoveryChip(status.displayName(), Icons.Default.Bookmark, filter.watchStatus == status, { onWatchStatus(status) }, Modifier.testTag("search-status-$status"))
                    }
                } }
                item { DiscoverySection(stringResource(R.string.seasonal_sort_label)) }
                items(SearchSort.entries.size) { index ->
                    val value = SearchSort.entries[index]
                    AppChoiceRow(stringResource(value.labelRes), when(value) { SearchSort.RELEVANCE -> Icons.Default.Search; SearchSort.TITLE -> Icons.Default.SortByAlpha; SearchSort.SCORE -> Icons.Default.Star; SearchSort.EPISODES -> Icons.Default.PlayCircle; SearchSort.NEWEST -> Icons.Default.NewReleases }, filter.sort == value, { onSort(value) }, Modifier.testTag("search-sort-${value.name}"))
                }
                item { Text(stringResource(R.string.search_numeric_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { DiscoverySection(stringResource(R.string.discovery_minimum_score)) }
                item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 6, 7, 8, 9).forEach { score -> DiscoveryChip(if (score == 0) stringResource(R.string.discovery_any_score) else stringResource(R.string.search_score_floor, score),
                        Icons.Default.Star, score == filter.minimumScore, { onMinimumScore(score) }, Modifier.testTag("search-score-$score")) }
                } }
                item { DiscoverySection(stringResource(R.string.search_maximum_score)) }
                item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(6, 7, 8, 9, 10).forEach { score -> DiscoveryChip(if (score == 10) stringResource(R.string.discovery_any_score) else stringResource(R.string.search_score_ceiling, score),
                        Icons.Default.StarOutline, score == filter.maximumScore, { onMaximumScore(score) }, Modifier.testTag("search-max-score-$score")) }
                } }
                item { DiscoverySection(stringResource(R.string.discovery_length)) }
                item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EpisodeLength.entries.forEach { value -> DiscoveryChip(stringResource(value.labelRes), Icons.Default.PlayCircle, value == filter.length, { onLength(value) }, Modifier.testTag("search-length-$value")) }
                } }
                val availableYears = (years + listOfNotNull(filter.year)).distinct().sortedDescending()
                item { DiscoverySection(stringResource(R.string.search_year)) }
                item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscoveryChip(stringResource(R.string.search_year_any), Icons.Default.CalendarToday, filter.year == null, { onYear(null) }, Modifier.testTag("search-year-ALL"))
                    availableYears.forEach { value -> DiscoveryChip(value.toString(), Icons.Default.CalendarToday, value == filter.year, { onYear(value) }, Modifier.testTag("search-year-$value")) }
                } }
                val availableFormats = (formats + filter.formats).distinct().sorted()
                if (availableFormats.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiscoverySection(stringResource(R.string.filter_format))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableFormats.forEach { value -> DiscoveryChip(discoveryFormatLabel(value) ?: value, Icons.Default.Movie, value in filter.formats, { onFormat(value) }, Modifier.testTag("search-format-$value"), multiple = true) }
                        }
                    }
                }
            }
            AppButton(stringResource(R.string.seasonal_filter_apply), onDismiss, Modifier.fillMaxWidth(), icon = Icons.Default.Check)
        }
    }
}
