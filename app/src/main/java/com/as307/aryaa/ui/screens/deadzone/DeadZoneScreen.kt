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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
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

    // Mode Selector Tab State
    var selectedMode by remember { mutableStateOf("PLAIN") } // "PLAIN", "SAFE_WALK", "HEARTBEAT"

    // Form inputs for Plain Dead Zone
    val plainPresets = listOf(60, 120, 240, 480)
    var selectedPlainPreset by remember { mutableStateOf(60) }
    var isPlainCustom by remember { mutableStateOf(false) }
    var customPlainDurationText by remember { mutableStateOf("") }
    var plainDurationError by remember { mutableStateOf<String?>(null) }

    // Form inputs for Safe Walk
    var destinationText by remember { mutableStateOf("") }
    val walkPresets = listOf(15, 30, 45, 60)
    var selectedWalkPreset by remember { mutableStateOf(30) }
    var isWalkCustom by remember { mutableStateOf(false) }
    var customWalkDurationText by remember { mutableStateOf("") }
    var walkDurationError by remember { mutableStateOf<String?>(null) }

    // Form inputs for Heartbeat Monitoring
    val heartbeatPresets = listOf(15, 30, 60, 120)
    var selectedHeartbeatPreset by remember { mutableStateOf(30) }
    var isHeartbeatCustom by remember { mutableStateOf(false) }
    var customHeartbeatDurationText by remember { mutableStateOf("") }
    var heartbeatDurationError by remember { mutableStateOf<String?>(null) }

    // Helper functions to get current input values
    fun getPlainDuration(): Int? {
        if (isPlainCustom) {
            val parsed = customPlainDurationText.toIntOrNull()
            if (parsed == null || parsed < 1 || parsed > 1440) return null
            return parsed
        }
        return selectedPlainPreset
    }

    fun getWalkDuration(): Int? {
        if (isWalkCustom) {
            val parsed = customWalkDurationText.toIntOrNull()
            if (parsed == null || parsed < 1 || parsed > 1440) return null
            return parsed
        }
        return selectedWalkPreset
    }

    fun getHeartbeatInterval(): Int? {
        if (isHeartbeatCustom) {
            val parsed = customHeartbeatDurationText.toIntOrNull()
            if (parsed == null || parsed < 1 || parsed > 1440) return null
            return parsed
        }
        return selectedHeartbeatPreset
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Safety Check-In",
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
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Custom Mode Selection Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(AryaaColors.NavyCard, RoundedCornerShape(12.dp))
                                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("PLAIN" to "Dead Zone", "SAFE_WALK" to "Safe Walk", "HEARTBEAT" to "Heartbeat").forEach { (mode, label) ->
                                val isSelected = selectedMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AryaaColors.Saffron else Color.Transparent)
                                        .clickable { selectedMode = mode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else AryaaColors.Slate,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        fontFamily = InterFamily
                                    )
                                }
                            }
                        }

                        // Contextual Form Layout
                        when (selectedMode) {
                            "PLAIN" -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Going Offline?",
                                            color = AryaaColors.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFamily
                                        )
                                        Text(
                                            text = "Set a timer before entering a no-signal area. If you don't check back in, your contacts will be automatically alerted.",
                                            color = AryaaColors.Slate,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = InterFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }

                                    // Duration Presets
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Select Duration",
                                            color = AryaaColors.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterFamily
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .background(AryaaColors.NavyCard, RoundedCornerShape(10.dp))
                                                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(10.dp))
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            plainPresets.forEach { minutes ->
                                                val isSelected = !isPlainCustom && selectedPlainPreset == minutes
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
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSelected) AryaaColors.Saffron else Color.Transparent)
                                                        .clickable {
                                                            isPlainCustom = false
                                                            selectedPlainPreset = minutes
                                                            plainDurationError = null
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) Color.White else AryaaColors.Slate,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp,
                                                        fontFamily = InterFamily
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isPlainCustom) AryaaColors.Saffron else Color.Transparent)
                                                    .clickable {
                                                        isPlainCustom = true
                                                        plainDurationError = null
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Custom",
                                                    color = if (isPlainCustom) Color.White else AryaaColors.Slate,
                                                    fontWeight = if (isPlainCustom) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    fontFamily = InterFamily
                                                )
                                            }
                                        }

                                        if (isPlainCustom) {
                                            OutlinedTextField(
                                                value = customPlainDurationText,
                                                onValueChange = { input ->
                                                    customPlainDurationText = input.filter { it.isDigit() }
                                                    plainDurationError = validateCustomDuration(customPlainDurationText)
                                                },
                                                label = { Text("Minutes (1-1440)", color = AryaaColors.Slate) },
                                                isError = plainDurationError != null,
                                                supportingText = plainDurationError?.let { msg -> { Text(msg, color = AryaaColors.Crimson) } },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = AryaaColors.White,
                                                    unfocusedTextColor = AryaaColors.White,
                                                    focusedBorderColor = AryaaColors.Saffron,
                                                    unfocusedBorderColor = AryaaColors.NavyBorder,
                                                    cursorColor = AryaaColors.Saffron
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            "SAFE_WALK" -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(AryaaColors.Emerald.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsWalk,
                                            contentDescription = null,
                                            tint = AryaaColors.Emerald,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Walk Guard",
                                            color = AryaaColors.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFamily
                                        )
                                        Text(
                                            text = "Automatically starts live location sharing with your contacts and starts a safety countdown timer.",
                                            color = AryaaColors.Slate,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = InterFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }

                                    // Destination Input
                                    OutlinedTextField(
                                        value = destinationText,
                                        onValueChange = { destinationText = it },
                                        label = { Text("Destination Address", color = AryaaColors.Slate) },
                                        placeholder = { Text("e.g. Home, Union Station", color = AryaaColors.Slate.copy(alpha = 0.5f)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = AryaaColors.White,
                                            unfocusedTextColor = AryaaColors.White,
                                            focusedBorderColor = AryaaColors.Saffron,
                                            unfocusedBorderColor = AryaaColors.NavyBorder,
                                            cursorColor = AryaaColors.Saffron
                                        )
                                    )

                                    // Duration Presets
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Select Estimated Duration",
                                            color = AryaaColors.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterFamily
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .background(AryaaColors.NavyCard, RoundedCornerShape(10.dp))
                                                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(10.dp))
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            walkPresets.forEach { minutes ->
                                                val isSelected = !isWalkCustom && selectedWalkPreset == minutes
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSelected) AryaaColors.Saffron else Color.Transparent)
                                                        .clickable {
                                                            isWalkCustom = false
                                                            selectedWalkPreset = minutes
                                                            walkDurationError = null
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${minutes}m",
                                                        color = if (isSelected) Color.White else AryaaColors.Slate,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp,
                                                        fontFamily = InterFamily
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isWalkCustom) AryaaColors.Saffron else Color.Transparent)
                                                    .clickable {
                                                        isWalkCustom = true
                                                        walkDurationError = null
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Custom",
                                                    color = if (isWalkCustom) Color.White else AryaaColors.Slate,
                                                    fontWeight = if (isWalkCustom) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    fontFamily = InterFamily
                                                )
                                            }
                                        }

                                        if (isWalkCustom) {
                                            OutlinedTextField(
                                                value = customWalkDurationText,
                                                onValueChange = { input ->
                                                    customWalkDurationText = input.filter { it.isDigit() }
                                                    walkDurationError = validateCustomDuration(customWalkDurationText)
                                                },
                                                label = { Text("Minutes (1-1440)", color = AryaaColors.Slate) },
                                                isError = walkDurationError != null,
                                                supportingText = walkDurationError?.let { msg -> { Text(msg, color = AryaaColors.Crimson) } },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = AryaaColors.White,
                                                    unfocusedTextColor = AryaaColors.White,
                                                    focusedBorderColor = AryaaColors.Saffron,
                                                    unfocusedBorderColor = AryaaColors.NavyBorder,
                                                    cursorColor = AryaaColors.Saffron
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            "HEARTBEAT" -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(AryaaColors.Saffron.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = AryaaColors.Saffron,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Heartbeat Monitoring",
                                            color = AryaaColors.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFamily
                                        )
                                        Text(
                                            text = "Send recurring check-in pings at set intervals. If you miss a ping within 5 minutes, your emergency contacts will be alerted.",
                                            color = AryaaColors.Slate,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = InterFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }

                                    // Interval Presets
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Safety Ping Interval",
                                            color = AryaaColors.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterFamily
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .background(AryaaColors.NavyCard, RoundedCornerShape(10.dp))
                                                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(10.dp))
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            heartbeatPresets.forEach { minutes ->
                                                val isSelected = !isHeartbeatCustom && selectedHeartbeatPreset == minutes
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSelected) AryaaColors.Saffron else Color.Transparent)
                                                        .clickable {
                                                            isHeartbeatCustom = false
                                                            selectedHeartbeatPreset = minutes
                                                            heartbeatDurationError = null
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${minutes}m",
                                                        color = if (isSelected) Color.White else AryaaColors.Slate,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp,
                                                        fontFamily = InterFamily
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isHeartbeatCustom) AryaaColors.Saffron else Color.Transparent)
                                                    .clickable {
                                                        isHeartbeatCustom = true
                                                        heartbeatDurationError = null
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Custom",
                                                    color = if (isHeartbeatCustom) Color.White else AryaaColors.Slate,
                                                    fontWeight = if (isHeartbeatCustom) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    fontFamily = InterFamily
                                                )
                                            }
                                        }

                                        if (isHeartbeatCustom) {
                                            OutlinedTextField(
                                                value = customHeartbeatDurationText,
                                                onValueChange = { input ->
                                                    customHeartbeatDurationText = input.filter { it.isDigit() }
                                                    heartbeatDurationError = validateCustomDuration(customHeartbeatDurationText)
                                                },
                                                label = { Text("Minutes (1-1440)", color = AryaaColors.Slate) },
                                                isError = heartbeatDurationError != null,
                                                supportingText = heartbeatDurationError?.let { msg -> { Text(msg, color = AryaaColors.Crimson) } },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = AryaaColors.White,
                                                    unfocusedTextColor = AryaaColors.White,
                                                    focusedBorderColor = AryaaColors.Saffron,
                                                    unfocusedBorderColor = AryaaColors.NavyBorder,
                                                    cursorColor = AryaaColors.Saffron
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                when (selectedMode) {
                                    "PLAIN" -> {
                                        getPlainDuration()?.let {
                                            viewModel.startCheckIn(durationMinutes = it, mode = "PLAIN")
                                        }
                                    }
                                    "SAFE_WALK" -> {
                                        getWalkDuration()?.let {
                                            val dest = destinationText.takeIf { it.isNotBlank() } ?: "Safe Destination"
                                            viewModel.startCheckIn(durationMinutes = it, mode = "SAFE_WALK", destination = dest)
                                        }
                                    }
                                    "HEARTBEAT" -> {
                                        getHeartbeatInterval()?.let {
                                            viewModel.startCheckIn(durationMinutes = it, mode = "HEARTBEAT", intervalMinutes = it)
                                        }
                                    }
                                }
                            },
                            enabled = when (selectedMode) {
                                "PLAIN" -> getPlainDuration() != null
                                "SAFE_WALK" -> getWalkDuration() != null
                                "HEARTBEAT" -> getHeartbeatInterval() != null
                                else -> false
                            },
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
                                text = when (selectedMode) {
                                    "SAFE_WALK" -> "Start Safe Walk Guard"
                                    "HEARTBEAT" -> "Activate Heartbeat Loop"
                                    else -> "Start Dead Zone Timer"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFamily
                            )
                        }
                    }
                }

                is DeadZoneUiState.Pending -> {
                    val pendingState = uiState as DeadZoneUiState.Pending
                    val mode = pendingState.mode

                    val formatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault())
                    val expectedTimeStr = try {
                        val instant = Instant.parse(pendingState.expectedBackAt)
                        formatter.format(instant)
                    } catch (_: Exception) {
                        pendingState.expectedBackAt
                    }

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
                        // Contextual Indicator Icon
                        val indicatorBg: Color
                        val indicatorTint: Color
                        val indicatorIcon: androidx.compose.ui.graphics.vector.ImageVector
                        val statusTitle: String
                        val statusContent: String

                        when (mode) {
                            "SAFE_WALK" -> {
                                indicatorBg = AryaaColors.Emerald.copy(alpha = 0.12f)
                                indicatorTint = AryaaColors.Emerald
                                indicatorIcon = Icons.Default.DirectionsWalk
                                statusTitle = "Safe Walk Guard Active"
                                statusContent = if (!pendingState.destination.isNullOrBlank()) {
                                    "Sharing live location updates while walking to ${pendingState.destination}."
                                } else {
                                    "Sharing live location updates while walking to destination."
                                }
                            }
                            "HEARTBEAT" -> {
                                indicatorBg = AryaaColors.Saffron.copy(alpha = 0.12f)
                                indicatorTint = AryaaColors.Saffron
                                indicatorIcon = Icons.Default.Favorite
                                statusTitle = "Heartbeat Monitoring Active"
                                statusContent = "Your safety ping loop is active. Keep this device online to confirm safety pings."
                            }
                            else -> {
                                indicatorBg = AryaaColors.Emerald.copy(alpha = 0.12f)
                                indicatorTint = AryaaColors.Emerald
                                indicatorIcon = Icons.Default.Security
                                statusTitle = "Dead Zone Guard Active"
                                statusContent = "You are currently marked as off-grid. Please check in once you have signal."
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(indicatorBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = indicatorIcon,
                                contentDescription = null,
                                tint = indicatorTint,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Info Text block
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = statusTitle,
                                color = AryaaColors.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFamily
                            )
                            Text(
                                text = statusContent,
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
                                    text = if (mode == "HEARTBEAT") "NEXT CHECK-IN BY" else "EXPECTED BACK BY",
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
                                    fontFamily = FontFamily.Monospace
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
                                    text = when (mode) {
                                        "SAFE_WALK" -> "I've Arrived — Stop Sharing"
                                        "HEARTBEAT" -> "Confirm I'm Safe"
                                        else -> "I'm Back — Check In"
                                    },
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
                                text = if (mode == "HEARTBEAT") {
                                    "If you do not confirm safety by $graceTimeStr, an SOS will be automatically triggered to notify your contacts."
                                } else {
                                    "If you don't check in by $graceTimeStr, your contacts will be notified with your last known location."
                                },
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
                            text = "Synchronizing with safety services...",
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
