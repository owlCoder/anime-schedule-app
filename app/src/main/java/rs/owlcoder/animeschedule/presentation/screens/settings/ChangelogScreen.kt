package rs.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.owlcoder.animeschedule.R

private data class ChangelogEntry(
    val version: String,
    val dateRes: Int,
    val changeRes: List<Int>
)

private val changelogEntries = listOf(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.changelog_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }
            itemsIndexed(changelogEntries) { index, entry ->
                ChangelogCard(entry = entry, expandedByDefault = index == 0)
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogEntry, expandedByDefault: Boolean) {
    var expanded by remember { mutableStateOf(expandedByDefault) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.changelog_version_prefix, entry.version),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(entry.dateRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    entry.changeRes.forEach { resId ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(16.dp)
                            )
                            Text(
                                stringResource(resId),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
