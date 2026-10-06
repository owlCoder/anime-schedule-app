package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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

/** Icons sit above labels so translated tabs remain readable at larger font sizes. */
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
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 58.dp)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                    ) {
                        Icon(option.icon, null, modifier = Modifier.size(20.dp))
                        Text(option.label + (option.count?.takeIf { it > 0 }?.let { " · $it" }
                            ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
