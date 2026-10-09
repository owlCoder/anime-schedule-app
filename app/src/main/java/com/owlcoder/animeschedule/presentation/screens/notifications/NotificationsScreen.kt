package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.presentation.components.GlassIconButton
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.iosPressScale
import com.owlcoder.animeschedule.presentation.screens.schedule.LocalScheduleZone
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/** Title, episode and arrival time keep a predictable hierarchy; read actions stay separate. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onReadChange: ((Boolean) -> Unit)? = null,
    readActionEnabled: Boolean = true,
) {
    val unread = !notification.isRead
    val interaction = remember { MutableInteractionSource() }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (unread) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(.5.dp, if (unread) MaterialTheme.colorScheme.primary.copy(alpha = .28f)
            else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.weight(1f).iosPressScale(interaction, .992f)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MediaThumbnail.Small(notification.coverImageUrl, notification.title, Modifier.size(52.dp, 70.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(notification.title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (unread) Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text(stringResource(R.string.notif_episode_label, notification.episode),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(relativeTime(notification.createdAtEpochSeconds), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (onReadChange != null) GlassIconButton(
                if (unread) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                stringResource(if (unread) R.string.notif_mark_read else R.string.notif_mark_unread),
                { onReadChange(!notification.isRead) }, Modifier.testTag("notif-toggle-${notification.id}"),
                enabled = readActionEnabled,
            )
        }
    }
}

@Composable
private fun relativeTime(epochSeconds: Long): String {
    val time = Instant.ofEpochSecond(epochSeconds)
    val minutesAgo = ChronoUnit.MINUTES.between(time, Instant.now())
    return when {
        minutesAgo < 1 -> stringResource(R.string.notif_time_just_now)
        minutesAgo < 60 -> stringResource(R.string.notif_time_minutes, minutesAgo)
        minutesAgo < 1440 -> stringResource(R.string.notif_time_hours, minutesAgo / 60)
        else -> time.atZone(LocalScheduleZone.current).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalConfiguration.current.locales[0]))
    }
}
