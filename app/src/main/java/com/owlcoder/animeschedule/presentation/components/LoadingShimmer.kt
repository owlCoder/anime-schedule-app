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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.PillShape

@Composable
fun ShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
        label = "shimmer_translate"
    )
    val color = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    return Brush.linearGradient(
        colors = listOf(color, highlight, color),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

/**
 * Loading skeleton that mirrors the current Schedule home layout: a big hero-card
 * placeholder + dot indicators, followed by two "section header + horizontal card row"
 * blocks (Today / Tomorrow), instead of the old vertical list-row cards.
 */
@Composable
fun LoadingShimmer() {
    val brush = ShimmerBrush()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        // Hero card placeholder.
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(brush)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { i ->
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(if (i == 0) 20.dp else 6.dp)
                            .clip(PillShape)
                            .background(brush)
                    )
                }
            }
        }
        // Two horizontal section placeholders.
        repeat(2) {
            ShimmerSection(brush)
        }
    }
}

@Composable
private fun ShimmerSection(brush: Brush) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section header row.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.width(120.dp).height(20.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
            Box(Modifier.width(56.dp).height(16.dp).clip(PillShape).background(brush))
        }
        // Horizontal card row.
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(3) {
                Column(
                    modifier = Modifier.width(140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(brush)
                    )
                    Box(Modifier.fillMaxWidth(0.9f).height(14.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
                    Box(Modifier.fillMaxWidth(0.5f).height(11.dp).clip(MaterialTheme.shapes.extraSmall).background(brush))
                }
            }
        }
    }
}
