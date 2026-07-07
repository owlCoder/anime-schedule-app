package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.PillShape

/**
 * App-wide "glass" button: a flat, translucent frosted pill/surface with a hairline
 * accent-tinted border — the shared interaction language across the app instead of solid
 * filled buttons. [accent] drives the tint (defaults to `primary`); [onImagery] makes the
 * glass read against photo/cover imagery (brighter white sheen) rather than a flat surface.
 *
 * Content (icon + label) is provided by the caller; use [glassContentColor] for the
 * matching foreground tint.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = PillShape,
    accent: Color = MaterialTheme.colorScheme.primary,
    onImagery: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable (contentColor: Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val fill: Color
    val border: Color
    val contentColor: Color
    if (onImagery) {
        fill = Color.White.copy(alpha = if (enabled) 0.16f else 0.08f)
        border = Color.White.copy(alpha = 0.28f)
        contentColor = Color.White.copy(alpha = if (enabled) 1f else 0.6f)
    } else {
        fill = accent.copy(alpha = if (enabled) 0.14f else 0.07f)
        border = accent.copy(alpha = if (enabled) 0.35f else 0.18f)
        contentColor = if (enabled) accent else accent.copy(alpha = 0.5f)
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        content(contentColor)
    }
}
