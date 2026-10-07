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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SearchFilterSheet(filter: SearchFilter, formats: List<String>, onTracking: (TrackingFilter) -> Unit, onFormat: (String) -> Unit, onSort: (SearchSort) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.discovery_search_filters), trailingContent = {
        AppButton(stringResource(R.string.seasonal_filter_reset), onClear, enabled = filter.isActive, variant = AppButtonVariant.Plain, icon = Icons.Default.RestartAlt)
    }) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 580.dp).testTag("search-filter-list"), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            item { Text(stringResource(R.string.discovery_loaded_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { DiscoverySection(stringResource(R.string.discovery_tracking)) }
            items(TrackingFilter.entries.size) { index ->
                val value = TrackingFilter.entries[index]
                AppChoiceRow(stringResource(value.labelRes), Icons.Default.Bookmark, filter.tracking == value, { onTracking(value) }, Modifier.testTag("search-tracking-${value.name}"))
            }
            item { DiscoverySection(stringResource(R.string.seasonal_sort_label)) }
            items(SearchSort.entries.size) { index ->
                val value = SearchSort.entries[index]
                AppChoiceRow(stringResource(value.labelRes), when(value) { SearchSort.RELEVANCE -> Icons.Default.Search; SearchSort.TITLE -> Icons.Default.SortByAlpha; SearchSort.SCORE -> Icons.Default.Star; SearchSort.EPISODES -> Icons.Default.PlayCircle }, filter.sort == value, { onSort(value) }, Modifier.testTag("search-sort-${value.name}"))
            }
            if (formats.isNotEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscoverySection(stringResource(R.string.filter_format))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        formats.forEach { value -> DiscoveryChip(discoveryFormatLabel(value) ?: value, Icons.Default.Movie, value in filter.formats, { onFormat(value) }, Modifier.testTag("search-format-$value"), multiple = true) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        AppButton(stringResource(R.string.seasonal_filter_apply), onDismiss, Modifier.fillMaxWidth(), icon = Icons.Default.Check)
    }
}
