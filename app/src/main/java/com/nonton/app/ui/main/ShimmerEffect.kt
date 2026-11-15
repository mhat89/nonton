package com.nonton.app.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** A continuously animated shimmer placeholder. */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF1E1E1E),
    highlightColor: Color = Color(0xFF2A2A2A)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val animation = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(baseColor, highlightColor, baseColor),
                    start = Offset(animation.value * 600f, 0f),
                    end = Offset(animation.value * 600f + 600f, 600f)
                )
            )
    )
}
