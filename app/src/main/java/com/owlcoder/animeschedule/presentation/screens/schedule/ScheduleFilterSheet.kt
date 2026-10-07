package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.discovery.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleFilterSheet(
    filter: ScheduleFilter,
    availableGenres: List<String>, availableFormats: List<String>, isLoggedIn: Boolean,
    onOnlyMyListChange: (Boolean) -> Unit, onUpcomingChange: (Boolean) -> Unit,
    onGenreToggle: (String) -> Unit, onFormatToggle: (String) -> Unit,
    onClear: () -> Unit, onDismiss: () -> Unit,
    onHideWatchedChange: (Boolean) -> Unit = {}, onFavoritesChange: (Boolean) -> Unit = {},
) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.filter_title), trailingContent = {
        AppButton(stringResource(R.string.filter_reset), onClear, enabled = filter.isActive, variant = AppButtonVariant.Plain, icon = Icons.Default.RestartAlt)
    }) {
        Column(Modifier.heightIn(max = 590.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(Modifier.weight(1f, fill = false).testTag("schedule-filter-list"), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
                item {
                    AppChoiceRow(stringResource(R.string.schedule_upcoming_only), Icons.Default.Schedule, filter.upcomingOnly,
                        { onUpcomingChange(!filter.upcomingOnly) }, subtitle = stringResource(R.string.schedule_upcoming_hint), selectionRole = Role.Checkbox)
                }
                if (isLoggedIn) item {
                    AppChoiceRow(stringResource(R.string.filter_only_my_list), Icons.Default.Bookmarks, filter.onlyMyList,
                        { onOnlyMyListChange(!filter.onlyMyList) }, subtitle = stringResource(R.string.filter_only_my_list_subtitle), selectionRole = Role.Checkbox)
                }
                item {
                    AppChoiceRow(stringResource(R.string.schedule_hide_watched), Icons.Default.VisibilityOff, filter.hideWatched,
                        { onHideWatchedChange(!filter.hideWatched) }, Modifier.testTag("schedule-hide-watched"),
                        subtitle = stringResource(R.string.schedule_hide_watched_hint), selectionRole = Role.Checkbox)
                }
                item {
                    AppChoiceRow(stringResource(R.string.schedule_favorites_only), Icons.Default.Favorite, filter.favoritesOnly,
                        { onFavoritesChange(!filter.favoritesOnly) }, Modifier.testTag("schedule-favorites-only"),
                        subtitle = stringResource(R.string.schedule_favorites_hint), selectionRole = Role.Checkbox)
                }
                if (availableFormats.isNotEmpty()) {
                    item { DiscoverySection(stringResource(R.string.filter_format)) }
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableFormats.forEach { format -> DiscoveryChip(discoveryFormatLabel(format) ?: format, Icons.Default.LiveTv, format in filter.formats, { onFormatToggle(format) }, Modifier.testTag("schedule-format-$format"), multiple = true) }
                    } }
                }
                if (availableGenres.isNotEmpty()) {
                    item { DiscoverySection(stringResource(R.string.filter_genre)) }
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableGenres.forEach { genre -> DiscoveryChip(localizedGenreLabel(genre), Icons.Default.Category, genre in filter.genres, { onGenreToggle(genre) }, multiple = true) }
                    } }
                }
            }
            AppButton(stringResource(R.string.seasonal_filter_apply), onDismiss, Modifier.fillMaxWidth(), icon = Icons.Default.Check)
        }
    }
}

@Composable
private fun localizedGenreLabel(genre: String): String = when (genre.lowercase()) {
    "action" -> stringResource(R.string.genre_action)
    "adventure" -> stringResource(R.string.genre_adventure)
    "comedy" -> stringResource(R.string.genre_comedy)
    "drama" -> stringResource(R.string.genre_drama)
    "ecchi" -> stringResource(R.string.genre_ecchi)
    "fantasy" -> stringResource(R.string.genre_fantasy)
    "hentai" -> stringResource(R.string.genre_hentai)
    "horror" -> stringResource(R.string.genre_horror)
    "mahou shoujo" -> stringResource(R.string.genre_mahou_shoujo)
    "mecha" -> stringResource(R.string.genre_mecha)
    "music" -> stringResource(R.string.genre_music)
    "mystery" -> stringResource(R.string.genre_mystery)
    "psychological" -> stringResource(R.string.genre_psychological)
    "romance" -> stringResource(R.string.genre_romance)
    "sci-fi" -> stringResource(R.string.genre_scifi)
    "slice of life" -> stringResource(R.string.genre_slice_of_life)
    "sports" -> stringResource(R.string.genre_sports)
    "supernatural" -> stringResource(R.string.genre_supernatural)
    "thriller" -> stringResource(R.string.genre_thriller)
    else -> genre
}
