package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.PillShape

enum class AppButtonVariant { Primary, Secondary, Plain, Destructive }

private val AppButtonHeight = 48.dp
private val AppButtonShape = ContinuousRoundedShape(19.dp)

@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
) {
    val accent = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val animatedModifier = modifier.iosPressScale(interactionSource, pressedScale = 0.975f)

    when (variant) {
        AppButtonVariant.Plain -> androidx.compose.material3.TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = animatedModifier.heightIn(min = AppButtonHeight),
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            shape = AppButtonShape,
        ) {
            ButtonContent(label, icon, accent, enabled)
        }

        AppButtonVariant.Primary -> {
            val container = if (enabled) accent else accent.copy(alpha = 0.30f)
            val contentColor = if (enabled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.58f)
            }
            Surface(
                modifier = animatedModifier
                    .heightIn(min = AppButtonHeight)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                shape = AppButtonShape,
                color = container,
                contentColor = contentColor,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                ButtonRow(label, icon, contentColor, enabled = true)
            }
        }

        AppButtonVariant.Secondary -> StandardOutlinedButton(
            label = label,
            icon = icon,
            modifier = animatedModifier,
            interactionSource = interactionSource,
            enabled = enabled,
            color = MaterialTheme.colorScheme.onSurface,
            onClick = onClick,
        )

        AppButtonVariant.Destructive -> StandardOutlinedButton(
            label = label,
            icon = icon,
            modifier = animatedModifier,
            interactionSource = interactionSource,
            enabled = enabled,
            color = MaterialTheme.colorScheme.error,
            onClick = onClick,
        )
    }
}

@Composable
private fun StandardOutlinedButton(
    label: String,
    icon: ImageVector?,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(min = AppButtonHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = AppButtonShape,
        color = appMaterialColor(AppMaterial.Interactive),
        contentColor = color,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        ButtonRow(label, icon, color, enabled)
    }
}

@Composable
private fun ButtonRow(label: String, icon: ImageVector?, color: Color, enabled: Boolean) {
    Row(
        modifier = Modifier
            .heightIn(min = AppButtonHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        ButtonContent(label, icon, color, enabled)
    }
}

@Composable
private fun RowScope.ButtonContent(label: String, icon: ImageVector?, color: Color, enabled: Boolean) {
    val resolved = if (enabled) color else color.copy(alpha = 0.42f)
    if (icon != null) {
        Icon(icon, null, Modifier.size(18.dp), tint = resolved)
        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
    }
    Text(
        text = label,
        modifier = Modifier.weight(1f, fill = false),
        color = resolved,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = PillShape,
    accent: Color = MaterialTheme.colorScheme.onSurface,
    onImagery: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 15.dp),
    content: @Composable (contentColor: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (onImagery) {
        Color.White
    } else if (enabled) {
        accent
    } else {
        accent.copy(alpha = 0.42f)
    }
    GlassSurface(
        modifier = modifier
            .iosPressScale(interactionSource)
            .heightIn(min = AppButtonHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        tone = if (onImagery) GlassTone.OnImage else GlassTone.Neutral,
        blur = GlassBlur.None,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = AppButtonHeight)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            content(contentColor)
        }
    }
}

/** The compact visual sits inside a full 48dp Android touch target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onImagery: Boolean = false,
    iconSize: Dp = 18.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .iosPressScale(interactionSource, pressedScale = 0.94f)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val buttonContent: @Composable () -> Unit = {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        if (onImagery) {
            GlassSurface(
                modifier = Modifier.size(36.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                tone = GlassTone.OnImage,
                blur = GlassBlur.None,
                contentColor = Color.White,
                content = buttonContent,
            )
        } else {
            AppMaterialSurface(
                modifier = Modifier.size(36.dp),
                material = AppMaterial.Interactive,
                shape = androidx.compose.foundation.shape.CircleShape,
                contentColor = MaterialTheme.colorScheme.onSurface,
                content = buttonContent,
            )
        }
    }
}
