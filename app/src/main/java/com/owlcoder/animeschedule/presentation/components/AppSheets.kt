package com.owlcoder.animeschedule.presentation.components

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.ui.theme.GlassTokens
import com.owlcoder.animeschedule.ui.theme.LocalAmoledDark

/** One modal owns its surface, dimming and motion, including nested Back navigation. */
@Composable
fun AppSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showBackButton: Boolean = true,
    showCloseButton: Boolean = false,
    // Return true after navigating to a parent page inside this same modal.
    onNavigateBack: () -> Boolean = { false },
    animateSizeChanges: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val dark = MaterialTheme.colorScheme.background.luminance() < .35f
    val container = if (LocalAmoledDark.current) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh
    val toast = LocalToast.current
    val sheetKey = remember { Any() }
    val visible = remember { MutableTransitionState(false) }
    var dismissing by remember { mutableStateOf(false) }
    val dismissAnimated: () -> Unit = {
        if (!dismissing && !onNavigateBack()) {
            dismissing = true
            visible.targetState = false
        }
    }
    LaunchedEffect(Unit) { if (!dismissing) visible.targetState = true }
    LaunchedEffect(visible.isIdle, visible.currentState, dismissing) {
        if (dismissing && visible.isIdle && !visible.currentState) onDismissRequest()
    }
    DisposableEffect(toast, sheetKey) {
        toast.attachSheet(sheetKey)
        onDispose { toast.detachSheet(sheetKey) }
    }

    Dialog(
        onDismissRequest = dismissAnimated,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnClickOutside = false),
    ) {
        val view = LocalView.current
        SideEffect {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) parent = parent.parent
            val window = (parent as? DialogWindowProvider)?.window
            if (window != null) {
                // Compose owns the scrim and timing. A second window fade/dim would linger.
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setWindowAnimations(0)
                window.isNavigationBarContrastEnforced = false
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        val scrimAlpha = animateFloatAsState(
            if (visible.targetState) { if (dark) .42f else .26f } else 0f,
            motion.iosTween(if (dismissing) IosMotion.Standard else IosMotion.Sheet), label = "sheet-scrim")
        val dismissLabel = stringResource(R.string.overlay_dismiss)
        val interaction = remember { MutableInteractionSource() }
        Box(Modifier.fillMaxSize()) {
            // Read opacity during drawing so dimming does not recompose the panel's content.
            Box(Modifier.matchParentSize().drawBehind { drawRect(Color.Black.copy(alpha = scrimAlpha.value)) }
                .clickable(interaction, indication = null, enabled = !dismissing, role = Role.Button, onClick = dismissAnimated)
                .semantics { contentDescription = dismissLabel })
            Box(Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(top = 8.dp),
                contentAlignment = Alignment.BottomCenter) {
                AnimatedVisibility(visibleState = visible,
                    enter = slideInVertically(motion.iosDecelerate(IosMotion.Sheet)) { if (motion.animationsEnabled) it else 0 },
                    exit = slideOutVertically(motion.iosAccelerate(IosMotion.Standard)) { if (motion.animationsEnabled) it else 0 },
                ) {
                    Surface(
                        modifier = modifier.widthIn(max = 640.dp).fillMaxWidth()
                            .then(if (animateSizeChanges) Modifier.animateContentSize(motion.iosTween(IosMotion.Standard)) else Modifier).semantics {
                            if (!title.isNullOrBlank()) paneTitle = title
                        },
                        shape = RoundedCornerShape(topStart = GlassTokens.sheetRadius, topEnd = GlassTokens.sheetRadius),
                        color = container, contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                    ) {
                        Column(Modifier.fillMaxWidth().navigationBarsPadding()
                            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp)) {
                            if (!title.isNullOrBlank()) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    if (showBackButton) GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack,
                                        stringResource(android.R.string.cancel), dismissAnimated,
                                        Modifier.padding(end = 6.dp), enabled = !dismissing)
                                    AnimatedContent(title, Modifier.weight(1f).padding(end = 8.dp),
                                        transitionSpec = { motion.contentTransform(durationMillis = IosMotion.Quick) },
                                        label = "sheet-title") { displayedTitle ->
                                        Text(displayedTitle.orEmpty(),
                                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    trailingContent?.invoke()
                                    if (showCloseButton) GlassIconButton(Icons.Default.Close,
                                        stringResource(android.R.string.cancel), dismissAnimated, enabled = !dismissing)
                                }
                            }
                            // Feedback takes space before scrollable content is measured.
                            Box(Modifier.weight(1f, fill = false)) { Column(Modifier.fillMaxWidth(), content = content) }
                            if (toast.activeSheet === sheetKey) {
                                ToastOverlay(toast, Modifier.fillMaxWidth(), topPadding = 12.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
