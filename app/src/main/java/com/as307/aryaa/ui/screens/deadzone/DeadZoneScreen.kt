package com.as307.aryaa.ui.screens.deadzone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadZoneScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeadZoneViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val durationPresets = listOf(60, 120, 240, 480) // 1h / 2h / 4h / 8h
    var selectedPreset by remember { mutableStateOf(60) }
    var isCustom by remember { mutableStateOf(false) }
    var customDurationText by remember { mutableStateOf("") }
    var durationError by remember { mutableStateOf<String?>(null) }

    fun activeDuration(): Int? {
        if (isCustom) {
            val parsed = customDurationText.toIntOrNull()
            if (parsed == null || parsed < 1 || parsed > 1440) return null
            return parsed
        }
        return selectedPreset
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dead Zone Check-In",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AryaaColors.White,
                        fontFamily = InterFamily
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AryaaColors.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AryaaColors.Navy
                )
            )
        },
        containerColor = AryaaColors.Navy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is DeadZoneUiState.Idle, is DeadZoneUiState.SettingDuration -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Icon Header
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Explanation Block
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Going Offline?",
                                color = AryaaColors.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFamily
                            )
                            Text(
                                text = "Set a timer before entering a no-signal area (trek, basement, rural road). If you don't check back in, your contacts will be automatically alerted.",
                                color = AryaaColors.Slate,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = InterFamily,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // Duration Selector segmented control
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Select Duration",
                                color = AryaaColors.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFamily
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(AryaaColors.NavyCard, RoundedCornerShape(12.dp))
                                    .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                durationPresets.forEach { minutes ->
                                    val isSelected = !isCustom && selectedPreset == minutes
                                    val label = when (minutes) {
                                        60 -> "1 Hr"
                                        120 -> "2 Hr"
                                        240 -> "4 Hr"
                                        480 -> "8 Hr"
                                        else -> "${minutes}m"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AryaaColors.Saffron else Color.Transparent)
                                            .clickable {
                                                isCustom = false
                                                selectedPreset = minutes
                                                durationError = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else AryaaColors.Slate,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            fontFamily = InterFamily
                                        )
                                    }
                                }

                                // Custom option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCustom) AryaaColors.Saffron else Color.Transparent)
                                        .clickable {
                                            isCustom = true
                                            durationError = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Custom",
                                        color = if (isCustom) Color.White else AryaaColors.Slate,
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        fontFamily = InterFamily
                                    )
                                }
                            }

                            // Custom duration input (visible when Custom is selected)
                            if (isCustom) {
                                OutlinedTextField(
                                    value = customDurationText,
                                    onValueChange = { input ->
                                        customDurationText = input.filter { it.isDigit() }
                                        durationError = validateCustomDuration(customDurationText)
                                    },
                                    label = { Text("Minutes (1-1440)", color = AryaaColors.Slate) },
                                    placeholder = { Text("e.g. 15", color = AryaaColors.Slate.copy(alpha = 0.5f)) },
                                    isError = durationError != null,
                                    supportingText = durationError?.let { msg ->
                                        { Text(msg, color = AryaaColors.Crimson) }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AryaaColors.White,
                                        unfocusedTextColor = AryaaColors.White,
                                        focusedBorderColor = AryaaColors.Saffron,
                                        unfocusedBorderColor = AryaaColors.NavyBorder,
                                        focusedContainerColor = AryaaColors.Navy,
                                        unfocusedContainerColor = AryaaColors.Navy,
                                        cursorColor = AryaaColors.Saffron
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Start Button
                        val canStart = activeDuration() != null
                        Button(
                            onClick = {
                                val duration = activeDuration()
                                if (duration != null) viewModel.startCheckIn(duration)
                            },
                            enabled = canStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AryaaColors.Saffron,
                                contentColor = Color.White,
                                disabledContainerColor = AryaaColors.Slate.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Start Check-In Timer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFamily
                            )
                        }
                    }
                }

                is DeadZoneUiState.Pending -> {
                    val pendingState = uiState as DeadZoneUiState.Pending

                    // Format expected return time
                    val formatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault())
                    val expectedTimeStr = try {
                        val instant = Instant.parse(pendingState.expectedBackAt)
                        formatter.format(instant)
                    } catch (_: Exception) {
                        pendingState.expectedBackAt
                    }

                    // Grace deadline is sent by backend — shows the actual proportional
                    // deadline (25% of duration, clamped to 5-30 min) instead of a flat 30 min.
                    val graceTimeStr = try {
                        val instant = Instant.parse(pendingState.gracePeriodEnd)
                        formatter.format(instant)
                    } catch (_: Exception) {
                        pendingState.gracePeriodEnd
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Shield active indicator
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(AryaaColors.Emerald.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AryaaColors.Emerald,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Status Info
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Dead Zone Mode Active",
                                color = AryaaColors.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFamily
                            )
                            Text(
                                text = "You are currently marked as off-grid. Please check in once you have signal.",
                                color = AryaaColors.Slate,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = InterFamily,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // Time display in JetBrains Mono
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AryaaColors.NavyCard),
                            border = BorderStroke(1.dp, AryaaColors.NavyBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "EXPECTED BACK BY",
                                    color = AryaaColors.Slate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFamily
                                )
                                Text(
                                    text = expectedTimeStr,
                                    color = AryaaColors.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace // Monospace font matching JetBrains Mono
                                )
                            }
                        }

                        // Check-in and cancel buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.checkIn() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AryaaColors.Emerald,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "I'm Back — Check In",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFamily
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.cancelSession() },
                                border = BorderStroke(1.dp, AryaaColors.Slate.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AryaaColors.Slate
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Cancel Session Early",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFamily
                                )
                            }
                        }

                        // Danger warning footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AryaaColors.Saffron.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .border(1.dp, AryaaColors.Saffron.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AryaaColors.Saffron,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "If you don't check in by $graceTimeStr, your contacts will be notified with your last known location.",
                                color = AryaaColors.Saffron,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFamily,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                is DeadZoneUiState.CheckingIn -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AryaaColors.Saffron,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Synchronizing with security service...",
                            color = AryaaColors.Slate,
                            fontSize = 14.sp,
                            fontFamily = InterFamily
                        )
                    }
                }

                is DeadZoneUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Connection Error",
                            color = AryaaColors.Crimson,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFamily
                        )
                        Text(
                            text = "Failed to sync check-in status with server. Please check your connection and try again.",
                            color = AryaaColors.Slate,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = InterFamily
                        )
                        Button(
                            onClick = { viewModel.resetToIdle() },
                            colors = ButtonDefaults.buttonColors(containerColor = AryaaColors.Slate),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Try Again", fontFamily = InterFamily)
                        }
                    }
                }
            }
        }
    }
}

private fun validateCustomDuration(input: String): String? {
    if (input.isBlank()) return "Enter a duration"
    val minutes = input.toIntOrNull()
    if (minutes == null) return "Enter a valid number"
    if (minutes < 1) return "Minimum is 1 minute"
    if (minutes > 1440) return "Maximum is 24 hours (1440 minutes)"
    return null
}
