package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.AppSwitch
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

@OptIn(ExperimentalMaterial3Api::class)
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.filter_title),
        trailingContent = {
            TextButton(onClick = onClear, enabled = filter.isActive) {
                Text(stringResource(R.string.filter_reset), fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                EqualOptionGrid(
                    title = stringResource(R.string.filter_format),
                    options = availableFormats,
                    label = { FORMAT_LABELS[it] ?: it },
                    selected = { it in filter.formats },
                    onClick = onFormatToggle,
                )
            }

            if (availableGenres.isNotEmpty()) {
                EqualOptionGrid(
                    title = stringResource(R.string.filter_genre),
                    options = availableGenres,
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
private fun <T> EqualOptionGrid(
    title: String,
    options: List<T>,
    label: (T) -> String,
    selected: (T) -> Boolean,
    onClick: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 2.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        options.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { option ->
                    EqualOption(
                        label = label(option),
                        selected = selected(option),
                        modifier = Modifier.weight(1f),
                        onClick = { onClick(option) },
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EqualOption(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.height(38.dp).clickable(onClick = onClick),
        shape = PillShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
        border = BorderStroke(
            0.5.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
