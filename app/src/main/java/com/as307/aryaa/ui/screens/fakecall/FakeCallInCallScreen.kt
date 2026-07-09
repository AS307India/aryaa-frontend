package com.as307.aryaa.ui.screens.fakecall

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.AryaaMono
import com.as307.aryaa.ui.theme.InterFamily

@Composable
fun FakeCallInCallScreen(
    viewModel: FakeCallViewModel,
    onCallEnded: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-navigate back on Ended state after 500ms
    androidx.compose.runtime.LaunchedEffect(uiState) {
        if (uiState is FakeCallUiState.Ended) {
            kotlinx.coroutines.delay(500)
            onCallEnded()
        }
    }

    val (callerName, duration) = when (val state = uiState) {
        is FakeCallUiState.InCall -> Pair(state.callerName, state.duration)
        else -> Pair("Maa", "0:00")
    }

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }

    // Intercept hardware/system back button and end call cleanly
    BackHandler {
        viewModel.endCall()
        onCallEnded()
    }

    // Full screen portrait, stock Android In-Call UI look (#1A1A1A background)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Profile and Duration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            // Small circular avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF3C4043), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = callerName.firstOrNull()?.uppercase() ?: "M",
                    color = Color.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Caller name
            Text(
                text = callerName,
                color = Color.White,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Monospace duration counter (AryaaMono style)
            Text(
                text = duration,
                color = Color(0xFF43A047), // Green timer
                style = AryaaMono.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )
        }

        // Center Section: Cosmetic call quality dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color.Gray.copy(alpha = 0.6f), shape = CircleShape)
                )
            }
        }

        // Bottom Section: Action buttons grid + Red End Call button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Action Grid: Mute, Keypad, Speaker, Hold (2x2 layout)
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallActionButton(
                        icon = Icons.Filled.MicOff,
                        label = "Mute",
                        isSelected = isMuted,
                        onClick = { isMuted = !isMuted }
                    )
                    CallActionButton(
                        icon = Icons.Filled.Dialpad,
                        label = "Keypad",
                        isSelected = false,
                        onClick = {}
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallActionButton(
                        icon = Icons.Filled.VolumeUp,
                        label = "Speaker",
                        isSelected = isSpeakerOn,
                        onClick = { isSpeakerOn = !isSpeakerOn }
                    )
                    CallActionButton(
                        icon = Icons.Filled.Pause,
                        label = "Hold",
                        isSelected = isOnHold,
                        onClick = { isOnHold = !isOnHold }
                    )
                }
            }

            // Red End Call Button
            FloatingActionButton(
                onClick = {
                    viewModel.endCall()
                    onCallEnded()
                },
                shape = CircleShape,
                containerColor = Color(0xFFE53935), // Red
                contentColor = Color.White,
                modifier = Modifier.size(64.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun CallActionButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(80.dp)
    ) {
        val backgroundColor = if (isSelected) Color.White else Color.Transparent
        val contentColor = if (isSelected) Color.Black else Color.White
        val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, Color(0xFF444444), CircleShape)

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(backgroundColor, shape = CircleShape)
                .then(borderModifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
