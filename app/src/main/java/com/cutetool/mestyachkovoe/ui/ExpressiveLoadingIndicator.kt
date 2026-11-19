package com.cutetool.mestyachkovoe.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp
) {
    val transition = rememberInfiniteTransition(label = "expressiveLoading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val morph by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1600
                0f at 0 using FastOutSlowInEasing
                1f at 440 using FastOutSlowInEasing
                0.35f at 880 using FastOutSlowInEasing
                0.9f at 1160 using FastOutSlowInEasing
                0f at 1600 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "morph"
    )

    Canvas(modifier.size(size).semantics { contentDescription = "Загрузка" }) {
        val base = this.size.minDimension
        val width = base * (0.62f + 0.24f * morph)
        val height = base * (0.62f + 0.04f * morph)
        val radius = minOf(width, height) * (0.5f - 0.22f * morph)

        rotate(degrees = rotation, pivot = center) {
            drawRoundRect(
                color = color,
                topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}
