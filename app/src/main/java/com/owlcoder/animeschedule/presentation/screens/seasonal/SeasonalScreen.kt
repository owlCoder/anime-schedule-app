package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.iosTween
import com.owlcoder.animeschedule.ui.theme.PillShape

private val FORMAT_LABELS = mapOf(
    "TV" to "TV",
    "TV_SHORT" to "TV Short",
    "MOVIE" to "Movie",
    "SPECIAL" to "Special",
    "OVA" to "OVA",
    "ONA" to "ONA",
    "MUSIC" to "Music",
)

@StringRes
internal fun AnimeSeason.labelRes(): Int = when (this) {
    AnimeSeason.WINTER -> R.string.season_winter
    AnimeSeason.SPRING -> R.string.season_spring
    AnimeSeason.SUMMER -> R.string.season_summer
    AnimeSeason.FALL -> R.string.season_fall
}

@Composable
internal fun SeasonTabRow(
    currentSeason: AnimeSeason,
    currentYear: Int,
    onSelect: (AnimeSeason, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionPolicy.current
    AppMaterialSurface(
        modifier = modifier.fillMaxWidth().height(39.dp),
        material = AppMaterial.Interactive,
        shape = RoundedCornerShape(11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(39.dp).padding(3.dp),
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
                        .height(33.dp)
                        .clickable(role = Role.Tab) { onSelect(season, currentYear) }
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    shape = RoundedCornerShape(8.dp),
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
private fun SeasonLabel(season: AnimeSeason, selected: Boolean, contentColor: Color) {
    Box(Modifier.fillMaxWidth().height(33.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(season.labelRes()),
            style = MaterialTheme.typography.labelLarge,
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
        FORMAT_LABELS[item.format] ?: item.format,
        item.episodes?.let { "$it ep" },
        (item.averageScore ?: item.meanScore)?.let { "★ $it" },
    ).joinToString(" · ")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MediaThumbnail.Large(
            url = item.coverImageUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.70f),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.seasonal_filter_title),
        trailingContent = {
            TextButton(onClick = onClear, enabled = filter.isActive) {
                Text(
                    stringResource(R.string.seasonal_filter_reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            EqualOptionSection(
                title = stringResource(R.string.seasonal_sort_label),
                options = SeasonalSortOrder.entries,
                columns = 3,
                label = { stringResource(it.labelRes) },
                selected = { filter.sortOrder == it },
                onClick = onSortChange,
            )
            if (availableFormats.isNotEmpty()) {
                EqualOptionSection(
                    title = stringResource(R.string.filter_format),
                    options = availableFormats,
                    columns = 2,
                    label = { FORMAT_LABELS[it] ?: it },
                    selected = { it in filter.formats },
                    onClick = onFormatToggle,
                )
            }
            if (availableGenres.isNotEmpty()) {
                EqualOptionSection(
                    title = stringResource(R.string.filter_genre),
                    options = availableGenres,
                    columns = 2,
                    label = { it },
                    selected = { it in filter.genres },
                    onClick = onGenreToggle,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun <T> EqualOptionSection(
    title: String,
    options: List<T>,
    columns: Int,
    label: @Composable (T) -> String,
    selected: (T) -> Boolean,
    onClick: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 2.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        options.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { option ->
                    EqualFilterOption(
                        label = label(option),
                        selected = selected(option),
                        modifier = Modifier.weight(1f),
                        onClick = { onClick(option) },
                    )
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun EqualFilterOption(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "season-filter-color",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "season-filter-fill",
    )
    Surface(
        modifier = modifier.height(38.dp).clickable(onClick = onClick),
        shape = PillShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            0.5.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    fadeIn(animationSpec = motion.iosTween(IosMotion.Quick)) togetherWith
                        fadeOut(animationSpec = motion.iosTween(IosMotion.Quick))
                },
                label = "season-filter-check",
            ) { checked ->
                if (checked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
