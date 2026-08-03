package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.owlcoder.animeschedule.ui.theme.PillShape

enum class AppButtonVariant { Primary, Secondary, Plain, Destructive }

/** 44dp visible controls with an intentionally calm label/icon rhythm. */
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
        AppButtonVariant.Primary, AppButtonVariant.Secondary, AppButtonVariant.Plain -> accent
        AppButtonVariant.Destructive -> MaterialTheme.colorScheme.error
    }

    if (variant == AppButtonVariant.Plain) {
        androidx.compose.material3.TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 44.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) {
            ButtonContent(label, icon, contentColor, enabled)
        }
        return
    }

    GlassSurface(
        modifier = modifier
            .height(44.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        shape = PillShape,
        tone = if (variant == AppButtonVariant.Primary) GlassTone.Accent else GlassTone.Neutral,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.height(44.dp).padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            ButtonContent(label, icon, contentColor, enabled)
        }
    }
}

@Composable
private fun ButtonContent(label: String, icon: ImageVector?, color: Color, enabled: Boolean) {
    val resolved = if (enabled) color else color.copy(alpha = 0.42f)
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = resolved,
        )
    }
    Text(
        text = label,
        color = resolved,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = PillShape,
    accent: Color = MaterialTheme.colorScheme.primary,
    onImagery: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
    content: @Composable (contentColor: Color) -> Unit,
) {
    val contentColor = if (onImagery) Color.White else if (enabled) accent else accent.copy(alpha = 0.42f)
    GlassSurface(
        modifier = modifier
            .height(44.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        shape = shape,
        tone = if (onImagery) GlassTone.OnImage else GlassTone.Accent,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.height(44.dp).padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            content(contentColor)
        }
    }
}

/** A 40dp glass disc nested inside a full 48dp accessibility target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onImagery: Boolean = false,
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.size(40.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            tone = if (onImagery) GlassTone.OnImage else GlassTone.Neutral,
            contentColor = if (onImagery) Color.White else MaterialTheme.colorScheme.onSurface,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}
