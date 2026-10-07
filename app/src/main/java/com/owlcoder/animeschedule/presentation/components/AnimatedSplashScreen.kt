package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import kotlin.math.cos
import kotlin.math.sin

/** Loading stays inside the content pane so navigation remains available on a slow connection. */
@Composable
fun AnimatedSplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier.background(Brush.radialGradient(listOf(
            lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary, .07f), MaterialTheme.colorScheme.background,
        ))).testTag("schedule-loading").semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                OrbitLoader(Modifier.fillMaxSize())
                Surface(shape = ContinuousRoundedShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        AnimeScheduleMark(Modifier.size(52.dp))
                    }
                }
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.splash_title), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** One draw-only animation drives the orbit; reduced motion uses a static progress symbol. */
@Composable
internal fun OrbitLoader(modifier: Modifier = Modifier) {
    val phase = if (LocalMotionPolicy.current.animationsEnabled) {
        rememberInfiniteTransition(label = "orbit-loader").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "orbit-phase",
        )
    } else remember { mutableFloatStateOf(0f) }
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier.progressSemantics()) {
        val stroke = size.minDimension * .024f
        val inset = stroke * 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(primary.copy(alpha = .12f), 0f, 360f, false, Offset(inset, inset), arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round))
        repeat(3) { index ->
            val angle = phase.value + index * 120f - 90f
            drawArc(primary.copy(alpha = 1f - index * .24f), angle, 58f, false,
                Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val radians = Math.toRadians(angle.toDouble())
            val radius = (size.minDimension - inset * 2) / 2
            drawCircle(primary.copy(alpha = 1f - index * .24f), stroke * 1.5f,
                Offset(center.x + cos(radians).toFloat() * radius, center.y + sin(radians).toFloat() * radius))
        }
    }
}

@Composable
fun AnimeScheduleMark(modifier: Modifier = Modifier, scale: Float = 1f) {
    Image(painterResource(R.drawable.ic_app_icon), null, modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}
