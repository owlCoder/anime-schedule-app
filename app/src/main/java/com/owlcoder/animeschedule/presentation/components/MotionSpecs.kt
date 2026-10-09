package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Calm, responsive motion values shared by navigation, overlays and interactive controls. */
object IosMotion {
    const val PressIn = 80
    const val PressOut = 180
    const val Quick = 160
    const val Standard = 240
    const val Navigation = 300
    const val Sheet = 300

    /** Smooth ease-out close to the timing used by modern iOS interface transitions. */
    val StandardEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1.00f)
    val DecelerateEasing = CubicBezierEasing(0.16f, 1.00f, 0.30f, 1.00f)
    val AccelerateEasing = CubicBezierEasing(0.40f, 0.00f, 0.20f, 1.00f)
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

fun MotionPolicy.iosPressIn(): FiniteAnimationSpec<Float> = tween(
    durationMillis = duration(IosMotion.PressIn),
    easing = IosMotion.DecelerateEasing,
)

fun MotionPolicy.iosPressOut(): FiniteAnimationSpec<Float> = iosDecelerate(IosMotion.PressOut)

/** Content size must use the same policy as its fade; Compose's default is a separate spring. */
fun MotionPolicy.contentTransform(
    enter: EnterTransition? = null,
    exit: ExitTransition? = null,
    durationMillis: Int = IosMotion.Standard,
): ContentTransform = ContentTransform(
    targetContentEnter = enter ?: fadeIn(iosDecelerate(durationMillis)),
    initialContentExit = exit ?: fadeOut(iosAccelerate(minOf(durationMillis, IosMotion.Quick))),
    sizeTransform = SizeTransform { _, _ -> iosTween(durationMillis) },
)

fun MotionPolicy.expandEnter(): EnterTransition =
    fadeIn(iosDecelerate(IosMotion.Standard)) + expandVertically(iosDecelerate(IosMotion.Standard))

fun MotionPolicy.expandExit(): ExitTransition =
    fadeOut(iosAccelerate(IosMotion.Quick)) + shrinkVertically(iosAccelerate(IosMotion.Quick))

/** Fast tactile compression on touch-down, followed by a bounded, smooth release. */
@Composable
fun Modifier.iosPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.975f,
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
