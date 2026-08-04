package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun CountdownText(
    airingAtEpochSeconds: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val airedLabel = stringResource(R.string.schedule_aired_label)
    val motion = LocalMotionPolicy.current
    var text by remember(airingAtEpochSeconds, airedLabel) {
        mutableStateOf(formatAiringCountdown(airingAtEpochSeconds, ZoneId.systemDefault(), airedLabel))
    }

    LaunchedEffect(airingAtEpochSeconds, airedLabel) {
        while (true) {
            text = formatAiringCountdown(airingAtEpochSeconds, ZoneId.systemDefault(), airedLabel)
            delay(60_000L)
        }
    }

    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = motion.iosTween(IosMotion.Quick)) togetherWith
                fadeOut(animationSpec = motion.iosTween(IosMotion.Quick))
        },
        label = "countdown-text",
    ) { value ->
        Text(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            color = color,
        )
    }
}
