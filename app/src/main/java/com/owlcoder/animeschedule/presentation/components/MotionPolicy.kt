package com.owlcoder.animeschedule.presentation.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Immutable
data class MotionPolicy(val reduceMotion: Boolean) {
    val animationsEnabled: Boolean get() = !reduceMotion
    fun duration(defaultMillis: Int): Int = if (reduceMotion) 0 else defaultMillis
}

val LocalMotionPolicy = compositionLocalOf { MotionPolicy(reduceMotion = false) }

@Composable
fun rememberMotionPolicy(): MotionPolicy {
    val context = LocalContext.current
    return remember(context) {
        MotionPolicy(
            reduceMotion = runCatching {
                val resolver = context.contentResolver
                val animator = Settings.Global.getFloat(
                    resolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                )
                val transitions = Settings.Global.getFloat(
                    resolver,
                    Settings.Global.TRANSITION_ANIMATION_SCALE,
                    1f,
                )
                animator == 0f || transitions == 0f
            }.getOrDefault(false),
        )
    }
}

@Composable
fun rememberReduceMotion(): Boolean = rememberMotionPolicy().reduceMotion

@Composable
fun ProvideMotionPolicy(
    policy: MotionPolicy = rememberMotionPolicy(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMotionPolicy provides policy, content = content)
}
