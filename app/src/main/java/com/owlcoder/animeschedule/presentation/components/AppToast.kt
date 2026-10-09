package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import kotlinx.coroutines.delay

enum class ToastTone { Info, Success, Error }

@Immutable
data class ToastData(val id: Long, val message: String, val tone: ToastTone, val action: ToastAction? = null)

@Immutable
data class ToastAction(val key: Long, val label: String, val onClick: () -> Unit, val onDismiss: () -> Unit = {})

/** A single current message; a new notification replaces the old timeout as well as its text. */
class ToastController(private val enabled: Boolean = true) {
    var current by mutableStateOf<ToastData?>(null)
        private set
    private var counter = 0L
    private val sheets = mutableStateListOf<Any>()
    internal val activeSheet: Any? get() = sheets.lastOrNull()
    internal fun attachSheet(key: Any) { if (enabled) sheets.add(key) }
    internal fun detachSheet(key: Any) { sheets.remove(key) }
    fun show(message: String, tone: ToastTone = ToastTone.Info, action: ToastAction? = null) {
        if (!enabled || message.isBlank()) return
        // Routine success events must not replace a newly offered Undo for the same edit.
        if (current?.action != null && tone == ToastTone.Success && action == null) return
        val previous = current
        current = ToastData(++counter, message.trim(), tone, action)
        if (previous?.action?.key != action?.key) previous?.action?.onDismiss?.invoke()
    }

    fun success(message: String) = show(message, ToastTone.Success)
    fun error(message: String) = show(message, ToastTone.Error)
    fun dismiss() {
        val previous = current
        current = null
        previous?.action?.onDismiss?.invoke()
    }
    fun clearAction(key: Long) { if (current?.action?.key == key) current = null }
    fun performAction(id: Long) {
        val data = current?.takeIf { it.id == id } ?: return
        val action = data.action ?: return
        current = null // Consume the tap before launching async work.
        action.onClick()
    }
}

// Unhosted previews/components must not share a mutable, never-expiring global message.
val LocalToast = compositionLocalOf { ToastController(enabled = false) }

@Composable
fun ToastHost(controller: ToastController, content: @Composable () -> Unit) {
    // A single timer survives moving the same message between the page and a modal.
    // Rendering it in two windows must not extend Undo or run two dismissal callbacks.
    ToastTimeout(controller)
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalToast provides controller, content = content)
        if (controller.activeSheet == null) {
            ToastOverlay(controller, Modifier.fillMaxSize().imePadding().navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = LocalNavBarHeight.current + 16.dp))
        }
    }
}

@Composable
private fun ToastTimeout(controller: ToastController) {
    val accessibility = LocalAccessibilityManager.current
    val data = controller.current
    LaunchedEffect(data?.id) {
        if (data != null) {
            val base = maxOf(
                if (data.action != null) 10_000L else if (data.tone == ToastTone.Error) 4500L else 2600L,
                (data.message.length * 45L).coerceAtMost(10_000L)
            )
            val timeout = accessibility?.calculateRecommendedTimeoutMillis(
                base,
                containsIcons = true,
                containsText = true,
                containsControls = true
            ) ?: base
            delay(timeout)
            if (controller.current?.id == data.id) controller.dismiss()
        }
    }
}

@Composable
internal fun ToastOverlay(controller: ToastController, modifier: Modifier) {
    val motion = LocalMotionPolicy.current
    val data = controller.current
    var lastShown by remember { mutableStateOf<ToastData?>(null) }
    SideEffect { if (data != null) lastShown = data }
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                data != null,
                enter = slideInVertically(
                    motion.iosDecelerate(IosMotion.Standard)
                ) { if (motion.animationsEnabled) it / 2 else 0 } + fadeIn(motion.iosTween(IosMotion.Quick)),
                exit = slideOutVertically(motion.iosTween(IosMotion.Quick)) { if (motion.animationsEnabled) it / 3 else 0 } + fadeOut(
                    motion.iosTween(IosMotion.Quick)
                ),
            ) {
                (data ?: lastShown)?.let { ToastCard(it, controller::dismiss) { controller.performAction(it.id) } }
            }
    }
}

@Composable
private fun ToastCard(data: ToastData, onDismiss: () -> Unit, onAction: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .35f
    val accent = when (data.tone) {
        ToastTone.Success -> if (dark) Color(0xFF9BD6AE) else Color(0xFF256A43)
        ToastTone.Info -> MaterialTheme.colorScheme.primary
        ToastTone.Error -> if (dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
    }
    val icon = when (data.tone) {
        ToastTone.Success -> Icons.Outlined.CheckCircle
        ToastTone.Info -> Icons.Outlined.Info
        ToastTone.Error -> Icons.Outlined.ErrorOutline
    }
    Surface(
        modifier = Modifier.widthIn(min = 180.dp, max = 420.dp).testTag("app-toast")
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        shadowElevation = 8.dp,
    ) {
        Column {
            Row(
                Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = CircleShape, color = accent.copy(alpha = .12f)) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    data.message,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (data.action != null) 3 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onDismiss, modifier = Modifier.size(48.dp).testTag("toast-dismiss")) {
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.toast_dismiss),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            data.action?.let { action ->
                AppButton(action.label, onAction, Modifier.fillMaxWidth().padding(horizontal = 8.dp).testTag("toast-action"), variant = AppButtonVariant.Plain, icon = Icons.AutoMirrored.Filled.Undo)
            }
        }
    }
}
