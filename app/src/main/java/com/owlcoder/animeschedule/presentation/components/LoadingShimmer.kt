package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.PillShape
import com.owlcoder.animeschedule.ui.theme.AppDensity

@Composable
fun ShimmerBrush(): Brush {
    val reduceMotion = rememberReduceMotion()
    val color = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    if (reduceMotion) {
        return SolidColor(color.copy(alpha = 0.72f))
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = listOf(color, highlight, color),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

/** Compact loading skeleton that mirrors the shared grouped-material language. */
@Composable
fun LoadingShimmer(modifier: Modifier = Modifier) {
    val brush = ShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDensity.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(AppDensity.sectionGap),
    ) {
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(110.dp).height(30.dp).clip(MaterialTheme.shapes.small).background(brush))
                Box(Modifier.width(150.dp).height(14.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
            }
            Box(Modifier.size(48.dp).clip(PillShape).background(brush))
        }
        // Compact horizontal feature placeholder; it must not consume the viewport.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp, max = 152.dp)
                .clip(MaterialTheme.shapes.large)
                .background(brush)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .aspectRatio(0.72f)
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(brush)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth(0.74f).height(18.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
                Box(Modifier.fillMaxWidth(0.48f).height(12.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(86.dp).height(32.dp).clip(PillShape).background(brush))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.width(116.dp).height(18.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
                Box(Modifier.width(70.dp).height(28.dp).clip(PillShape).background(brush))
            }
            // Shared timeline rows instead of independent large cards.
            repeat(4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(42.dp).height(12.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
                    Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(brush))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.fillMaxWidth(0.84f).height(14.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
                        Box(Modifier.fillMaxWidth(0.42f).height(10.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
                    }
                    Box(Modifier.size(32.dp).clip(PillShape).background(brush))
                }
            }
        }
    }
}

/** A single compact loading row for lists that do not need a full-screen skeleton. */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    val brush = ShimmerBrush()
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(MaterialTheme.shapes.small).background(brush))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth(0.78f).height(14.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
            Box(Modifier.fillMaxWidth(0.42f).height(11.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
        }
    }
}
