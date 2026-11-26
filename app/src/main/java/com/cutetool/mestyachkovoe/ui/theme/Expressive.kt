package com.cutetool.mestyachkovoe.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Material 3 Expressive shape family used across the app. */
object ExpressiveShapes {
    val card = RoundedCornerShape(
        topStart = 30.dp,
        topEnd = 10.dp,
        bottomEnd = 30.dp,
        bottomStart = 10.dp
    )
    val cardFlipped = RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 30.dp,
        bottomEnd = 10.dp,
        bottomStart = 30.dp
    )
    val tile = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 8.dp,
        bottomEnd = 22.dp,
        bottomStart = 8.dp
    )
    val hero = RoundedCornerShape(
        topStart = 38.dp,
        topEnd = 12.dp,
        bottomEnd = 38.dp,
        bottomStart = 12.dp
    )
    val sheet = RoundedCornerShape(
        topStart = 38.dp,
        topEnd = 12.dp
    )
    val button = CircleShape
    val pill = CircleShape
    val arch = RoundedCornerShape(
        topStart = 80.dp,
        topEnd = 80.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp
    )
}

@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
