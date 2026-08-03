package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

    var selectedStatus by remember { mutableStateOf(currentEntry?.status ?: WatchStatus.PLAN_TO_WATCH) }
    var episodesWatched by remember { mutableIntStateOf(clampEpisodes(currentEntry?.episodesWatched ?: 0)) }
    var score by remember { mutableFloatStateOf(currentEntry?.score?.toFloat() ?: 0f) }
    val statuses = WatchStatus.entries.filter { it != WatchStatus.NOT_IN_LIST }

    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.list_status_title),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                statuses.forEach { status ->
                    val selected = selectedStatus == status
                    GlassSurface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable {
                                selectedStatus = status
                                if (status == WatchStatus.COMPLETED && total != null) {
                                    episodesWatched = total
                                }
                            }
                            .semantics { role = Role.RadioButton },
                        shape = PillShape,
                        tone = if (selected) GlassTone.Accent else GlassTone.Neutral,
                        blur = if (selected) GlassBlur.Soft else GlassBlur.None,
                        contentColor = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Text(
                            text = status.displayName(),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }

            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Grouped,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.list_status_episodes_label),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = if (total != null) "$episodesWatched of $total" else episodesWatched.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.list_status_decrease_episode),
                            onClick = { episodesWatched = clampEpisodes(episodesWatched - 1) },
                            enabled = episodesWatched > 0,
                        )
                        Text(
                            text = episodesWatched.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.Center,
                        )
                        GlassIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = stringResource(R.string.list_status_increase_episode),
                            onClick = { episodesWatched = clampEpisodes(episodesWatched + 1) },
                            enabled = total == null || episodesWatched < total,
                        )
                    }

                    if (total != null) {
                        LinearProgressIndicator(
                            progress = { episodesWatched.toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(PillShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }

            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Grouped,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.list_status_score_label, ""),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = score.toInt().takeIf { it > 0 }?.let { "$it / 10" }
                                ?: stringResource(R.string.list_status_score_unrated),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Slider(
                        value = score,
                        onValueChange = { score = it },
                        valueRange = 0f..10f,
                        steps = 9,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppButton(
                    label = stringResource(R.string.common_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Plain,
                )
                AppButton(
                    label = stringResource(R.string.common_save),
                    onClick = {
                        onConfirm(
                            animeId,
                            MalListUpdate(
                                status = selectedStatus,
                                episodesWatched = clampEpisodes(episodesWatched),
                                score = score.toInt(),
                            ),
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Primary,
                )
            }

            if (currentEntry != null && onRemove != null) {
                AppButton(
                    label = stringResource(R.string.list_status_remove),
                    onClick = {
                        onRemove(animeId)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = AppButtonVariant.Destructive,
                )
            }

            Spacer(Modifier.height(2.dp))
        }
    }
}
