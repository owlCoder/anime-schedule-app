package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.ui.theme.PillShape

private data class ChangelogEntry(
    val version: String,
    val dateRes: Int,
    val changeRes: List<Int>
)

private val changelogEntries = listOf(
    ChangelogEntry(
        version = "2.0.1",
        dateRes = R.string.cl_201_date,
        changeRes = listOf(
            R.string.cl_201_1, R.string.cl_201_2, R.string.cl_201_3, R.string.cl_201_4,
            R.string.cl_201_5, R.string.cl_201_6, R.string.cl_201_7, R.string.cl_201_8
        )
    ),
    ChangelogEntry(
        version = "1.5.2",
        dateRes = R.string.cl_152_date,
        changeRes = listOf(R.string.cl_152_1, R.string.cl_152_2, R.string.cl_152_3, R.string.cl_152_4, R.string.cl_152_5)
    ),
    ChangelogEntry(
        version = "1.2.54",
        dateRes = R.string.cl_154_date,
        changeRes = listOf(R.string.cl_154_1, R.string.cl_154_2, R.string.cl_154_3, R.string.cl_154_4, R.string.cl_154_5)
    ),
    ChangelogEntry(
        version = "1.1",
        dateRes = R.string.cl_11_date,
        changeRes = listOf(R.string.cl_11_1, R.string.cl_11_2, R.string.cl_11_3, R.string.cl_11_4, R.string.cl_11_5, R.string.cl_11_6)
    ),
    ChangelogEntry(
        version = "1.0.4",
        dateRes = R.string.cl_104_date,
        changeRes = listOf(R.string.cl_104_1, R.string.cl_104_2, R.string.cl_104_3)
    ),
    ChangelogEntry(
        version = "1.0.3",
        dateRes = R.string.cl_103_date,
        changeRes = listOf(R.string.cl_103_1, R.string.cl_103_2, R.string.cl_103_3)
    ),
    ChangelogEntry(
        version = "1.0.2",
        dateRes = R.string.cl_102_date,
        changeRes = listOf(R.string.cl_102_1, R.string.cl_102_2, R.string.cl_102_3)
    ),
    ChangelogEntry(
        version = "1.0.1",
        dateRes = R.string.cl_101_date,
        changeRes = listOf(R.string.cl_101_1, R.string.cl_101_2, R.string.cl_101_3)
    ),
    ChangelogEntry(
        version = "1.0.0",
        dateRes = R.string.cl_100_date,
        changeRes = listOf(R.string.cl_100_1, R.string.cl_100_2, R.string.cl_100_3)
    )
)

/**
 * Changelog as a bottom-sheet overlay (mirrors Search/Notifications) instead of a full nav
 * destination — quick, infrequent surface that doesn't need its own back stack entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Text(
            stringResource(R.string.changelog_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .padding(horizontal = 16.dp)
        ) {
            itemsIndexed(changelogEntries) { index, entry ->
                ChangelogCard(entry = entry, expandedByDefault = index == 0)
                Spacer(Modifier.height(14.dp))
            }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogEntry, expandedByDefault: Boolean) {
    var expanded by remember { mutableStateOf(expandedByDefault) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        stringResource(R.string.changelog_version_prefix, entry.version),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(entry.dateRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded)
                        stringResource(R.string.cd_collapse)
                    else
                        stringResource(R.string.cd_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp)) {
                    entry.changeRes.forEach { resId ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp, end = 12.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                stringResource(resId),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
