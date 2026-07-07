package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Visual intent of a toast — drives its icon and accent tint. */
enum class ToastTone { Info, Success, Error }

@Immutable
data class ToastData(val id: Long, val message: String, val tone: ToastTone)

/**
 * App-styled toast controller. Replaces the classic Material `Snackbar`/`android.widget.Toast`
 * with a rounded "glass" card that rises from the bottom (just above the floating nav bar),
 * with a tinted icon chip, and auto-dismisses.
 *
 * Provided once at the app root via [LocalToast]; call it from any screen:
 * `LocalToast.current.success("Epizoda označena")`.
 */
class ToastController {
    var current by mutableStateOf<ToastData?>(null)
        private set

    private var counter = 0L

    fun show(message: String, tone: ToastTone = ToastTone.Info) {
        if (message.isBlank()) return
        current = ToastData(id = ++counter, message = message.trim(), tone = tone)
    }

    fun info(message: String) = show(message, ToastTone.Info)
    fun success(message: String) = show(message, ToastTone.Success)
    fun error(message: String) = show(message, ToastTone.Error)

    fun dismiss() { current = null }
}

/** No-op fallback so the local always has a value. */
val LocalToast = compositionLocalOf { ToastController() }

/**
 * Renders the active toast over [content]. Anchored just below the status bar at the
 * TOP of the screen, slides down + fades in, then auto-dismisses after a short delay.
 * [bottomInset] is unused (kept for call-site compatibility).
 */
@Composable
fun ToastHost(
    controller: ToastController,
    @Suppress("UNUSED_PARAMETER") bottomInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()

        val data = controller.current
        LaunchedEffect(data?.id) {
            if (data != null) {
                delay(2400)
                if (controller.current?.id == data.id) controller.dismiss()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedVisibility(
                visible = data != null,
                enter = slideInVertically(spring()) { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                val shown = data ?: controller.current
                if (shown != null) ToastCard(shown)
            }
        }
    }
}

@Composable
private fun ToastCard(data: ToastData) {
    val accent: Color = when (data.tone) {
        ToastTone.Info -> MaterialTheme.colorScheme.primary
        ToastTone.Success -> MaterialTheme.colorScheme.primary
        ToastTone.Error -> MaterialTheme.colorScheme.error
    }
    val icon: ImageVector = when (data.tone) {
        ToastTone.Info -> Icons.Outlined.Info
        ToastTone.Success -> Icons.Outlined.CheckCircle
        ToastTone.Error -> Icons.Outlined.ErrorOutline
    }
    val shape = RoundedCornerShape(20.dp)
    // Frosted glass: a genuinely blurred translucent surface layer sits beneath the sharp
    // content (icon + text). The blur is isolated to this backing box (clipped by the parent
    // shape) so the message stays crisp while the card reads as frosted rather than flat.
    val glassColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .shadow(16.dp, shape, spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 22.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(Brush.verticalGradient(glassColors))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.size(12.dp))
            Text(
                data.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
