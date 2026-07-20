package com.cappielloantonio.tempo.ui.home

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun PassiveCornerGlowBackground(
    modifier: Modifier = Modifier,
    containerColor: Color,
    glowColor: Color = Color(0xFFC43328),
    baseRadius: Float = 1600f,
    corner: GlowCorner = GlowCorner.TOP_LEFT,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(containerColor)
            .passiveCornerGlow(
                glowColor = glowColor,
                baseRadius = baseRadius,
                corner = corner,
            )
    ) {
        content()
    }
}

fun Modifier.passiveCornerGlow(
    glowColor: Color = Color.Cyan,
    baseRadius: Float = 800f,
    corner: GlowCorner = GlowCorner.TOP_LEFT,
) = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")

    val radiusScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "radius_scale",
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha_scale",
    )

    this.drawBehind {
        val centerOffset = when (corner) {
            GlowCorner.TOP_LEFT -> Offset(0f, 0f)
            GlowCorner.TOP_RIGHT -> Offset(size.width, 0f)
            GlowCorner.BOTTOM_LEFT -> Offset(0f, size.height)
            GlowCorner.BOTTOM_RIGHT -> Offset(size.width, size.height)
        }

        val brush = Brush.radialGradient(
            0.0f to glowColor.copy(alpha = alpha),
            0.45f to glowColor.copy(alpha = alpha * 0.55f),
            0.75f to glowColor.copy(alpha = alpha * 0.18f),
            1.0f to Color.Transparent,
            center = centerOffset,
            radius = baseRadius * radiusScale,
        )

        drawRect(brush = brush)
    }
}

enum class GlowCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}
