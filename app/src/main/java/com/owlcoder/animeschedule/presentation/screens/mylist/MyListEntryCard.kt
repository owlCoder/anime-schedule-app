package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.displayName
import com.owlcoder.animeschedule.presentation.components.iosPressScale

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
    modifier: Modifier = Modifier,
) {
    val statusTint = statusColor(entry.status)
    val progress = entry.totalEpisodes
        ?.takeIf { it > 0 && entry.episodesWatched > 0 }
        ?.let { (entry.episodesWatched.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
    val cardInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp)
                .iosPressScale(cardInteraction, pressedScale = 0.992f),
            shape = ContinuousRoundedShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.58f),
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
            ),
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
                    .padding(start = 10.dp, end = 7.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = onCardClick,
                        )
                        .semantics { role = Role.Button },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MediaThumbnail.Small(
                        url = coverImageUrl,
                        contentDescription = title,
                        modifier = Modifier.size(50.dp, 68.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = episodeLabel(entry),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Surface(
                                shape = CircleShape,
                                color = statusTint.copy(alpha = 0.12f),
                                contentColor = statusTint,
                                tonalElevation = 0.dp,
                            ) {
                                Text(
                                    text = entry.status.displayName(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (entry.score > 0) {
                                Text(
                                    text = "★ ${entry.score}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(CircleShape),
                                color = statusTint,
                                trackColor = statusTint.copy(alpha = 0.12f),
                                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            )
                        }
                    }
                }

                Spacer(Modifier.width(5.dp))
                if (entry.status == WatchStatus.WATCHING) {
                    GlassListAction(
                        enabled = !isIncrementing,
                        onClick = onIncrementEpisode,
                        actionDescription = "+1",
                    ) {
                        if (isIncrementing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "+1",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(Modifier.width(2.dp))
                }
                GlassListAction(
                    onClick = onEditStatus,
                    actionDescription = stringResource(R.string.cd_edit_list_status),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (showDivider) Spacer(Modifier.height(1.dp))
    }
}

@Composable
private fun GlassListAction(
    onClick: () -> Unit,
    actionDescription: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(42.dp)
            .iosPressScale(interactionSource, pressedScale = 0.91f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                role = Role.Button
                contentDescription = actionDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f),
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
            ),
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

private fun episodeLabel(entry: MalListEntry): String =
    "Ep. ${entry.episodesWatched}${entry.totalEpisodes?.let { "/$it" } ?: ""}"
