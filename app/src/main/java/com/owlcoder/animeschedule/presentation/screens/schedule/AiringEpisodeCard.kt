package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.presentation.components.CountdownText
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail

/** A compact timeline row. The left rail keeps all Today entries on one visual axis. */
@Composable
fun AiringEpisodeCard(
    episode: AiringEpisode,
    isLoggedIn: Boolean,
    isIncrementing: Boolean = false,
    onCardClick: () -> Unit,
    onIncrementEpisode: () -> Unit,
    onEditStatus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor = episode.coverColor?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } ?: MaterialTheme.colorScheme.primary
    val entry = episode.malListEntry
    val isWatching = entry?.status == WatchStatus.WATCHING

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onCardClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = episode.airingTimeLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                CountdownText(episode.airingAtEpochSeconds)
            }

            Box(
                modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        drawLine(
                            color = accentColor.copy(alpha = 0.35f),
                            start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(accentColor, CircleShape)
                )
            }

            MediaThumbnail.Small(
                url = episode.coverImageUrl,
                contentDescription = episode.title,
                modifier = Modifier.size(width = 56.dp, height = 60.dp)
            )
            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(
                            R.string.schedule_episode_label,
                            episode.episode,
                            episode.totalEpisodes?.let { "/$it" } ?: ""
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    entry?.let {
                        Text(
                            text = stringResource(
                                R.string.schedule_watched_progress,
                                it.episodesWatched,
                                it.totalEpisodes?.let { total -> "/$total" } ?: ""
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            maxLines = 1
                        )
                    }
                }
            }

            if (isLoggedIn && (isWatching || entry != null)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWatching) {
                        IconButton(
                            onClick = onIncrementEpisode,
                            enabled = !isIncrementing,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (isIncrementing) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    "+1",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 7.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onEditStatus, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.schedule_edit_status_action),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun AiringEpisode.airingTimeLabel(): String {
    return java.time.Instant.ofEpochSecond(airingAtEpochSeconds)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .toString()
        .take(5)
}
