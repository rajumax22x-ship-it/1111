package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold

@Composable
fun JarvisAudioVisualizer(isListening: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")

    val barCount = 5
    val heights = remember { mutableStateOf(List(barCount) { 0.2f }) }

    if (isListening) {
        val animProgress = infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wavePulse"
        )

        Row(
            modifier = modifier
                .height(36.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until barCount) {
                val multiplier = ((i % 3) + 1) * 0.3f
                val currentHeight = (animProgress.value * multiplier).coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(currentHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i % 2 == 0) JarvisCyan else JarvisGold)
                )
            }
        }
    }
}
