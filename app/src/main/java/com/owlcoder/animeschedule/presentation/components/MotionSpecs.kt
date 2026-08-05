package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Motion values tuned for calm, direct iOS-style movement without visible bounce. */
object IosMotion {
    const val Quick = 160
    const val Standard = 260
    const val Navigation = 320
    const val Sheet = 360

    val StandardEasing = CubicBezierEasing(0.20f, 0.00f, 0.00f, 1.00f)
    val DecelerateEasing = CubicBezierEasing(0.00f, 0.00f, 0.00f, 1.00f)
}

fun <T> MotionPolicy.iosTween(
    durationMillis: Int = IosMotion.Standard,
    delayMillis: Int = 0,
): FiniteAnimationSpec<T> = tween(
    durationMillis = duration(durationMillis),
    delayMillis = if (reduceMotion) 0 else delayMillis,
    easing = IosMotion.StandardEasing,
)

fun <T> MotionPolicy.iosDecelerate(
    durationMillis: Int = IosMotion.Standard,
): FiniteAnimationSpec<T> = tween(
    durationMillis = duration(durationMillis),
    easing = IosMotion.DecelerateEasing,
)

fun <T> MotionPolicy.iosSpring(): FiniteAnimationSpec<T> = if (reduceMotion) {
    tween(durationMillis = 0)
} else {
    spring(
        dampingRatio = 0.90f,
        stiffness = Spring.StiffnessMediumLow,
    )
}

/** Subtle tactile compression for icon buttons and tab items. */
@Composable
fun Modifier.iosPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f,
): Modifier {
    val policy = LocalMotionPolicy.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && policy.animationsEnabled) pressedScale else 1f,
        animationSpec = policy.iosSpring(),
        label = "ios-press-scale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
