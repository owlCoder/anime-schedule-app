package com.owlcoder.animeschedule.presentation.screens.settings

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppInlineHeader
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.AppSwitch
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.FaviconImage
import com.owlcoder.animeschedule.presentation.components.GlassIconButton
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.iosSpring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSourcesScreen(
    onBack: () -> Unit,
    viewModel: WatchSourcesViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<WatchSource?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            AppInlineHeader(
                title = stringResource(R.string.watch_sources_title),
                modifier = Modifier.padding(top = 2.dp),
                onBack = onBack,
                trailingContent = {
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        iconSize = 21.dp,
                        contentDescription = stringResource(R.string.watch_sources_add),
                        onClick = { showAddSheet = true },
                    )
                },
            )
            WatchSourcesContent(
                sources = sources,
                modifier = Modifier.weight(1f),
                onAdd = { showAddSheet = true },
                onEdit = { editingSource = it },
                onOpenExternallyChange = viewModel::setOpenExternally,
            )
        }
    }

    WatchSourceEditorOverlays(
        showAddSheet = showAddSheet,
        editingSource = editingSource,
        onAddDismiss = { showAddSheet = false },
        onEditDismiss = { editingSource = null },
        onAdd = { name, url, external ->
            viewModel.addSource(name, url, external)
            showAddSheet = false
        },
        onUpdate = { source, name, url, external ->
            viewModel.updateSource(source, name, url, external)
            editingSource = null
        },
        onDelete = { source ->
            viewModel.deleteSource(source)
            editingSource = null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSourcesBottomSheet(
    onDismiss: () -> Unit,
    viewModel: WatchSourcesViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<WatchSource?>(null) }

    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.watch_sources_title),
        trailingContent = {
            GlassIconButton(
                icon = Icons.Default.Add,
                iconSize = 21.dp,
                contentDescription = stringResource(R.string.watch_sources_add),
                onClick = { showAddSheet = true },
            )
        },
    ) {
        WatchSourcesContent(
            sources = sources,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 590.dp),
            onAdd = { showAddSheet = true },
            onEdit = { editingSource = it },
            onOpenExternallyChange = viewModel::setOpenExternally,
        )
    }

    WatchSourceEditorOverlays(
        showAddSheet = showAddSheet,
        editingSource = editingSource,
        onAddDismiss = { showAddSheet = false },
        onEditDismiss = { editingSource = null },
        onAdd = { name, url, external ->
            viewModel.addSource(name, url, external)
            showAddSheet = false
        },
        onUpdate = { source, name, url, external ->
            viewModel.updateSource(source, name, url, external)
            editingSource = null
        },
        onDelete = { source ->
            viewModel.deleteSource(source)
            editingSource = null
        },
    )
}

@Composable
private fun WatchSourceEditorOverlays(
    showAddSheet: Boolean,
    editingSource: WatchSource?,
    onAddDismiss: () -> Unit,
    onEditDismiss: () -> Unit,
    onAdd: (String, String, Boolean) -> Unit,
    onUpdate: (WatchSource, String, String, Boolean) -> Unit,
    onDelete: (WatchSource) -> Unit,
) {
    if (showAddSheet) {
        WatchSourceFormSheet(
            title = stringResource(R.string.watch_sources_add),
            initialName = "",
            initialUrl = "",
            initialOpenExternally = false,
            onDismiss = onAddDismiss,
            onConfirm = onAdd,
        )
    }
    editingSource?.let { source ->
        WatchSourceFormSheet(
            title = stringResource(R.string.watch_sources_edit),
            initialName = source.name,
            initialUrl = source.urlTemplate,
            initialOpenExternally = source.openExternally,
            onDismiss = onEditDismiss,
            onConfirm = { name, url, external ->
                onUpdate(source, name, url, external)
            },
            onDelete = { onDelete(source) },
        )
    }
}

