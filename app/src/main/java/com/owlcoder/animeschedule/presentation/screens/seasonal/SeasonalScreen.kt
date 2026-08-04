package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.iosTween
import java.util.Locale

private val FORMAT_LABEL_RES = mapOf(
    "TV" to R.string.format_tv,
    "TV_SHORT" to R.string.format_tv_short,
    "MOVIE" to R.string.format_movie,
    "SPECIAL" to R.string.format_special,
    "OVA" to R.string.format_ova,
    "ONA" to R.string.format_ona,
    "MUSIC" to R.string.format_music,
)

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

private fun formatCommunityScore(score: Int): String =
    String.format(Locale.ROOT, "%.1f", score / 10.0)

@Composable
internal fun SeasonTabRow(
    currentSeason: AnimeSeason,
    currentYear: Int,
    onSelect: (AnimeSeason, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionPolicy.current
    AppMaterialSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        material = AppMaterial.Interactive,
        shape = ContinuousRoundedShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            AnimeSeason.entries.forEach { season ->
                val isSelected = season == currentSeason
                val fill by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = motion.iosTween(IosMotion.Standard),
                    label = "season-tab-fill",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = motion.iosTween(IosMotion.Standard),
                    label = "season-tab-color",
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable(role = Role.Tab) { onSelect(season, currentYear) }
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    shape = ContinuousRoundedShape(11.dp),
                    color = fill,
                    contentColor = contentColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    SeasonLabel(season, isSelected, contentColor)
                }
            }
        }
    }
}

@Composable
private fun SeasonLabel(
    season: AnimeSeason,
    selected: Boolean,
    contentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = season.icon(),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = contentColor,
        )
        Text(
            text = stringResource(season.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SeasonalAnimeCard(
    item: SeasonalAnimeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val meta = listOfNotNull(
        localizedFormatLabel(item.format),
        item.episodes?.let { "$it ep" },
        (item.averageScore ?: item.meanScore)?.let { "★ ${formatCommunityScore(it)}" },
    ).joinToString(" · ")

    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MediaThumbnail.Large(
            url = item.coverImageUrl,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SeasonalFilterSheet(
    filter: SeasonalFilter,
    availableGenres: List<String>,
    availableFormats: List<String>,
    onGenreToggle: (String) -> Unit,
    onFormatToggle: (String) -> Unit,
    onSortChange: (SeasonalSortOrder) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectionCount = filter.genres.size + filter.formats.size

    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.seasonal_filter_title),
        trailingContent = {
            TextButton(onClick = onClear, enabled = filter.isActive) {
                Text(
                    text = stringResource(R.string.seasonal_filter_reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (filter.isActive) {
                AppMaterialSurface(
                    modifier = Modifier.fillMaxWidth(),
                    material = AppMaterial.Interactive,
                    shape = ContinuousRoundedShape(13.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (selectionCount > 0) {
                                stringResource(R.string.seasonal_filter_selected_count, selectionCount)
                            } else {
                                stringResource(R.string.seasonal_filter_custom_sort)
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FilterSectionTitle(stringResource(R.string.seasonal_sort_label))
                SortSegmentedControl(
                    selected = filter.sortOrder,
                    onSelect = onSortChange,
                )

                if (availableFormats.isNotEmpty()) {
                    FilterSectionTitle(stringResource(R.string.filter_format))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        availableFormats.forEach { format ->
                            CompactFilterChip(
                                label = localizedFormatLabel(format),
                                selected = format in filter.formats,
                                onClick = { onFormatToggle(format) },
                            )
                        }
                    }
                }

                if (availableGenres.isNotEmpty()) {
                    FilterSectionTitle(stringResource(R.string.filter_genre))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        availableGenres.forEach { genre ->
                            CompactFilterChip(
                                label = localizedGenreLabel(genre),
                                selected = genre in filter.genres,
                                onClick = { onGenreToggle(genre) },
                            )
                        }
                    }
                }
            }

            AppButton(
                label = stringResource(R.string.seasonal_filter_apply),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 2.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SortSegmentedControl(
    selected: SeasonalSortOrder,
    onSelect: (SeasonalSortOrder) -> Unit,
) {
    val motion = LocalMotionPolicy.current
    AppMaterialSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        material = AppMaterial.Interactive,
        shape = ContinuousRoundedShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            SeasonalSortOrder.entries.forEach { option ->
                val isSelected = selected == option
                val container by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = motion.iosTween(IosMotion.Standard),
                    label = "seasonal-sort-fill",
                )
                val content by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = motion.iosTween(IosMotion.Standard),
                    label = "seasonal-sort-color",
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable(role = Role.RadioButton) { onSelect(option) }
                        .semantics { selected = isSelected },
                    shape = ContinuousRoundedShape(11.dp),
                    color = container,
                    contentColor = content,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = content,
                            )
                        }
                        Text(
                            text = stringResource(option.labelRes),
                            modifier = Modifier.padding(start = if (isSelected) 4.dp else 0.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "seasonal-filter-chip-color",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "seasonal-filter-chip-fill",
    )

    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected },
        shape = ContinuousRoundedShape(11.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = 0.6.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun localizedFormatLabel(format: String): String =
    FORMAT_LABEL_RES[format]?.let { stringResource(it) } ?: format

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

internal fun prevSeason(season: AnimeSeason, year: Int): Pair<AnimeSeason, Int> {
    val seasons = AnimeSeason.entries
    val idx = seasons.indexOf(season)
    return if (idx == 0) Pair(seasons.last(), year - 1) else Pair(seasons[idx - 1], year)
}

internal fun nextSeason(season: AnimeSeason, year: Int): Pair<AnimeSeason, Int> {
    val seasons = AnimeSeason.entries
    val idx = seasons.indexOf(season)
    return if (idx == seasons.lastIndex) Pair(seasons.first(), year + 1) else Pair(seasons[idx + 1], year)
}
