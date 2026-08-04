package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.InsetGroup

private data class ChangelogEntry(
    val version: String,
    val dateRes: Int,
    val changeRes: List<Int>,
)

private val changelogEntries = listOf(
    ChangelogEntry("3.5", R.string.cl_35_date, listOf(R.string.cl_35_1)),
    ChangelogEntry("3.2", R.string.cl_32_date, listOf(R.string.cl_32_1)),
    ChangelogEntry(
        "3.1", R.string.cl_31_date,
        listOf(R.string.cl_31_1, R.string.cl_31_2, R.string.cl_31_3, R.string.cl_31_4, R.string.cl_31_5, R.string.cl_31_6, R.string.cl_31_7, R.string.cl_31_8),
    ),
    ChangelogEntry(
        "2.0.1", R.string.cl_201_date,
        listOf(R.string.cl_201_1, R.string.cl_201_2, R.string.cl_201_3, R.string.cl_201_4, R.string.cl_201_5, R.string.cl_201_6, R.string.cl_201_7, R.string.cl_201_8),
    ),
    ChangelogEntry("1.5.2", R.string.cl_152_date, listOf(R.string.cl_152_1, R.string.cl_152_2, R.string.cl_152_3, R.string.cl_152_4, R.string.cl_152_5)),
    ChangelogEntry("1.2.54", R.string.cl_154_date, listOf(R.string.cl_154_1, R.string.cl_154_2, R.string.cl_154_3, R.string.cl_154_4, R.string.cl_154_5)),
    ChangelogEntry("1.1", R.string.cl_11_date, listOf(R.string.cl_11_1, R.string.cl_11_2, R.string.cl_11_3, R.string.cl_11_4, R.string.cl_11_5, R.string.cl_11_6)),
    ChangelogEntry("1.0.4", R.string.cl_104_date, listOf(R.string.cl_104_1, R.string.cl_104_2, R.string.cl_104_3)),
    ChangelogEntry("1.0.3", R.string.cl_103_date, listOf(R.string.cl_103_1, R.string.cl_103_2, R.string.cl_103_3)),
    ChangelogEntry("1.0.2", R.string.cl_102_date, listOf(R.string.cl_102_1, R.string.cl_102_2, R.string.cl_102_3)),
    ChangelogEntry("1.0.1", R.string.cl_101_date, listOf(R.string.cl_101_1, R.string.cl_101_2, R.string.cl_101_3)),
    ChangelogEntry("1.0.0", R.string.cl_100_date, listOf(R.string.cl_100_1, R.string.cl_100_2, R.string.cl_100_3)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(onDismiss: () -> Unit) {
    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.changelog_title),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            itemsIndexed(changelogEntries) { index, entry ->
                ChangelogCard(entry = entry, expandedByDefault = index == 0)
                if (index < changelogEntries.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogEntry, expandedByDefault: Boolean) {
    var expanded by remember(entry.version) { mutableStateOf(expandedByDefault) }

    InsetGroup {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.changelog_version_prefix, entry.version),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(entry.dateRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(start = 13.dp, end = 13.dp, bottom = 12.dp)) {
                    entry.changeRes.forEach { resId ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp, end = 9.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Text(
                                text = stringResource(resId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
