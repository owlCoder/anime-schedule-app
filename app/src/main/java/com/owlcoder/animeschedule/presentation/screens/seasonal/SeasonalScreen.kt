package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.discovery.*

@StringRes
internal fun AnimeSeason.labelRes(): Int = when (this) {
    AnimeSeason.WINTER -> R.string.season_winter
    AnimeSeason.SPRING -> R.string.season_spring
    AnimeSeason.SUMMER -> R.string.season_summer
    AnimeSeason.FALL -> R.string.season_fall
}

private fun AnimeSeason.icon(): ImageVector = when (this) {
    AnimeSeason.WINTER -> Icons.Default.AcUnit
    AnimeSeason.SPRING -> Icons.Default.LocalFlorist
    AnimeSeason.SUMMER -> Icons.Default.WbSunny
    AnimeSeason.FALL -> Icons.Default.Eco
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeasonTabRow(currentSeason: AnimeSeason, currentYear: Int, onSelect: (AnimeSeason, Int) -> Unit, modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth < 280.dp || fontScale > 1.6f -> 1
            maxWidth >= 480.dp && fontScale <= 1.15f -> 4
            else -> 2
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AnimeSeason.entries.chunked(columns).forEach { seasons ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    seasons.forEach { season ->
                        DiscoveryChip(stringResource(season.labelRes()), season.icon(), season == currentSeason,
                            { onSelect(season, currentYear) }, Modifier.weight(1f).testTag("season-${season.name}"))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SeasonalFilterSheet(filter: SeasonalFilter, availableGenres: List<String>, availableFormats: List<String>, onGenreToggle: (String) -> Unit, onFormatToggle: (String) -> Unit, onSortChange: (SeasonalSortOrder) -> Unit, onRelease: (ReleaseFilter) -> Unit, onLength: (EpisodeLength) -> Unit, onScore: (Int) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.seasonal_filter_title), trailingContent = {
        AppButton(stringResource(R.string.seasonal_filter_reset), onClear, enabled = filter.isActive, variant = AppButtonVariant.Plain, icon = Icons.Default.RestartAlt)
    }) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 580.dp).testTag("season-filter-list"), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            item { DiscoverySection(stringResource(R.string.seasonal_sort_label)) }
            items(SeasonalSortOrder.entries.size) { index ->
                val value = SeasonalSortOrder.entries[index]
                AppChoiceRow(stringResource(value.labelRes), when(value) { SeasonalSortOrder.POPULARITY -> Icons.Default.TrendingUp; SeasonalSortOrder.SCORE -> Icons.Default.Star; SeasonalSortOrder.TITLE -> Icons.Default.SortByAlpha }, filter.sortOrder == value, { onSortChange(value) }, Modifier.testTag("season-sort-${value.name}"))
            }
            item { DiscoverySection(stringResource(R.string.discovery_release)) }
            items(ReleaseFilter.entries.size) { index ->
                val value = ReleaseFilter.entries[index]
                AppChoiceRow(stringResource(value.labelRes), Icons.Default.LiveTv, filter.release == value, { onRelease(value) }, Modifier.testTag("season-release-${value.name}"))
            }
            item { DiscoverySection(stringResource(R.string.discovery_length)) }
            items(EpisodeLength.entries.size) { index ->
                val value = EpisodeLength.entries[index]
                AppChoiceRow(stringResource(value.labelRes), Icons.Default.PlayCircle, filter.length == value, { onLength(value) }, Modifier.testTag("season-length-${value.name}"))
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscoverySection(stringResource(R.string.discovery_minimum_score))
                    AppChoiceRow(stringResource(R.string.discovery_any_score), Icons.Default.Star, filter.minimumScore == 0, { onScore(0) }, Modifier.testTag("season-score-0"))
                    listOf(listOf(6, 7), listOf(8, 9)).forEach { scores ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            scores.forEach { value -> DiscoveryChip(stringResource(R.string.discovery_score_value, value), Icons.Default.Star, filter.minimumScore == value, { onScore(value) }, Modifier.weight(1f).testTag("season-score-$value")) }
                        }
                    }
                    Text(stringResource(R.string.discovery_score_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (availableFormats.isNotEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscoverySection(stringResource(R.string.filter_format))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableFormats.forEach { value -> DiscoveryChip(localizedFormatLabel(value), Icons.Default.Movie, value in filter.formats, { onFormatToggle(value) }, multiple = true) }
                    }
                }
            }
            if (availableGenres.isNotEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscoverySection(stringResource(R.string.filter_genre))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableGenres.forEach { value -> DiscoveryChip(localizedGenreLabel(value), Icons.Default.Category, value in filter.genres, { onGenreToggle(value) }, multiple = true) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        AppButton(stringResource(R.string.seasonal_filter_apply), onDismiss, Modifier.fillMaxWidth(), icon = Icons.Default.Check)
    }
}
@Composable
private fun localizedFormatLabel(format: String): String =
    discoveryFormatLabel(format) ?: format

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
