package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.GlassTokens
import com.owlcoder.animeschedule.ui.theme.PillShape

enum class AppButtonVariant { Primary, Secondary, Plain, Destructive }

/** One action vocabulary for the whole app. Visible controls stay 44dp; hit targets stay 48dp. */
@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    val contentColor = when (variant) {
        AppButtonVariant.Primary -> MaterialTheme.colorScheme.onPrimary
        AppButtonVariant.Secondary, AppButtonVariant.Plain -> accent
        AppButtonVariant.Destructive -> MaterialTheme.colorScheme.error
    }
    if (variant == AppButtonVariant.Plain) {
        androidx.compose.material3.TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(18.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(if (icon != null) 8.dp else 0.dp))
            androidx.compose.material3.Text(label, color = contentColor)
        }
    } else {
        val tone = if (variant == AppButtonVariant.Primary) GlassTone.Accent else GlassTone.Neutral
        GlassSurface(
            modifier = modifier
                .heightIn(min = 48.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { role = Role.Button },
            shape = MaterialTheme.shapes.small,
            tone = tone,
            contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.45f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) Icon(icon, null, Modifier.size(18.dp))
                androidx.compose.material3.Text(label, color = if (enabled) contentColor else contentColor.copy(alpha = 0.45f), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Compatibility action used by existing screens while they migrate to AppButton. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    accent: Color = MaterialTheme.colorScheme.primary,
    onImagery: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable (contentColor: Color) -> Unit,
) {
    val contentColor = if (onImagery) Color.White else if (enabled) accent else accent.copy(alpha = 0.45f)
    GlassSurface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        shape = shape,
        tone = if (onImagery) GlassTone.OnImage else GlassTone.Accent,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) { content(contentColor) }
    }
}

/** Icon-only chrome action with a fixed 48dp accessibility target and 22dp icon. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onImagery: Boolean = false,
) {
    GlassSurface(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        shape = androidx.compose.foundation.shape.CircleShape,
        tone = if (onImagery) GlassTone.OnImage else GlassTone.Neutral,
        contentColor = if (onImagery) Color.White else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}
