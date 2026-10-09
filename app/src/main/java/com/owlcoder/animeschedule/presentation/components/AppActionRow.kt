package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.LocalCompactLayout

/** Menu actions share one icon column; labels wrap freely at larger text sizes. */
@Composable
fun AppActionRow(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier,
    subtitle: String? = null, enabled: Boolean = true, showChevron: Boolean = true) {
    val compact = LocalCompactLayout.current
    val content = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .42f)
    Surface(modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.heightIn(min = if (compact) 48.dp else 60.dp).padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(if (compact) 32.dp else 36.dp), shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) .10f else .04f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = if (enabled) MaterialTheme.colorScheme.primary else content) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = content)
                subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else content) }
            }
            if (showChevron) Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = content.copy(alpha = .6f))
        }
    }
}
