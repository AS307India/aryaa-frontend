package com.as307.aryaa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.screens.sos.SosUiState

@Composable
fun SosButton(
    state: SosUiState,
    onHoldStart: () -> Unit,
    onHoldRelease: () -> Unit,
    modifier: Modifier = Modifier,
    isPracticeMode: Boolean = false
) {
    val latestState by rememberUpdatedState(state)
    val latestOnHoldStart by rememberUpdatedState(onHoldStart)
    val latestOnHoldRelease by rememberUpdatedState(onHoldRelease)

    // Determine background color based on Active state
    val isActive = state is SosUiState.Active
    val buttonColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF2E7D32) else Color(0xFFD32F2F), // Emerald vs Crimson
        animationSpec = tween(durationMillis = 500),
        label = "buttonColor"
    )

    // Pulse Scale animation using infiniteTransition
    val isUnderTest = remember { com.as307.aryaa.util.TestEnv.isUnderTest }
    val pulseScale by if (isUnderTest) {
        remember { mutableStateOf(1f) }
    } else if (state is SosUiState.Holding || state is SosUiState.Countdown) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    // Progress Arc animation
    val targetProgress = if (state is SosUiState.Holding || state is SosUiState.Countdown) 360f else 0f
    val progressArc by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (targetProgress == 360f) {
            tween(durationMillis = 3000, easing = LinearEasing)
        } else {
            snap() // Instant reset
        },
        label = "progressArc"
    )

    // DrawBehind colors
    val ringColor = if (isActive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val ringAlpha = if (state is SosUiState.Holding || state is SosUiState.Countdown) 1.0f else 0.15f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(180.dp)
            .scale(pulseScale)
            .drawBehind {
                val strokeWidth = 8.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth
                if (isActive) {
                    // Solid Emerald outer ring
                    drawCircle(
                        color = ringColor,
                        radius = radius + strokeWidth / 2,
                        style = Stroke(width = strokeWidth)
                    )
                } else if (state is SosUiState.Holding || state is SosUiState.Countdown) {
                    // Sweeping Crimson outer ring
                    drawArc(
                        color = ringColor.copy(alpha = ringAlpha),
                        startAngle = -90f,
                        sweepAngle = progressArc,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    // Subtle outer ring
                    drawCircle(
                        color = ringColor.copy(alpha = ringAlpha),
                        radius = radius + strokeWidth / 2,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
            .clip(CircleShape)
            .background(buttonColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // Only allow interactions when Idle or Cancelled
                        if (latestState is SosUiState.Idle || latestState is SosUiState.Cancelled) {
                            try {
                                latestOnHoldStart()
                                tryAwaitRelease()
                            } finally {
                                latestOnHoldRelease()
                            }
                        }
                    }
                )
            }
    ) {
        if (isPracticeMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
                    .background(Color(0xFF3B82F6), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PRACTICE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        when (state) {
            is SosUiState.Triggering -> {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
            is SosUiState.Countdown -> {
                Text(
                    text = state.secondsLeft.toString(),
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif
                )
            }
            is SosUiState.Active -> {
                Text(
                    text = "SAFE",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif
                )
            }
            else -> {
                Text(
                    text = "SOS",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}
