package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.presentation.components.InsetListRow
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Compact grouped-list row; the parent owns the inset surface and date grouping. */
@Composable
internal fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    val isUnread = !notification.isRead

    InsetListRow(
        label = notification.title,
        supportingText = "Ep. ${notification.episode} · ${relativeTime(notification.createdAtEpochSeconds)}",
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

private fun relativeTime(epochSeconds: Long): String {
    val now = Instant.now()
    val time = Instant.ofEpochSecond(epochSeconds)
    val minutesAgo = ChronoUnit.MINUTES.between(time, now)
    return when {
        minutesAgo < 1 -> "Upravo"
        minutesAgo < 60 -> "Pre ${minutesAgo}min"
        minutesAgo < 1440 -> "Pre ${minutesAgo / 60}h"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("d.M.yyyy")
            time.atZone(ZoneId.systemDefault()).format(formatter)
        }
    }
}
