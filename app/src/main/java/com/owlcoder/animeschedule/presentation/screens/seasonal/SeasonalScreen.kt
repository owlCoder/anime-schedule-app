package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
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
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
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
    AppMaterialSurface(
        modifier = modifier.fillMaxWidth().height(38.dp),
        material = AppMaterial.Grouped,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(38.dp).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AnimeSeason.entries.forEach { season ->
                val selected = season == currentSeason
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(role = Role.Tab) { onSelect(season, currentYear) }
                        .semantics { role = Role.Tab },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            shape = RoundedCornerShape(9.dp),
                            tone = GlassTone.Accent,
                            blur = GlassBlur.None,
                        ) {
                            SeasonLabel(season, true)
                        }
                    } else {
                        SeasonLabel(season, false)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonLabel(season: AnimeSeason, selected: Boolean) {
    Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(season.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
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
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MediaThumbnail.Large(
            url = item.coverImageUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.70f),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
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
    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.seasonal_filter_title),
        trailingContent = {
            TextButton(onClick = onClear, enabled = filter.isActive) {
                Text(stringResource(R.string.seasonal_filter_reset), fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FilterSection(stringResource(R.string.seasonal_sort_label)) {
                SeasonalSortOrder.entries.forEach { order ->
                    CompactFilterChip(
                        label = stringResource(order.labelRes),
                        selected = filter.sortOrder == order,
                        onClick = { onSortChange(order) },
                    )
                }
            }
            if (availableFormats.isNotEmpty()) {
                FilterSection(stringResource(R.string.filter_format)) {
                    availableFormats.forEach { format ->
                        CompactFilterChip(
                            label = FORMAT_LABELS[format] ?: format,
                            selected = format in filter.formats,
                            onClick = { onFormatToggle(format) },
                        )
                    }
                }
            }
            if (availableGenres.isNotEmpty()) {
                FilterSection(stringResource(R.string.filter_genre)) {
                    availableGenres.forEach { genre ->
                        CompactFilterChip(
                            label = genre,
                            selected = genre in filter.genres,
                            onClick = { onGenreToggle(genre) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    content: @Composable FlowRowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            content = content,
        )
    }
}

@Composable
private fun CompactFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier.height(34.dp).clickable(onClick = onClick),
        shape = PillShape,
        tone = if (selected) GlassTone.Accent else GlassTone.Neutral,
        blur = GlassBlur.None,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (selected) Icon(Icons.Default.Check, null, Modifier.size(13.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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
