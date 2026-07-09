package com.as307.aryaa.ui.screens.fakecall

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily

@Composable
fun FakeCallRingingScreen(
    viewModel: FakeCallViewModel,
    onDecline: () -> Unit,
    onAnswer: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Auto-navigate back on Ended state after 500ms
    androidx.compose.runtime.LaunchedEffect(uiState) {
        if (uiState is FakeCallUiState.Ended) {
            kotlinx.coroutines.delay(500)
            onDecline()
        }
    }

    // Retrieve caller name, defaulting to "Maa"
    val callerName = when (val state = uiState) {
        is FakeCallUiState.Ringing -> state.callerName
        else -> "Maa"
    }

    // Initialize and handle RingtonePlayer lifecycle
    val ringtonePlayer = remember { RingtonePlayer(context) }
    DisposableEffect(Unit) {
        ringtonePlayer.play()
        onDispose {
            ringtonePlayer.stop()
        }
    }

    // Intercept hardware/system back button and end call cleanly
    BackHandler {
        viewModel.endCall()
        onDecline()
    }

    // Fullscreen portrait, pure black background (simulating stock Android call screen)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Caller Profile
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            // Large circular avatar with the first letter of caller's name
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF3C4043), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = callerName.firstOrNull()?.uppercase() ?: "M",
                    color = Color.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Caller name
            Text(
                text = callerName,
                color = Color.White,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mobile label
            Text(
                text = "Mobile",
                color = Color(0xFF888888),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // Bottom Section: Answer and Decline Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        viewModel.endCall()
                        onDecline()
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFFE53935), // Red
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "Decline call",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Decline",
                    color = Color.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }

            // Answer Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        viewModel.answerCall()
                        onAnswer()
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF43A047), // Green
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Answer call",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Answer",
                    color = Color.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }
        }
    }
}
