package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.SmartListFilter
import com.owlcoder.animeschedule.presentation.components.*

/** Secondary library controls share one scrollable panel; the list keeps its primary actions. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun MyListFiltersSheet(
    uiState: MyListUiState,
    onFavorites: () -> Unit,
    onUnrated: () -> Unit,
    onSmart: () -> Unit,
    onRating: () -> Unit,
    onTags: () -> Unit,
    onViews: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(onDismiss, title = stringResource(R.string.list_filters_title)) {
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 570.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(stringResource(R.string.list_filters_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { AppChoiceRow(stringResource(R.string.favorites), Icons.Default.Star, uiState.favoritesOnly,
                onFavorites, Modifier.testTag("list-favorites-filter")) }
            item { AppChoiceRow(stringResource(R.string.list_unrated), Icons.Default.StarBorder, uiState.unratedOnly,
                onUnrated, Modifier.testTag("list-unrated-filter")) }
            item { AppChoiceRow(stringResource(if (uiState.smartFilter == SmartListFilter.ALL) R.string.smart_filters else uiState.smartFilter.labelRes()),
                Icons.Default.FilterAlt, uiState.smartFilter != SmartListFilter.ALL, onSmart, Modifier.testTag("list-smart-filter")) }
            item { AppChoiceRow(if (uiState.scoreRange == (0 to 10)) stringResource(R.string.rating_filter)
                else stringResource(R.string.rating_value, uiState.scoreRange.first, uiState.scoreRange.second),
                Icons.Default.StarHalf, uiState.scoreRange != (0 to 10), onRating, Modifier.testTag("list-rating-filter")) }
            item { AppChoiceRow(uiState.activeTag ?: stringResource(R.string.personal_tags), Icons.Default.Label,
                uiState.activeTag != null, onTags, Modifier.testTag("list-tags-filter")) }
            item { AppButton(stringResource(R.string.saved_list_views), onViews, Modifier.fillMaxWidth().testTag("list-saved-views"),
                icon = Icons.Default.Bookmarks, variant = AppButtonVariant.Secondary) }
            item { AppButton(stringResource(R.string.list_clear_filters), onClear, Modifier.fillMaxWidth(),
                icon = Icons.Default.FilterAltOff, variant = AppButtonVariant.Plain) }
        }
        AppButton(stringResource(R.string.list_show_results, uiState.entries.size), onDismiss,
            Modifier.fillMaxWidth().testTag("list-filters-apply"), icon = Icons.Default.Check)
    }
}
