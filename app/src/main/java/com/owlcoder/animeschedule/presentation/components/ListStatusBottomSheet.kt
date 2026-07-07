package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListStatusBottomSheet(
    animeId: Int,
    currentEntry: MalListEntry?,
    onDismiss: () -> Unit,
    onConfirm: (Int, MalListUpdate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val total = currentEntry?.totalEpisodes?.takeIf { it > 0 }

    fun clampEpisodes(value: Int): Int {
        val floored = value.coerceAtLeast(0)
        return if (total != null) floored.coerceAtMost(total) else floored
    }

    var selectedStatus by remember { mutableStateOf(currentEntry?.status ?: WatchStatus.PLAN_TO_WATCH) }
    var episodesWatched by remember { mutableIntStateOf(clampEpisodes(currentEntry?.episodesWatched ?: 0)) }
    var score by remember { mutableFloatStateOf(currentEntry?.score?.toFloat() ?: 0f) }

    val statuses = WatchStatus.entries.filter { it != WatchStatus.NOT_IN_LIST }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(
                stringResource(R.string.list_status_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            // Status chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.forEach { status ->
                    val selected = selectedStatus == status
                    Text(
                        text = status.displayName(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                selectedStatus = status
                                if (status == WatchStatus.COMPLETED && total != null) {
                                    episodesWatched = total
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Episode stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.list_status_episodes_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = { episodesWatched = clampEpisodes(episodesWatched - 1) },
                    enabled = episodesWatched > 0,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.list_status_decrease_episode))
                }
                Text(
                    text = if (total != null) "$episodesWatched / $total" else "$episodesWatched",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(72.dp).padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )
                FilledTonalIconButton(
                    onClick = { episodesWatched = clampEpisodes(episodesWatched + 1) },
                    enabled = total == null || episodesWatched < total,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.list_status_increase_episode))
                }
            }

            if (total != null) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { episodesWatched.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().clip(PillShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            // Score
            Text(
                text = stringResource(
                    R.string.list_status_score_label,
                    score.toInt().takeIf { it > 0 }?.toString() ?: stringResource(R.string.list_status_score_unrated)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Slider(value = score, onValueChange = { score = it }, valueRange = 0f..10f, steps = 9)

            Spacer(Modifier.height(16.dp))

            // Actions — glass buttons matching the app-wide language.
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = PillShape
                ) { Text(stringResource(R.string.common_cancel)) }
                Spacer(Modifier.width(12.dp))
                GlassButton(
                    onClick = {
                        onConfirm(
                            animeId, MalListUpdate(
                                status = selectedStatus,
                                episodesWatched = clampEpisodes(episodesWatched),
                                score = score.toInt().takeIf { it > 0 }
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { contentColor ->
                    Text(
                        stringResource(R.string.common_save),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            }
        }
    }
}
