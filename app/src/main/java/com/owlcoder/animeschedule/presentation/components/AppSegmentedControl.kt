package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection

data class SegmentOption(val label: String, val icon: ImageVector, val count: Int? = null)

/** Each segment centers its leading icon and wrapping label as a single group. */
@Composable
fun AppSegmentedControl(
    options: List<SegmentOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (options.isEmpty()) return
    val motion = LocalMotionPolicy.current
    val indicator = animateFloatAsState(selectedIndex.coerceIn(options.indices).toFloat(),
        motion.iosDecelerate(IosMotion.Standard), label = "segment-indicator")
    val gap = if (compact) 2.dp else 4.dp
    val fill = MaterialTheme.colorScheme.surface
    val shape = MaterialTheme.shapes.medium
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val lineHeight = with(density) { MaterialTheme.typography.labelMedium.lineHeight.toDp() }
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth / options.size.toFloat() < 96.dp * density.fontScale
            val track = Modifier.fillMaxWidth().padding(gap).selectableGroup().drawWithCache {
                val spacing = gap.toPx()
                val extent = ((if (stacked) size.height else size.width) - spacing * (options.size - 1)) / options.size
                val itemSize = if (stacked) Size(size.width, extent) else Size(extent, size.height)
                val outline = shape.createOutline(itemSize, layoutDirection, this)
                onDrawBehind {
                    val position = if (!stacked && rtl) options.lastIndex - indicator.value else indicator.value
                    val offset = position * (extent + spacing)
                    translate(left = if (stacked) 0f else offset, top = if (stacked) offset else 0f) {
                        drawOutline(outline, fill)
                    }
                }
            }
            val segment: @Composable (Int, SegmentOption, Modifier) -> Unit = { index, option, itemModifier ->
                val selected = selectedIndex == index
                val interaction = remember { MutableInteractionSource() }
                val tint by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    motion.iosTween(IosMotion.Quick), label = "segment-tint")
                Row(itemModifier.heightIn(min = 48.dp).clip(shape)
                    .selectable(selected, interaction, indication = null, role = Role.Tab, onClick = { onSelect(index) })
                    .iosPressScale(interaction)
                    .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 6.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp, Alignment.CenterHorizontally)) {
                    Icon(option.icon, null, Modifier.size(if (compact) 16.dp else 18.dp), tint = tint)
                    Text(option.label + (option.count?.takeIf { it > 0 }?.let { " · $it" } ?: ""),
                        Modifier.weight(1f, fill = false), color = tint, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            if (stacked) {
                val rowHeight = maxOf(48.dp, lineHeight * 2 + if (compact) 12.dp else 20.dp)
                Column(track, verticalArrangement = Arrangement.spacedBy(gap)) {
                    options.forEachIndexed { index, option -> segment(index, option, Modifier.fillMaxWidth().height(rowHeight)) }
                }
            } else Row(track.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                options.forEachIndexed { index, option -> segment(index, option, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}
