package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListStatusBottomSheet(
    animeId: Int,
    currentEntry: MalListEntry?,
    onDismiss: () -> Unit,
    onConfirm: (Int, MalListUpdate) -> Unit,
    onRemove: ((Int) -> Unit)? = null,
    animeTitle: String = currentEntry?.title.orEmpty(),
    totalEpisodes: Int? = currentEntry?.totalEpisodes,
) {
    AppSheet(onDismissRequest = onDismiss) {
        key(animeId) {
            ListStatusEditor(
                animeId,
                currentEntry,
                onDismiss,
                onConfirm,
                onRemove,
                animeTitle,
                totalEpisodes
            )
        }
    }
}

/** The day overlay reuses this content, keeping one modal and its original list position. */
@Composable
fun ListStatusEditor(
    animeId: Int,
    currentEntry: MalListEntry?,
    onDismiss: () -> Unit,
    onConfirm: (Int, MalListUpdate) -> Unit,
    onRemove: ((Int) -> Unit)? = null,
    animeTitle: String = currentEntry?.title.orEmpty(),
    totalEpisodes: Int? = currentEntry?.totalEpisodes,
) {
    val initialEntry = remember(animeId) { currentEntry }
    val total = totalEpisodes?.takeIf { it > 0 }
    val tools = LocalWatchTools.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val motion = LocalMotionPolicy.current
    DisposableEffect(animeId) { onDispose { keyboard?.hide() } }
    fun close() {
        focus.clearFocus(force = true); keyboard?.hide(); onDismiss()
    }

    fun clamp(value: Int) = value.coerceIn(0, total ?: 99999)
    var status by remember(animeId) {
        mutableStateOf(
            initialEntry?.status ?: WatchStatus.PLAN_TO_WATCH
        )
    }
    var episodes by remember(animeId) {
        mutableIntStateOf(
            clamp(
                initialEntry?.episodesWatched ?: 0
            )
        )
    }
    var input by remember(animeId) { mutableStateOf(episodes.toString()) }
    var score by remember(animeId) { mutableIntStateOf((initialEntry?.score ?: 0).coerceIn(0, 10)) }
    var statusExpanded by remember(animeId) { mutableStateOf(false) }
    var noteExpanded by remember(animeId) { mutableStateOf(false) }
    var confirmRemoval by remember(animeId) { mutableStateOf(false) }
    var note by remember(animeId) { mutableStateOf(tools.data.notes[animeId].orEmpty()) }
    var tagsExpanded by remember(animeId) { mutableStateOf(false) }
    var tags by remember(animeId) { mutableStateOf(tools.data.tags[animeId]?.joinToString(", ").orEmpty()) }
    fun setEpisodes(value: Int) {
        episodes = clamp(value)
        input = episodes.toString()
        if (status == WatchStatus.COMPLETED && total != null && episodes < total) status =
            WatchStatus.WATCHING
    }

    fun save() {
        tools.setNote(animeId, note)
        tools.setTags(animeId, tags)
        onConfirm(
            animeId,
            MalListUpdate(status = status, episodesWatched = clamp(episodes), score = score)
        )
        close()
    }

    Column {
        AppInlineHeader(
            title = stringResource(R.string.list_status_title), onBack = ::close,
            backContentDescription = stringResource(R.string.common_back),
            trailingContent = {
                AppButton(stringResource(R.string.common_save), ::save, Modifier.testTag("list-editor-save"), icon = Icons.Default.Save)
            },
        )
        Column(
            Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState())
                .padding(top = 6.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        animeTitle.ifBlank { "#$animeId" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.editor_local_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { tools.toggleFavorite(animeId) },
                    modifier = Modifier.testTag("editor-favorite")
                ) {
                    Icon(
                        if (animeId in tools.data.favorites) Icons.Default.Star else Icons.Outlined.StarBorder,
                        stringResource(if (animeId in tools.data.favorites) R.string.remove_favorite else R.string.add_favorite),
                        tint = if (animeId in tools.data.favorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            EditorGroup {
                Row(
                    Modifier.fillMaxWidth().clickable { statusExpanded = !statusExpanded }
                        .testTag("editor-status-picker").padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        status.editorIcon(),
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            stringResource(R.string.detail_status),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            status.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        if (statusExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        stringResource(R.string.editor_choose_status)
                    )
                }
                AnimatedVisibility(
                    statusExpanded,
                    enter = fadeIn(motion.iosTween(IosMotion.Quick)),
                    exit = fadeOut(motion.iosTween(IosMotion.Quick))
                ) {
                    Column(
                        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WatchStatus.entries.filter { it != WatchStatus.NOT_IN_LIST }.chunked(2)
                            .forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { choice ->
                                        Surface(
                                            modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium).selectable(
                                                choice == status,
                                                role = Role.RadioButton
                                            ) {
                                                status = choice
                                                if (choice == WatchStatus.COMPLETED && total != null) {
                                                    episodes = total; input = total.toString()
                                                }
                                                statusExpanded = false
                                            },
                                            shape = MaterialTheme.shapes.medium,
                                            color = if (choice == status) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ) {
                                            Row(
                                                Modifier.heightIn(min = 48.dp)
                                                    .padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    choice.editorIcon(),
                                                    null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    choice.displayName(),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                    }
                }
            }
            EditorGroup {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.list_status_episodes_label),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (total != null) androidx.compose.ui.res.pluralStringResource(
                                R.plurals.editor_remaining_count,
                                (total - episodes).coerceAtLeast(0),
                                (total - episodes).coerceAtLeast(0)
                            ) else stringResource(R.string.editor_unknown_total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            14.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { setEpisodes(episodes - 1) },
                            enabled = episodes > 0,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                stringResource(R.string.list_status_decrease_episode)
                            )
                        }
                        OutlinedTextField(
                            value = input,
                            onValueChange = { value ->
                                if (value.length <= 5 && value.all(Char::isDigit)) {
                                    setEpisodes(value.toIntOrNull() ?: 0)
                                    if (value.isEmpty()) input = ""
                                }
                            },
                            modifier = Modifier.width(128.dp).testTag("list-editor-episodes"),
                            textStyle = (if (input.length > 3) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium).copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            suffix = {
                                if (total != null && total.toString().length <= 3 && input.length <= 3) Text(
                                    "/ $total",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() }),
                            shape = MaterialTheme.shapes.large,
                        )
                        FilledTonalIconButton(
                            onClick = { setEpisodes(episodes + 1) },
                            enabled = total == null || episodes < total,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                stringResource(R.string.list_status_increase_episode)
                            )
                        }
                    }
                    if (total != null && (total.toString().length > 3 || input.length > 3)) {
                        Text(
                            "$episodes / $total",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (total != null) {
                        val progress by animateFloatAsState(
                            episodes.toFloat() / total,
                            motion.iosSpring(),
                            label = "episode-progress"
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .clip(MaterialTheme.shapes.small),
                            gapSize = 0.dp,
                            drawStopIndicator = {})
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AppButton("−10", { setEpisodes(episodes - 10) }, Modifier.weight(1f), variant = AppButtonVariant.Plain, enabled = episodes > 0, icon = Icons.Default.FastRewind)
                        if (total != null) AppButton(stringResource(R.string.editor_finish_all), {
                            setEpisodes(total); status = WatchStatus.COMPLETED
                        }, Modifier.weight(1.6f), variant = AppButtonVariant.Plain, icon = Icons.Default.DoneAll)
                        AppButton("+10", { setEpisodes(episodes + 10) }, Modifier.weight(1f), variant = AppButtonVariant.Plain, enabled = total == null || episodes < total, icon = Icons.Default.FastForward)
                    }
                }
            }
            EditorGroup {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            stringResource(R.string.detail_score),
                            Modifier.weight(1f).padding(start = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (score == 0) stringResource(R.string.list_status_score_unrated) else "$score / 10",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    (1..10).chunked(5).forEach { values ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            values.forEach { value ->
                                Surface(
                                    modifier = Modifier.weight(1f).height(44.dp).clip(MaterialTheme.shapes.medium).selectable(
                                        score == value,
                                        role = Role.RadioButton
                                    ) { score = value },
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (score == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if (score == value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            value.toString(),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (score > 0) AppButton(stringResource(R.string.editor_clear_rating), { score = 0 }, variant = AppButtonVariant.Plain, icon = Icons.Default.StarBorder)
                }
            }
            EditorGroup {
                Row(Modifier.fillMaxWidth().clickable { noteExpanded = !noteExpanded }
                    .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EditNote,
                        null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.personal_note),
                        Modifier.weight(1f).padding(horizontal = 10.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        if (noteExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null
                    )
                }
                AnimatedVisibility(
                    noteExpanded,
                    enter = fadeIn(motion.iosTween(IosMotion.Quick)),
                    exit = fadeOut(motion.iosTween(IosMotion.Quick))
                ) {
                    OutlinedTextField(
                        note,
                        { if (it.length <= 2000) note = it },
                        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            .testTag("editor-note"),
                        placeholder = { Text(stringResource(R.string.note_placeholder)) },
                        minLines = 2,
                        maxLines = 5,
                        supportingText = { Text(stringResource(R.string.note_local)) },
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
            EditorGroup {
                Row(Modifier.fillMaxWidth().clickable { tagsExpanded = !tagsExpanded }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Label, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.personal_tags), Modifier.weight(1f).padding(horizontal = 10.dp), fontWeight = FontWeight.SemiBold)
                    Icon(if (tagsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
                if (tagsExpanded) OutlinedTextField(tags, { tags = it.take(208) }, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp).testTag("editor-tags"),
                    placeholder = { Text(stringResource(R.string.tags_placeholder)) }, supportingText = { Text(stringResource(R.string.tags_hint)) }, shape = MaterialTheme.shapes.medium)
            }
            if (initialEntry != null && onRemove != null) {
                if (confirmRemoval) {
                    Text(
                        stringResource(R.string.editor_remove_confirm),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        AppButton(stringResource(R.string.common_cancel), { confirmRemoval = false }, Modifier.weight(1f), variant = AppButtonVariant.Plain, icon = Icons.Default.Close)
                        AppButton(stringResource(R.string.list_status_remove), { onRemove(animeId); close() }, Modifier.weight(1f).testTag("editor-remove-confirm"), variant = AppButtonVariant.Destructive, icon = Icons.Default.DeleteOutline)
                    }
                } else {
                    AppButton(stringResource(R.string.list_status_remove), { confirmRemoval = true }, Modifier.align(Alignment.CenterHorizontally), variant = AppButtonVariant.Plain, icon = Icons.Default.DeleteOutline)
                }
            }
        }
    }
}

@Composable
private fun EditorGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

private fun WatchStatus.editorIcon() = when (this) {
    WatchStatus.WATCHING -> Icons.Default.PlayCircle
    WatchStatus.COMPLETED -> Icons.Default.CheckCircle
    WatchStatus.PLAN_TO_WATCH, WatchStatus.NOT_IN_LIST -> Icons.Default.Bookmark
    WatchStatus.ON_HOLD -> Icons.Default.PauseCircle
    WatchStatus.DROPPED -> Icons.Default.RemoveCircle
}
