package rs.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

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

@Composable
fun LoadingShimmer() {
    Column {
        repeat(4) {
            ShimmerCard()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShimmerCard() {
    val brush = ShimmerBrush()
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(12.dp)) {
            Box(Modifier.size(60.dp, 85.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
        }
    }
}
