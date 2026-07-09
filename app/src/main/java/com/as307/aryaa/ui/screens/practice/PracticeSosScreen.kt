package com.as307.aryaa.ui.screens.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.as307.aryaa.ui.components.SosButton
import com.as307.aryaa.ui.screens.sos.SosUiState
import com.as307.aryaa.ui.theme.AryaaColors
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSosScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (holdDuration: Int, contactsCount: Int, accuracy: Int, duress: Boolean) -> Unit,
    viewModel: PracticeSosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contactCount by viewModel.contactCount.collectAsState()
    val simulatedContacts by viewModel.simulatedContacts.collectAsState()
    val userName by viewModel.userName.collectAsState()

    // Observe navigation events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            if (event is PracticeSosViewModel.PracticeNavigationEvent.NavigateToSummary) {
                onNavigateToSummary(
                    event.holdDuration,
                    event.contactsCount,
                    event.accuracy,
                    event.duressPracticed
                )
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Persistent safety banner (Step 5)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3B82F6))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎓 PRACTICE MODE — No real alerts will be sent",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )
                }

                // 2. Navigation Top Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "SOS Practice Session",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AryaaColors.White
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
            }
        },
        containerColor = AryaaColors.Navy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Warning if zero contacts (same as real SOS screen)
                AnimatedVisibility(
                    visible = contactCount == 0 && uiState is SosUiState.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AryaaColors.Saffron.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, AryaaColors.Saffron, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ You have no trusted contacts added. Real alerts would fail to dispatch. Please add contacts in the Contacts tab.",
                            color = AryaaColors.Saffron,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // State descriptive subtitle
                Text(
                    text = when (uiState) {
                        is SosUiState.Idle -> "Hold the button for 3 seconds to practice triggering SOS."
                        is SosUiState.Holding, is SosUiState.Countdown -> "Keep holding..."
                        is SosUiState.Triggering -> "Triggering simulated SOS..."
                        is SosUiState.Active -> "Simulated SOS is active."
                        else -> "Initializing..."
                    },
                    color = AryaaColors.Slate,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // The main interactive SOS button (Step 8)
                SosButton(
                    state = uiState,
                    onHoldStart = { viewModel.onHoldStart() },
                    onHoldRelease = { viewModel.onHoldRelease() },
                    isPracticeMode = true
                )

                // Simulated active details and checklist ("What would happen" panel)
                if (uiState is SosUiState.Active) {
                    val activeState = uiState as SosUiState.Active

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // 1. Simulated contacts checklist
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AryaaColors.NavyCard),
                                border = BorderStroke(1.dp, AryaaColors.NavyBorder),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AryaaColors.Emerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "📱 Emergency Alerts Dispatch List",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AryaaColors.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (simulatedContacts.isEmpty()) {
                                        Text(
                                            text = "No contacts configured to receive alerts.",
                                            color = AryaaColors.Slate,
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        simulatedContacts.forEach { contact ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = AryaaColors.Emerald,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = contact.name,
                                                    fontSize = 13.sp,
                                                    color = AryaaColors.White,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = if (contact.hasFcmToken) "via push notification" else "via SMS",
                                                    fontSize = 12.sp,
                                                    color = if (contact.hasFcmToken) AryaaColors.Emerald else AryaaColors.Saffron,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Simulated Location Details
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AryaaColors.NavyCard),
                                border = BorderStroke(1.dp, AryaaColors.NavyBorder),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = AryaaColors.Emerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "📍 Shared Emergency Location",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AryaaColors.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Coordinates: ${activeState.w3wAddress ?: "12.9716, 77.5946"}",
                                        fontSize = 13.sp,
                                        color = AryaaColors.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Accuracy: accurate to ~${(activeState.accuracy ?: 10.0).toInt()}m",
                                        fontSize = 12.sp,
                                        color = AryaaColors.Slate
                                    )
                                }
                            }
                        }

                        // 3. Notification Preview Card
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "🔔 Recipient Notification Preview",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AryaaColors.White,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AryaaColors.Crimson.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = AryaaColors.Crimson,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "🆘 EMERGENCY SOS — ARYAA",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = AryaaColors.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$userName needs help! Tap to view coordinates, reverse geocoded address, and safety details.",
                                                fontSize = 12.sp,
                                                color = AryaaColors.Slate
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Calm Cancel SOS Button with Stealth Hold Gesture (Step 2/Step 8)
            if (uiState is SosUiState.Active) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                color = AryaaColors.Emerald.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val pointerId = down.id

                                        val upOrCancel = withTimeoutOrNull(3000L) {
                                            var isDown = true
                                            while (isDown) {
                                                val event = awaitPointerEvent()
                                                val anyPressed = event.changes.any { it.pressed && it.id == pointerId }
                                                if (!anyPressed) {
                                                    isDown = false
                                                }
                                            }
                                            true
                                        }

                                        if (upOrCancel == null) {
                                            // Held past 3 seconds -> duress cancel
                                            viewModel.onDuressCancel()
                                            // Wait for pointer release
                                            var isDown = true
                                            while (isDown) {
                                                val event = awaitPointerEvent()
                                                val anyPressed = event.changes.any { it.pressed && it.id == pointerId }
                                                if (!anyPressed) {
                                                    isDown = false
                                                }
                                            }
                                        } else {
                                            // Released before 3 seconds -> normal cancel
                                            viewModel.onCancelSos()
                                        }
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = "I'm Safe — Cancel SOS",
                            color = AryaaColors.Emerald,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
