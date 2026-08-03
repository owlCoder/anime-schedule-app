package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.presentation.components.FaviconImage
import com.owlcoder.animeschedule.presentation.components.AppInlineHeader
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.GlassIconButton
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.InsetListRow
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSourcesScreen(
    onBack: () -> Unit,
    viewModel: WatchSourcesViewModel = hiltViewModel()
) {
    val sources by viewModel.sources.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp)
        ) {
            AppInlineHeader(
                title = stringResource(R.string.watch_sources_title),
                onBack = onBack,
                trailingContent = {
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = stringResource(R.string.watch_sources_add),
                        onClick = { showAddSheet = true },
                    )
                },
            )
            Text(
                stringResource(R.string.watch_sources_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            InsetGroup(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.watch_sources_title),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 4.dp + LocalNavBarHeight.current)
                ) {
                    items(sources, key = { it.id }) { source ->
                        WatchSourceRow(
                            source = source,
                            onDelete = { viewModel.deleteSource(source) },
                            onOpenExternallyChange = { viewModel.setOpenExternally(source, it) }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddWatchSourceSheet(
            onDismiss = { showAddSheet = false },
            onConfirm = { name, urlTemplate, openExternally ->
                viewModel.addSource(name, urlTemplate, openExternally)
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun WatchSourceRow(
    source: WatchSource,
    onDelete: () -> Unit,
    onOpenExternallyChange: (Boolean) -> Unit
) {
    InsetListRow(
        label = source.name,
        supportingText = source.urlTemplate,
        leadingContent = {
                if (source.faviconUrl != null) {
                    FaviconImage(
                        faviconUrl = source.faviconUrl,
                        siteUrl = source.urlTemplate,
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                    )
                }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.material3.IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.watch_sources_delete), tint = MaterialTheme.colorScheme.error)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                    stringResource(R.string.watch_sources_open_externally),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(checked = source.openExternally, onCheckedChange = onOpenExternallyChange)
                }
            }
        },
        onClick = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWatchSourceSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, urlTemplate: String, openExternally: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var urlTemplate by remember { mutableStateOf("") }
    var openExternally by remember { mutableStateOf(false) }
    val isValid = name.isNotBlank() && urlTemplate.contains("{query}")

    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.watch_sources_add),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.watch_sources_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = urlTemplate,
                onValueChange = { urlTemplate = it },
                label = { Text(stringResource(R.string.watch_sources_url_label)) },
                placeholder = { Text("https://example.com/search?q={query}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.watch_sources_url_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.watch_sources_open_externally),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.watch_sources_open_externally_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = openExternally, onCheckedChange = { openExternally = it })
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { onConfirm(name.trim(), urlTemplate.trim(), openExternally) },
                    enabled = isValid
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
