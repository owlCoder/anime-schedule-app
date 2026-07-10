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
import kotlinx.coroutines.delay
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.time.formatAiringCountdown
import java.time.ZoneId

@Composable
fun CountdownText(
    airingAtEpochSeconds: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val airedLabel = stringResource(R.string.schedule_aired_label)
    var text by remember(airedLabel) {
        mutableStateOf(formatAiringCountdown(airingAtEpochSeconds, ZoneId.systemDefault(), airedLabel))
    }
    LaunchedEffect(airingAtEpochSeconds, airedLabel) {
        while (true) {
            text = formatAiringCountdown(airingAtEpochSeconds, ZoneId.systemDefault(), airedLabel)
            delay(60_000L)
        }
    }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum"
        ),
        color = color
    )
}
