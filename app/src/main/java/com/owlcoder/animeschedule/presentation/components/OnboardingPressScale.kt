package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Press feedback for controls that do not expose their click interaction source.
 * Pointer changes are observed without being consumed, so the following clickable still owns
 * the tap while this modifier only supplies the subtle iOS-style scale response.
 */
@Composable
fun Modifier.iosPressScale(pressedScale: Float = 0.97f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(stiffness = 620f, dampingRatio = 0.82f),
        label = "standalone-press-scale",
    )
    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    pressed = event.changes.any { it.pressed }
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