@Composable
private fun WatchSourcesContent(
    sources: List<WatchSource>,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onEdit: (WatchSource) -> Unit,
    onOpenExternallyChange: (WatchSource, Boolean) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.watch_sources_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        if (sources.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PlayCircle,
                title = stringResource(R.string.watch_sources_empty_title),
                subtitle = stringResource(R.string.watch_sources_empty_subtitle),
                actionLabel = stringResource(R.string.watch_sources_add),
                onAction = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                item(key = "watch-source-list") {
                    InsetGroup {
                        sources.forEachIndexed { index, source ->
                            SourceRow(
                                source = source,
                                onClick = { onEdit(source) },
                                onOpenExternallyChange = {
                                    onOpenExternallyChange(source, it)
                                },
                            )
                            if (index < sources.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 58.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    source: WatchSource,
    onClick: () -> Unit,
    onOpenExternallyChange: (Boolean) -> Unit,
) {
    val motion = LocalMotionPolicy.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .animateContentSize(animationSpec = motion.iosSpring())
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (source.faviconUrl != null) {
            FaviconImage(
                faviconUrl = source.faviconUrl,
                siteUrl = source.urlTemplate,
                modifier = Modifier
                    .size(36.dp)
                    .clip(ContinuousRoundedShape(10.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(ContinuousRoundedShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = source.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = source.urlTemplate.displayHost(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    if (source.openExternally) {
                        R.string.watch_sources_mode_external
                    } else {
                        R.string.watch_sources_mode_internal
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }

        AppSwitch(
            checked = source.openExternally,
            onCheckedChange = onOpenExternallyChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchSourceFormSheet(
    title: String,
    initialName: String,
    initialUrl: String,
    initialOpenExternally: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, urlTemplate: String, openExternally: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var urlTemplate by remember(initialUrl) { mutableStateOf(initialUrl) }
    var openExternally by remember(initialOpenExternally) {
        mutableStateOf(initialOpenExternally)
    }
    val isValid = name.isNotBlank() && urlTemplate.contains("{query}")

    AppSheet(
        onDismissRequest = onDismiss,
        title = title,
        trailingContent = {
            TextButton(
                onClick = {
                    onConfirm(name.trim(), urlTemplate.trim(), openExternally)
                },
                enabled = isValid,
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.common_save),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 470.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Grouped,
                shape = ContinuousRoundedShape(18.dp),
            ) {
                SourceForm(
                    name = name,
                    onNameChange = { name = it },
                    urlTemplate = urlTemplate,
                    onUrlChange = { urlTemplate = it },
                    openExternally = openExternally,
                    onOpenExternallyChange = { openExternally = it },
                )
            }
            if (onDelete != null) {
                AppButton(
                    label = stringResource(R.string.watch_sources_delete),
                    icon = Icons.Default.DeleteOutline,
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    variant = AppButtonVariant.Destructive,
                )
            }
        }
    }
}

@Composable
private fun SourceForm(
    name: String,
    onNameChange: (String) -> Unit,
    urlTemplate: String,
    onUrlChange: (String) -> Unit,
    openExternally: Boolean,
    onOpenExternallyChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FormField(
            label = stringResource(R.string.watch_sources_name_label),
            value = name,
            onValueChange = onNameChange,
        )
        FormField(
            label = stringResource(R.string.watch_sources_url_label),
            value = urlTemplate,
            onValueChange = onUrlChange,
            helper = stringResource(R.string.watch_sources_url_hint),
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.watch_sources_open_externally),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.watch_sources_open_externally_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppSwitch(
                checked = openExternally,
                onCheckedChange = onOpenExternallyChange,
            )
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    helper: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 13.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (!helper.isNullOrBlank()) {
            Text(
                text = helper,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

private fun String.displayHost(): String = runCatching {
    Uri.parse(this).host?.removePrefix("www.")
}.getOrNull().orEmpty().ifBlank { substringBefore("/{query}").take(36) }
