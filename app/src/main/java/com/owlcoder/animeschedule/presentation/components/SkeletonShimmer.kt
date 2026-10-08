package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/** Read animation state only during drawing; all placeholders share one clock. */
@Composable
fun rememberSkeletonShimmer(): Modifier {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    if (!LocalMotionPolicy.current.animationsEnabled) return Modifier.drawBehind { drawRect(base) }
    val phase = rememberInfiniteTransition(label = "skeleton").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "skeleton-phase",
    )
    return Modifier.drawBehind {
        val band = size.width.coerceAtLeast(1f)
        val offset = phase.value * band * 3 - band
        drawRect(Brush.linearGradient(listOf(base, highlight, base),
            start = Offset(offset - band, 0f), end = Offset(offset, size.height)))
    }
}
