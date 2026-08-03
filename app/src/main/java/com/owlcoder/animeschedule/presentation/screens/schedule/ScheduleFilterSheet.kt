package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    "SPECIAL" to "Specijal",
    "OVA" to "OVA",
    "ONA" to "ONA",
    "MUSIC" to "Music"
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
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = stringResource(R.string.filter_title),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.filter_reset))
                }
            }

            Spacer(Modifier.height(4.dp))

            if (isLoggedIn) {
                InsetGroup {
                    InsetListRow(
                        label = stringResource(R.string.filter_only_my_list),
                        supportingText = stringResource(R.string.filter_only_my_list_subtitle),
                        trailingContent = {
                            Switch(
                                checked = filter.onlyMyList,
                                onCheckedChange = onOnlyMyListChange
                            )
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            if (availableFormats.isNotEmpty()) {
                Text(
                    stringResource(R.string.filter_format),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableFormats.forEach { fmt ->
                        FilterChip(
                            selected = fmt in filter.formats,
                            onClick = { onFormatToggle(fmt) },
                            shape = PillShape,
                            label = { Text(FORMAT_LABELS[fmt] ?: fmt) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (availableGenres.isNotEmpty()) {
                Text(
                    stringResource(R.string.filter_genre),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableGenres.forEach { genre ->
                        FilterChip(
                            selected = genre in filter.genres,
                            onClick = { onGenreToggle(genre) },
                            shape = PillShape,
                            label = { Text(genre) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
