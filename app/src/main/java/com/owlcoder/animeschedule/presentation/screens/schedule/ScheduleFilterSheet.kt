package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.AppSwitch
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.InsetListRow
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.iosTween

private val FORMAT_LABEL_RES = mapOf(
    "TV" to R.string.format_tv,
    "TV_SHORT" to R.string.format_tv_short,
    "MOVIE" to R.string.format_movie,
    "SPECIAL" to R.string.format_special,
    "OVA" to R.string.format_ova,
    "ONA" to R.string.format_ona,
    "MUSIC" to R.string.format_music,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleFilterSheet(
    filter: ScheduleFilter,
    availableGenres: List<String>,
    availableFormats: List<String>,
    isLoggedIn: Boolean,
    onOnlyMyListChange: (Boolean) -> Unit,
    onGenreToggle: (String) -> Unit,
    onFormatToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectionCount = filter.genres.size + filter.formats.size + if (filter.onlyMyList) 1 else 0

    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.filter_title),
        trailingContent = {
            TextButton(
                onClick = onClear,
                enabled = filter.isActive,
            ) {
                Text(
                    text = stringResource(R.string.filter_reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 590.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (filter.isActive) {
                AppMaterialSurface(
                    modifier = Modifier.fillMaxWidth(),
                    material = AppMaterial.Interactive,
                    shape = ContinuousRoundedShape(13.dp),
                ) {
                    Text(
                        text = stringResource(R.string.seasonal_filter_selected_count, selectionCount),
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
                if (isLoggedIn) {
                    InsetGroup {
                        InsetListRow(
                            label = stringResource(R.string.filter_only_my_list),
                            supportingText = stringResource(R.string.filter_only_my_list_subtitle),
                            trailingContent = {
                                AppSwitch(
                                    checked = filter.onlyMyList,
                                    onCheckedChange = onOnlyMyListChange,
                                )
                            },
                        )
                    }
                }

                if (availableFormats.isNotEmpty()) {
                    FilterSectionTitle(stringResource(R.string.filter_format))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        availableFormats.forEach { format ->
                            CompactFilterChip(
                                label = formatLabel(format),
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
private fun CompactFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "schedule-filter-chip-color",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "schedule-filter-chip-fill",
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
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
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
private fun formatLabel(format: String): String =
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
