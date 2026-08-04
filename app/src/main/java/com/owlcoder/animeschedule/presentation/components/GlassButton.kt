package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.PillShape

enum class AppButtonVariant { Primary, Secondary, Plain, Destructive }

private val AppButtonHeight = 44.dp
private val AppButtonShape = ContinuousRoundedShape(14.dp)

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
                    .height(AppButtonHeight)
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
            .height(AppButtonHeight)
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
            .height(AppButtonHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        ButtonContent(label, icon, color, enabled)
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
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
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
            .height(AppButtonHeight)
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
                .height(AppButtonHeight)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            content(contentColor)
        }
    }
}

/** 36dp visible lens nested inside a 44dp accessibility target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onImagery: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .iosPressScale(interactionSource, pressedScale = 0.94f)
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.size(36.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            tone = if (onImagery) GlassTone.OnImage else GlassTone.Neutral,
            blur = GlassBlur.None,
            contentColor = if (onImagery) Color.White else MaterialTheme.colorScheme.onSurface,
        ) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
