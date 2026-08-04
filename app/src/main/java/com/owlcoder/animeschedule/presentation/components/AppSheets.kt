package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.GlassTokens

/**
 * Content-layer sheet. Liquid Glass is intentionally not used for the sheet body: Apple places
 * glass on navigation and interactive controls, while scoped modal content uses a stable standard
 * material so text and controls never compete with the parent view underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    title: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    val container = if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val edge = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val scrim = Color.Black.copy(alpha = if (dark) 0.40f else 0.24f)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = ContinuousRoundedShape(GlassTokens.sheetRadius),
        containerColor = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = scrim,
        tonalElevation = 0.dp,
        dragHandle = { AppSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, edge, ContinuousRoundedShape(GlassTokens.sheetRadius))
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .padding(bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    trailingContent?.invoke()
                }
            }
            content()
        }
    }
}

@Composable
fun AppSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 7.dp, bottom = 4.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .requiredHeight(4.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)),
        )
    }
}
