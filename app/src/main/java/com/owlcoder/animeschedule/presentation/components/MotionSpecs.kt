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

/** Calm, responsive motion values shared by navigation, overlays and interactive controls. */
object IosMotion {
    const val PressIn = 90
    const val Quick = 150
    const val Standard = 240
    const val Navigation = 300
    const val Sheet = 340

    /** Smooth ease-out close to the timing used by modern iOS interface transitions. */
    val StandardEasing = CubicBezierEasing(0.16f, 1.00f, 0.30f, 1.00f)
    val DecelerateEasing = CubicBezierEasing(0.05f, 0.70f, 0.10f, 1.00f)
    val AccelerateEasing = CubicBezierEasing(0.40f, 0.00f, 1.00f, 1.00f)
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

fun <T> MotionPolicy.iosAccelerate(
    durationMillis: Int = IosMotion.Quick,
): FiniteAnimationSpec<T> = tween(
    durationMillis = duration(durationMillis),
    easing = IosMotion.AccelerateEasing,
)

/**
 * Critically damped positional spring. It settles quickly without the visible rubber-band bounce
 * that feels distracting on nav indicators, switches and content-size changes.
 */
fun <T> MotionPolicy.iosSpring(
    dampingRatio: Float = 1.0f,
    stiffness: Float = Spring.StiffnessMedium,
): FiniteAnimationSpec<T> = if (reduceMotion) {
    tween(durationMillis = 0)
} else {
    spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
    )
}

fun MotionPolicy.iosPressIn(): FiniteAnimationSpec<Float> = tween(
    durationMillis = duration(IosMotion.PressIn),
    easing = IosMotion.DecelerateEasing,
)

fun MotionPolicy.iosPressOut(): FiniteAnimationSpec<Float> = if (reduceMotion) {
    tween(durationMillis = 0)
} else {
    spring(
        dampingRatio = 0.88f,
        stiffness = 720f,
    )
}

/** Fast tactile compression on touch-down, followed by a soft spring release. */
@Composable
fun Modifier.iosPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f,
): Modifier {
    val policy = LocalMotionPolicy.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && policy.animationsEnabled) pressedScale else 1f,
        animationSpec = if (pressed) policy.iosPressIn() else policy.iosPressOut(),
        label = "ios-press-scale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
