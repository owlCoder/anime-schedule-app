package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class SegmentOption(val label: String, val icon: ImageVector, val count: Int? = null)

/** Each segment centers its leading icon and wrapping label as a single group. */
@Composable
fun AppSegmentedControl(
    options: List<SegmentOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = LocalMotionPolicy.current
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, option ->
                val selected = selectedIndex == index
                val fill by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    motion.iosTween(IosMotion.Quick),
                    label = "segment-fill"
                )
                val tint =
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    modifier = Modifier.weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .selectable(selected, role = Role.Tab) { onSelect(index) },
                    shape = MaterialTheme.shapes.medium, color = fill, contentColor = tint,
                    border = if (selected) BorderStroke(
                        .5.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    ) else null,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        Icon(option.icon, null, modifier = Modifier.size(18.dp))
                        Text(option.label + (option.count?.takeIf { it > 0 }?.let { " · $it" } ?: ""),
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
