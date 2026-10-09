package com.owlcoder.animeschedule.presentation.components

import android.provider.Settings
import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Immutable
data class MotionPolicy(val reduceMotion: Boolean) {
    val animationsEnabled: Boolean get() = !reduceMotion
    fun duration(defaultMillis: Int): Int = if (reduceMotion) 0 else defaultMillis
}

val LocalMotionPolicy = compositionLocalOf { MotionPolicy(reduceMotion = false) }

@Composable
fun rememberMotionPolicy(): MotionPolicy {
    val resolver = LocalContext.current.contentResolver
    var policy by remember(resolver) { mutableStateOf(resolver.readMotionPolicy()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                policy = resolver.readMotionPolicy()
            }
        }
        runCatching {
            listOf(Settings.Global.ANIMATOR_DURATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE)
                .forEach { resolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer) }
        }
        // Read again after subscribing so a change during composition cannot be missed.
        policy = resolver.readMotionPolicy()
        onDispose { runCatching { resolver.unregisterContentObserver(observer) } }
    }
    return policy
}

private fun ContentResolver.readMotionPolicy() = MotionPolicy(runCatching {
    Settings.Global.getFloat(this, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
        Settings.Global.getFloat(this, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
}.getOrDefault(false))

@Composable
fun rememberReduceMotion(): Boolean = rememberMotionPolicy().reduceMotion

@Composable
fun ProvideMotionPolicy(
    policy: MotionPolicy = rememberMotionPolicy(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMotionPolicy provides policy, content = content)
}
