package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListStatusBottomSheet(
    animeId: Int,
    currentEntry: MalListEntry?,
    onDismiss: () -> Unit,
    onConfirm: (Int, MalListUpdate) -> Unit,
    onRemove: ((Int) -> Unit)? = null,
) {
    val total = currentEntry?.totalEpisodes?.takeIf { it > 0 }

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
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SectionLabel(stringResource(R.string.list_status_title))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    statuses.forEach { status ->
                        val selected = selectedStatus == status
                        GlassSurface(
                            modifier = Modifier
                                .height(36.dp)
                                .clickable {
                                    selectedStatus = status
                                    if (status == WatchStatus.COMPLETED && total != null) {
                                        episodesWatched = total
                                    }
                                }
                                .semantics { role = Role.RadioButton },
                            shape = PillShape,
                            tone = if (selected) GlassTone.Accent else GlassTone.Neutral,
                            blur = GlassBlur.None,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Text(
                                    text = status.displayName(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.list_status_episodes_label),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            if (total != null) {
                                Text(
                                    text = "$episodesWatched of $total",
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
                        Text(
                            text = episodesWatched.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.Center,
                        )
                        StepperButton(
                            icon = Icons.Default.Add,
                            description = stringResource(R.string.list_status_increase_episode),
                            enabled = total == null || episodesWatched < total,
                            onClick = { episodesWatched = clampEpisodes(episodesWatched + 1) },
                        )
                    }
                    if (total != null) {
                        LinearProgressIndicator(
                            progress = { episodesWatched.toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(PillShape),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(
                        text = stringResource(R.string.list_status_score_label, "").trim(),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (score == 0) stringResource(R.string.list_status_score_unrated) else "$score / 10",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    (0..10).forEach { value ->
                        val selected = score == value
                        GlassSurface(
                            modifier = Modifier
                                .size(if (value == 0) 54.dp else 36.dp, 36.dp)
                                .clickable { score = value },
                            shape = PillShape,
                            tone = if (selected) GlassTone.Accent else GlassTone.Neutral,
                            blur = GlassBlur.None,
                        ) {
                            Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (value == 0) "—" else value.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (currentEntry != null && onRemove != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
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
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
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
            .size(38.dp)
            .clip(PillShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.size(34.dp),
            shape = PillShape,
            tone = GlassTone.Neutral,
            blur = GlassBlur.None,
        ) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(17.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                )
            }
        }
    }
}
