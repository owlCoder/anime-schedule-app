package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.owlcoder.animeschedule.ui.theme.GlassTokens
import com.owlcoder.animeschedule.ui.theme.LocalAmoledDark
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Stable modal content surface. The Activity backdrop host owns blur separately from the dialog,
 * so content switches reuse the same effect and disposal always releases it.
 *
 * Sheet drag gestures are disabled by default because nested scrollable content otherwise hands
 * its remaining drag to ModalBottomSheet at the top/bottom boundary. That makes a fully expanded
 * overlay visibly jump a few pixels while the user is only trying to scroll its list. Every app
 * sheet has an explicit back action, so locking the sheet position keeps navigation predictable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showBackButton: Boolean = true,
    showCloseButton: Boolean = false,
    sheetGesturesEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    val container = if (LocalAmoledDark.current) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh
    val scrim = Color.Black.copy(alpha = if (dark) 0.42f else 0.26f)
    val toast = LocalToast.current
    val sheetKey = remember { Any() }
    val backdrop = LocalSheetBackdrop.current
    DisposableEffect(backdrop, sheetKey) {
        backdrop?.attach(sheetKey)
        onDispose { backdrop?.detach(sheetKey) }
    }
    DisposableEffect(toast, sheetKey) {
        toast.attachSheet(sheetKey)
        onDispose { toast.detachSheet(sheetKey) }
    }

    ModalBottomSheet(
        onDismissRequest = {
            // Android Back can report dismissal after a rejected Hidden transition. Content
            // navigation may keep the sheet visible; do not close its owner in that case.
            if (!sheetState.isVisible) onDismissRequest()
        },
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = RoundedCornerShape(
            topStart = GlassTokens.sheetRadius,
            topEnd = GlassTokens.sheetRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
        ),
        containerColor = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = scrim,
        tonalElevation = 0.dp,
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            isAppearanceLightStatusBars = !dark,
            isAppearanceLightNavigationBars = !dark,
        ),
    ) {
        // Material 3 initializes these flags when the dialog is created, but does not
        // update them when a live theme preview changes an already-open sheet.
        val view = LocalView.current
        SideEffect {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) parent = parent.parent
            val window = (parent as? DialogWindowProvider)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        val ownsToast = toast.activeSheet === sheetKey
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showBackButton) {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(android.R.string.cancel),
                            onClick = onDismissRequest,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    Text(
                        text = title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    trailingContent?.invoke()
                    if (showCloseButton) {
                        GlassIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = stringResource(android.R.string.cancel),
                            onClick = onDismissRequest,
                        )
                    }
                }
            }
            // Measure feedback first and give scrollable content the remaining space.
            // A floating message could cover Save/Retry or the bottom of a short sheet.
            Box(Modifier.weight(1f, fill = false)) {
                Column(Modifier.fillMaxWidth(), content = content)
            }
            if (ownsToast && toast.current != null) {
                ToastOverlay(toast, Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
    }
}
