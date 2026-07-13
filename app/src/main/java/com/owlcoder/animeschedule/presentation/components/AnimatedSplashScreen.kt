package com.owlcoder.animeschedule.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen animated brand splash shown while the app's first data load is in flight
 * (mirrors tapiz-lms's SessionLoadingScreen + AnimatedLetterBackground). The main UI
 * (schedule + nav bar) is NOT shown until loading finishes; a pull-to-refresh later uses
 * the LoadingShimmer skeleton instead, never this. A large "AS" brand glyph tilts/drifts
 * behind an eyebrow + title text, same recipe as tapiz-lms's tilting "T".
 */
@Composable
fun AnimatedSplashScreen(modifier: Modifier = Modifier) {
    AnimatedLetterBackground(modifier.fillMaxSize()) {
        val primary = MaterialTheme.colorScheme.primary
        val transition = rememberInfiniteTransition(label = "splashPulse")
        val pulseScale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulseScale"
        )
        val pulseAlpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulseAlpha"
        )

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 56.dp)
                .size(36.dp)
                .scale(pulseScale)
        ) {
            drawCircle(
                color = primary.copy(alpha = pulseAlpha),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.7f)
                .padding(start = 24.dp, end = 24.dp, bottom = 56.dp)
        ) {
            Text(
                text = stringResource(R.string.splash_eyebrow),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.splash_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 3
            )
        }
    }
}

/**
 * Full-screen animated canvas: a large "S" brand glyph (the same amber Niti
 * accent thread as the launcher icon — see `ic_launcher_foreground.xml`, path
 * "M67,30 C67,30 41,26 41,41 C41,54 67,54 67,67 C67,82 41,78 41,78" on a
 * 108-unit grid) skews/drifts in 3D behind [content], instead of a static
 * watermark.
 */
@Composable
private fun AnimatedLetterBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val brandAmber = Color(0xFFE8A33D)
    val isDark = true // app is dark-only (see AnimeScheduleTheme)

    val transition = rememberInfiniteTransition(label = "loadingLetterDrift")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(14_000, easing = LinearEasing)),
        label = "loadingLetterTurn"
    )
    val driftT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(19_000, easing = LinearEasing)),
        label = "loadingLetterDriftT"
    )

    val glyphAlpha = if (isDark) 0.30f else 0.22f

    Box(modifier = modifier.background(Brush.verticalGradient(listOf(background, surface)))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val turnAngle = turn * 2f * Math.PI.toFloat()
            val shearX = cos(turnAngle) * 0.55f
            val shearY = sin(turnAngle * 0.5f) * 0.12f
            val driftAngle = driftT * 2f * Math.PI.toFloat()
            val cx = w * 0.5f + cos(driftAngle) * w * 0.10f
            val cy = h * 0.42f + sin(driftAngle) * h * 0.08f

            // "S" glyph, same shape as the launcher icon's accent thread (108-unit
            // grid path scaled to this canvas), centred on the origin so the shear
            // transform pivots around the glyph's own centre, then translated to (cx, cy).
            val glyphSize = size.minDimension * 1.25f
            val u = glyphSize / 108f
            val halfW = 54f * u
            val halfH = 54f * u

            val sPath = Path().apply {
                moveTo(67f * u, 30f * u)
                cubicTo(67f * u, 30f * u, 41f * u, 26f * u, 41f * u, 41f * u)
                cubicTo(41f * u, 54f * u, 67f * u, 54f * u, 67f * u, 67f * u)
                cubicTo(67f * u, 82f * u, 41f * u, 78f * u, 41f * u, 78f * u)
            }.let { path ->
                val m = Matrix().apply { translate(-halfW, -halfH) }
                Path().apply { addPath(path, Offset.Zero); transform(m) }
            }

            val brush = Brush.linearGradient(
                colors = listOf(
                    brandAmber.copy(alpha = glyphAlpha * 0.5f),
                    brandAmber.copy(alpha = glyphAlpha),
                    brandAmber.copy(alpha = glyphAlpha * 0.5f)
                ),
                start = Offset(-halfW, -halfH),
                end = Offset(halfW, halfH)
            )

            withTransform({
                translate(cx, cy)
                transform(
                    Matrix().apply {
                        this[1, 0] = shearX
                        this[0, 1] = -shearY
                    }
                )
            }) {
                drawPath(
                    sPath,
                    brush = brush,
                    style = Stroke(width = 13.5f * u, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}
