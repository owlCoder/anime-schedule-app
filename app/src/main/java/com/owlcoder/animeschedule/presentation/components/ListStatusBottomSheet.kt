package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListStatusBottomSheet(
    animeId: Int,
    currentEntry: MalListEntry?,
    onDismiss: () -> Unit,
    onConfirm: (Int, MalListUpdate) -> Unit,
    onRemove: ((Int) -> Unit)? = null,
) {
    val total = currentEntry?.totalEpisodes?.takeIf { it > 0 }
    val motion = LocalMotionPolicy.current

    fun clampEpisodes(value: Int): Int {
        val floored = value.coerceAtLeast(0)
        return if (total != null) floored.coerceAtMost(total) else floored
    }

    var selectedStatus by remember(currentEntry) {
        mutableStateOf(currentEntry?.status ?: WatchStatus.PLAN_TO_WATCH)
    }
    var episodesWatched by remember(currentEntry) {
        mutableIntStateOf(clampEpisodes(currentEntry?.episodesWatched ?: 0))
    }
    var score by remember(currentEntry) { mutableIntStateOf(currentEntry?.score ?: 0) }
    val statuses = WatchStatus.entries.filter { it != WatchStatus.NOT_IN_LIST }

    fun save() {
        onConfirm(
            animeId,
            MalListUpdate(
                status = selectedStatus,
                episodesWatched = clampEpisodes(episodesWatched),
                score = score,
            ),
        )
        onDismiss()
    }

    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.list_status_title),
        trailingContent = {
            TextButton(onClick = ::save) {
                Text(
                    text = stringResource(R.string.common_save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 590.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.detail_status))
                statuses.chunked(2).forEach { rowStatuses ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowStatuses.forEach { status ->
                            StatusChoice(
                                label = status.displayName(),
                                selected = selectedStatus == status,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedStatus = status
                                    if (status == WatchStatus.COMPLETED && total != null) {
                                        episodesWatched = total
                                    }
                                },
                            )
                        }
                        if (rowStatuses.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Grouped,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.list_status_episodes_label),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (total != null) {
                                Text(
                                    text = "$episodesWatched / $total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        StepperButton(
                            icon = Icons.Default.Remove,
                            description = stringResource(R.string.list_status_decrease_episode),
                            enabled = episodesWatched > 0,
                            onClick = { episodesWatched = clampEpisodes(episodesWatched - 1) },
                        )
                        AnimatedContent(
                            targetState = episodesWatched,
                            modifier = Modifier.width(44.dp),
                            transitionSpec = {
                                fadeIn(animationSpec = motion.iosTween(IosMotion.Quick)) togetherWith
                                    fadeOut(animationSpec = motion.iosTween(IosMotion.Quick))
                            },
                            label = "episode-stepper-value",
                        ) { value ->
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        StepperButton(
                            icon = Icons.Default.Add,
                            description = stringResource(R.string.list_status_increase_episode),
                            enabled = total == null || episodesWatched < total,
                            onClick = { episodesWatched = clampEpisodes(episodesWatched + 1) },
                        )
                    }
                    if (total != null) {
                        val progress by animateFloatAsState(
                            targetValue = episodesWatched.toFloat() / total.toFloat(),
                            animationSpec = motion.iosSpring(),
                            label = "episode-progress",
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(PillShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(
                        text = stringResource(R.string.detail_score),
                        modifier = Modifier.weight(1f),
                    )
                    AnimatedContent(
                        targetState = score,
                        transitionSpec = {
                            fadeIn(animationSpec = motion.iosTween(IosMotion.Quick)) togetherWith
                                fadeOut(animationSpec = motion.iosTween(IosMotion.Quick))
                        },
                        label = "score-label",
                    ) { value ->
                        Text(
                            text = if (value == 0) {
                                stringResource(R.string.list_status_score_unrated)
                            } else {
                                "$value / 10"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (value == 0) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                listOf((0..5).toList(), (6..10).toList()).forEach { values ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        values.forEach { value ->
                            ScoreChoice(
                                value = value,
                                selected = score == value,
                                modifier = Modifier.weight(1f),
                                onClick = { score = value },
                            )
                        }
                        repeat(6 - values.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            if (currentEntry != null && onRemove != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(
                    onClick = {
                        onRemove(animeId)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = stringResource(R.string.list_status_remove),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val fill by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            appMaterialColor(AppMaterial.Interactive)
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "status-choice-fill",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "status-choice-color",
    )
    Surface(
        modifier = modifier
            .sizeIn(minHeight = 44.dp)
            .toggleable(
                value = selected,
                role = Role.RadioButton,
                onValueChange = { onClick() },
            ),
        shape = PillShape,
        color = fill,
        contentColor = contentColor,
        border = BorderStroke(
            0.5.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(width = 20.dp, height = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun ScoreChoice(
    value: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val fill by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            appMaterialColor(AppMaterial.Interactive)
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "score-choice-fill",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "score-choice-color",
    )
    Surface(
        modifier = modifier
            .sizeIn(minHeight = 44.dp)
            .toggleable(
                value = selected,
                role = Role.RadioButton,
                onValueChange = { onClick() },
            ),
        shape = PillShape,
        color = fill,
        contentColor = contentColor,
        border = BorderStroke(
            0.5.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (value == 0) "—" else value.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = PillShape,
            color = appMaterialColor(AppMaterial.Interactive),
            contentColor = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            },
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}
