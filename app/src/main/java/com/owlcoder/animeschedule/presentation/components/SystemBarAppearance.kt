package com.owlcoder.animeschedule.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Route-level system bar contrast override.
 * Use [statusBarOnImagery] for screens whose content extends behind a photographic hero.
 */
@Composable
fun AppSystemBarAppearance(statusBarOnImagery: Boolean = false) {
    val view = LocalView.current
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.35f

    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = if (statusBarOnImagery) false else !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
