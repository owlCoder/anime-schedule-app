package com.owlcoder.animeschedule.presentation.components

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.owlcoder.animeschedule.ui.theme.GlassTokens

/**
 * Stable modal content surface.
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
    @Suppress("UNUSED_PARAMETER") showDragHandle: Boolean = false,
    sheetGesturesEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    val container = if (dark) Color(0xFF0D0D0F) else Color(0xFFFBFBFD)
    val scrim = Color.Black.copy(alpha = if (dark) 0.42f else 0.26f)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
    ) {
        AppSheetBlurBehind()
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
            content()
        }
    }
}

@Composable
private fun AppSheetBlurBehind() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window?.attributes = window?.attributes?.apply {
            blurBehindRadius = 32
        }
        onDispose {
            window?.attributes = window?.attributes?.apply {
                blurBehindRadius = 0
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
    }
}

@Composable
fun AppSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 8.dp, bottom = 5.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .requiredHeight(5.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)),
        )
    }
}
