package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.displayName

@Composable
private fun statusColor(status: WatchStatus): Color = when (status) {
    WatchStatus.WATCHING -> MaterialTheme.colorScheme.primary
    WatchStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
    WatchStatus.ON_HOLD -> MaterialTheme.colorScheme.secondary
    WatchStatus.DROPPED -> MaterialTheme.colorScheme.error
    WatchStatus.PLAN_TO_WATCH, WatchStatus.NOT_IN_LIST -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun MyListEntryCard(
    entry: MalListEntry,
    title: String,
    coverImageUrl: String?,
    isIncrementing: Boolean = false,
    onCardClick: () -> Unit,
    onIncrementEpisode: () -> Unit,
    onEditStatus: () -> Unit,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val statusTint = statusColor(entry.status)
    val progress = entry.totalEpisodes
        ?.takeIf { it > 0 }
        ?.let { (entry.episodesWatched.toFloat() / it.toFloat()).coerceIn(0f, 1f) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCardClick)
                    .semantics { role = Role.Button },
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaThumbnail.Small(
                    url = coverImageUrl,
                    contentDescription = title,
                    modifier = Modifier.size(52.dp, 72.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            episodeLabel(entry),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Box(Modifier.size(4.dp).clip(androidx.compose.foundation.shape.CircleShape).then(Modifier)) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth()) { drawCircle(statusTint) }
                        }
                        Text(
                            entry.status.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(50)),
                            color = statusTint,
                            trackColor = statusTint.copy(alpha = 0.16f),
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            if (entry.status == WatchStatus.WATCHING) {
                IconButton(
                    onClick = onIncrementEpisode,
                    enabled = !isIncrementing,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isIncrementing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("+1", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(onClick = onEditStatus, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_list_status), modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
        }
    }
}

private fun episodeLabel(entry: MalListEntry): String =
    "Ep. ${entry.episodesWatched}${entry.totalEpisodes?.let { "/$it" } ?: ""}"
