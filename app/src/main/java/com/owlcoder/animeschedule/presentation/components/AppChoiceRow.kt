package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A shared leading icon, label and reserved check column keep every picker aligned. */
@Composable
fun AppChoiceRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    selectionRole: Role = Role.RadioButton,
    iconBadge: Boolean = false,
) {
    val motion = LocalMotionPolicy.current
    val source = remember { MutableInteractionSource() }
    val fill by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        motion.iosTween(IosMotion.Quick), label = "choice-fill")
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else MaterialTheme.colorScheme.outlineVariant,
        motion.iosTween(IosMotion.Quick), label = "choice-border")
    val interaction = if (selectionRole == Role.Checkbox) Modifier.toggleable(selected, source, indication = null, role = selectionRole, onValueChange = { onClick() })
        else Modifier.selectable(selected, source, indication = null, role = selectionRole, onClick = onClick)
    Surface(modifier.fillMaxWidth().iosPressScale(source, pressedScale = .99f).clip(MaterialTheme.shapes.large).then(interaction),
        shape = MaterialTheme.shapes.large,
        color = fill,
        border = BorderStroke(.7.dp, border)) {
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (iconBadge) {
                Surface(Modifier.size(36.dp), shape = MaterialTheme.shapes.medium,
                    color = iconTint.copy(alpha = .10f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(20.dp), tint = iconTint)
                    }
                }
            } else Icon(icon, null, Modifier.size(22.dp), tint = iconTint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            else Spacer(Modifier.size(20.dp))
        }
    }
}
