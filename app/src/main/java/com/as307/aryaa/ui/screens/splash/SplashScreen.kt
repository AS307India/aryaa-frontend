package com.as307.aryaa.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.as307.aryaa.ui.navigation.Destination
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.AryaaTheme
import com.as307.aryaa.ui.theme.AryaaTypography

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                SplashNavigationEvent.NavigateToHome -> onNavigate(Destination.Home.route)
                SplashNavigationEvent.NavigateToLogin -> onNavigate(Destination.Login.createRoute())
            }
        }
    }

    SplashContent()
}

@Composable
fun SplashContent() {
    val isUnderTest = com.as307.aryaa.util.TestEnv.isUnderTest
    remember { android.util.Log.d("AryaaTest", "SplashContent isUnderTest: $isUnderTest") }
    // Pulsing saffron dot animation: 0.5 to 1.0 alpha over a 2s loop (1s pulse in, 1s pulse out)
    val alpha = if (isUnderTest) {
        1.0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "SosPulse")
        val alphaState by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "SosPulseAlpha"
        )
        alphaState
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AryaaColors.Navy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Pulsing Saffron dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .alpha(alpha)
                        .background(color = AryaaColors.Saffron, shape = CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "ARYAA",
                    style = AryaaTypography.displayLarge.copy(fontSize = 48.sp),
                    color = AryaaColors.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle "आर्या" in Saffron
            Text(
                text = "आर्या",
                style = AryaaTypography.titleLarge.copy(fontSize = 24.sp),
                color = AryaaColors.Saffron
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    AryaaTheme {
        SplashContent()
    }
}
