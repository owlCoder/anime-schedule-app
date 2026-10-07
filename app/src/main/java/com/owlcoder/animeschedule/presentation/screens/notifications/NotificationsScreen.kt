package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.screens.schedule.LocalScheduleZone
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.presentation.components.InsetListRow
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Each notification owns a rounded surface, including its selected background. */
@Composable
internal fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    val isUnread = !notification.isRead

    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
        InsetListRow(
            label = notification.title,
            supportingText = stringResource(R.string.notification_episode_time, notification.episode, relativeTime(notification.createdAtEpochSeconds)),
            selected = isUnread,
            onClick = onClick,
            leadingContent = {
                MediaThumbnail.Small(
                    url = notification.coverImageUrl,
                    contentDescription = notification.title,
                    modifier = Modifier.size(48.dp, 62.dp),
                )
            },
            trailingContent = {
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            },
        )
    }
}

@Composable
private fun relativeTime(epochSeconds: Long): String {
    val now = Instant.now()
    val time = Instant.ofEpochSecond(epochSeconds)
    val minutesAgo = ChronoUnit.MINUTES.between(time, now)
    return when {
        minutesAgo < 1 -> stringResource(R.string.notif_time_just_now)
        minutesAgo < 60 -> stringResource(R.string.notif_time_minutes, minutesAgo)
        minutesAgo < 1440 -> stringResource(R.string.notif_time_hours, minutesAgo / 60)
        else -> {
            val formatter = DateTimeFormatter.ofPattern("d.M.yyyy")
            time.atZone(LocalScheduleZone.current).format(formatter)
        }
    }
}
