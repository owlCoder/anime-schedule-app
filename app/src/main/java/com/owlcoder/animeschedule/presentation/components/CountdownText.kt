package com.owlcoder.animeschedule.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.time.formatAiringCountdown
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun CountdownText(
    airingAtEpochSeconds: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    val airedLabel = stringResource(R.string.schedule_aired_label)
    var text by remember(airingAtEpochSeconds, zoneId, airedLabel) {
        mutableStateOf(formatAiringCountdown(airingAtEpochSeconds, zoneId, airedLabel))
    }

    LaunchedEffect(airingAtEpochSeconds, zoneId, airedLabel) {
        while (true) {
            val remainingSeconds = airingAtEpochSeconds - Instant.now().epochSecond
            text = formatAiringCountdown(airingAtEpochSeconds, zoneId, airedLabel)
            if (remainingSeconds <= 0L) break
            delay(countdownRefreshDelayMillis(remainingSeconds))
        }
    }

    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
        ),
        color = color,
    )
}

private fun countdownRefreshDelayMillis(remainingSeconds: Long): Long = when {
    remainingSeconds > 6 * 60 * 60 -> 15 * 60_000L
    remainingSeconds > 60 * 60 -> 5 * 60_000L
    else -> 60_000L
}
