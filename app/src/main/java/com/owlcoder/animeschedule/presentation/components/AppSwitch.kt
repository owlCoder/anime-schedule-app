package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/** 46x28 visual control centered in a 50x44 accessibility target. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val motion = LocalMotionPolicy.current
    val interactionSource = remember { MutableInteractionSource() }
    val dark = MaterialTheme.colorScheme.background.luminance() < .35f
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.46f)
            checked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "switch-track",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = motion.iosSpring(),
        label = "switch-thumb",
    )
    val thumbColor by animateColorAsState(
        targetValue = when {
            dark && !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f)
            dark && checked -> MaterialTheme.colorScheme.background
            dark -> MaterialTheme.colorScheme.onSurfaceVariant
            enabled -> Color.White
            else -> Color.White.copy(alpha = .76f)
        },
        animationSpec = motion.iosTween(IosMotion.Quick),
        label = "switch-thumb-color",
    )

    Box(
        modifier = modifier
            .size(width = 50.dp, height = 44.dp)
            .then(if (onCheckedChange != null) Modifier.toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(CircleShape)
                .background(trackColor),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                    .size(24.dp)
                    .shadow(1.5.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}
