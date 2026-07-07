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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
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
 * Full-screen animated canvas: a large "AS" brand glyph (letterform built from rounded
 * rects, same grid-based approach as tapiz-lms's tilting "T") skews/drifts in 3D behind
 * [content], instead of a static watermark.
 */
@Composable
private fun AnimatedLetterBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
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

            // "AS" glyph on a 96×64 grid, built centred on the origin so the shear
            // transform pivots around the glyph's own centre, then translated to (cx, cy).
            // "A": two angled strokes meeting at an apex + a crossbar, approximated with
            // rotated rounded rects. "S": a compact stacked-bar zigzag to the glyph's right,
            // sitting slightly raised (superscript-style).
            val glyphSize = size.minDimension * 1.25f
            val u = glyphSize / 96f
            val halfW = 48f * u
            val halfH = 32f * u

            fun rect(cxLocal: Float, cyLocal: Float, w0: Float, h0: Float, r: Float, rotationDeg: Float = 0f) =
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = -w0 / 2f, top = -h0 / 2f,
                            right = w0 / 2f, bottom = h0 / 2f,
                            cornerRadius = CornerRadius(r, r)
                        )
                    )
                }.let { path ->
                    val m = Matrix().apply {
                        translate(cxLocal - halfW, cyLocal - halfH)
                        if (rotationDeg != 0f) {
                            val rad = Math.toRadians(rotationDeg.toDouble())
                            val cosR = cos(rad).toFloat()
                            val sinR = sin(rad).toFloat()
                            this[0, 0] = cosR; this[1, 0] = sinR
                            this[0, 1] = -sinR; this[1, 1] = cosR
                        }
                    }
                    Path().apply { addPath(path, Offset.Zero); transform(m) }
                }

            // Left leg of "A": tall bar rotated to lean inward.
            val leftLeg = rect(20f * u, 32f * u, 12f * u, 56f * u, 4f * u, rotationDeg = -14f)
            // Right leg of "A": mirrored lean.
            val rightLeg = rect(44f * u, 32f * u, 12f * u, 56f * u, 4f * u, rotationDeg = 14f)
            // Crossbar joining the two legs.
            val crossbar = rect(32f * u, 38f * u, 30f * u, 9f * u, 3f * u)
            // "S": three short offset bars forming a zigzag, placed to the right and
            // slightly higher (superscript feel).
            val sTop = rect(74f * u, 12f * u, 22f * u, 9f * u, 4.5f * u)
            val sMid = rect(70f * u, 28f * u, 22f * u, 9f * u, 4.5f * u)
            val sBottom = rect(74f * u, 44f * u, 22f * u, 9f * u, 4.5f * u)
            val sLeftLink = rect(65f * u, 20f * u, 9f * u, 9f * u, 4.5f * u)
            val sRightLink = rect(83f * u, 36f * u, 9f * u, 9f * u, 4.5f * u)

            val brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = glyphAlpha * 0.5f),
                    primary.copy(alpha = glyphAlpha),
                    primary.copy(alpha = glyphAlpha * 0.5f)
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
                listOf(leftLeg, rightLeg, crossbar, sTop, sMid, sBottom, sLeftLink, sRightLink)
                    .forEach { drawPath(it, brush = brush) }
            }
        }
        content()
    }
}
