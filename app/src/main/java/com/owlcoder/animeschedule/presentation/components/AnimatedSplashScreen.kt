package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R

/** Branded first-load surface: quiet, centered and intentionally animation-light. */
@Composable
fun AnimatedSplashScreen(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "loadingPulse")
    val pulse = if (reduceMotion) {
        1f
    } else {
        transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "logoPulse",
        ).value
    }
    val rotation = if (reduceMotion) {
        0f
    } else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
            ),
            label = "loadingRing",
        ).value
    }
    val loadingDescription = stringResource(R.string.splash_title)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { change -> change.consume() }
                    }
                }
            }
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .semantics {
                contentDescription = loadingDescription
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                val bloomOuter = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                val bloomInner = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        color = bloomOuter,
                        radius = size.minDimension * 0.48f,
                    )
                    drawCircle(
                        color = bloomInner,
                        radius = size.minDimension * 0.32f,
                    )
                }
                AnimeScheduleMark(
                    modifier = Modifier
                        .size(78.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        },
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.splash_eyebrow),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            LoadingRing(
                rotation = rotation,
                reduceMotion = reduceMotion,
                modifier = Modifier.semantics {
                    contentDescription = loadingDescription
                },
            )
        }
    }
}

/** Shared calendar-and-star application mark used by loading and branded surfaces. */
@Composable
fun AnimeScheduleMark(
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    Image(
        painter = painterResource(R.drawable.ic_app_icon),
        contentDescription = null,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}

@Composable
private fun LoadingRing(
    rotation: Float,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(20.dp)) {
        drawArc(
            color = primary.copy(alpha = 0.20f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 1.8.dp.toPx()),
        )
        drawArc(
            color = primary,
            startAngle = rotation - 90f,
            sweepAngle = if (reduceMotion) 110f else 96f,
            useCenter = false,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
