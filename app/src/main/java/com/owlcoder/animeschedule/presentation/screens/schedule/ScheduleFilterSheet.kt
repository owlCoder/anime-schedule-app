package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.InsetListRow
import com.owlcoder.animeschedule.ui.theme.PillShape

private val FORMAT_LABELS = mapOf(
    "TV" to "TV",
    "TV_SHORT" to "TV Short",
    "MOVIE" to "Film",
    "SPECIAL" to "Special",
    "OVA" to "OVA",
    "ONA" to "ONA",
    "MUSIC" to "Music",
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
    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        title = stringResource(R.string.filter_title),
        trailingContent = {
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.filter_reset))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (isLoggedIn) {
                InsetGroup {
                    InsetListRow(
                        label = stringResource(R.string.filter_only_my_list),
                        supportingText = stringResource(R.string.filter_only_my_list_subtitle),
                        trailingContent = {
                            Switch(
                                checked = filter.onlyMyList,
                                onCheckedChange = onOnlyMyListChange,
                            )
                        },
                    )
                }
            }

            if (availableFormats.isNotEmpty()) {
                FilterSection(title = stringResource(R.string.filter_format)) {
                    availableFormats.forEach { format ->
                        CompactScheduleFilterChip(
                            label = FORMAT_LABELS[format] ?: format,
                            selected = format in filter.formats,
                            onClick = { onFormatToggle(format) },
                        )
                    }
                }
            }

            if (availableGenres.isNotEmpty()) {
                FilterSection(title = stringResource(R.string.filter_genre)) {
                    availableGenres.forEach { genre ->
                        CompactScheduleFilterChip(
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
    content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun CompactScheduleFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
    }
    Row(
        modifier = Modifier
            .sizeIn(minHeight = 36.dp)
            .clip(PillShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else Color.Transparent,
            )
            .border(1.dp, borderColor, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
