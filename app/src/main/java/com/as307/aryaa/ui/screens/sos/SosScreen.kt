package com.as307.aryaa.ui.screens.sos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import com.as307.aryaa.ui.theme.AryaaMono

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.testTag
import com.as307.aryaa.ui.components.SosButton
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.AryaaColors.dim
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SosScreen(
    api: com.as307.aryaa.data.remote.AryaaApi,
    onNavigateToContacts: () -> Unit,
    viewModel: SosViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val contactCount by viewModel.contactCount.collectAsState()

    // Location permission state tracking
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var dismissLocationWarning by remember { mutableStateOf(false) }
    var showNearbySheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // Request permissions at runtime on the SOS screen, not startup
    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !com.as307.aryaa.util.TestEnv.isUnderTest) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission &&
            !com.as307.aryaa.util.TestEnv.isUnderTest
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Auto-dismiss error banner after 5 seconds
    LaunchedEffect(uiState) {
        if (uiState is SosUiState.Error) {
            delay(5000)
            viewModel.onDismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AryaaColors.Navy)
            .padding(16.dp)
    ) {
        // --- TOP ALERTS / BANNERS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            // 1. Error Banner (Crimson)
            AnimatedVisibility(
                visible = uiState is SosUiState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val errorMsg = (uiState as? SosUiState.Error)?.error?.userMessage ?: ""
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AryaaColors.Crimson, RoundedCornerShape(8.dp))
                        .clickable { viewModel.onDismissError() }
                        .padding(16.dp)
                ) {
                    Text(
                        text = errorMsg,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2. Zero Contacts Alert (Saffron)
            AnimatedVisibility(
                visible = contactCount == 0 && uiState !is SosUiState.Active,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AryaaColors.Amber.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable { onNavigateToContacts() }
                        .padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ No trusted contacts added yet. Tap here to add contacts so they can be notified.",
                        color = AryaaColors.Amber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 4. SMS Permission Denied Alert (Crimson)
            val isSmsPermissionDenied = remember(uiState) {
                context.getSharedPreferences("aryaa_prefs", android.content.Context.MODE_PRIVATE)
                    .getBoolean("sms_permission_denied", false)
            }
            AnimatedVisibility(
                visible = isSmsPermissionDenied && uiState !is SosUiState.Active,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AryaaColors.Crimson.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ SMS alerts may not have been sent — please check SMS permissions in Settings",
                        color = AryaaColors.Crimson,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 5. Notification Permission Warning Banner (Amber)
            AnimatedVisibility(
                visible = !hasNotificationPermission && uiState !is SosUiState.Active,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AryaaColors.Amber.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Notifications disabled — SOS will work but no background alert will show. Tap to enable in Settings.",
                        color = AryaaColors.Amber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. Location Permission Missing Banner (Slate)
            AnimatedVisibility(
                visible = !hasLocationPermission && !dismissLocationWarning && uiState !is SosUiState.Active,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AryaaColors.NavyCard, RoundedCornerShape(8.dp))
                        .clickable {
                            android.util.Log.d("SosScreen", "Location warning clicked!")
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Location unavailable — Tap to enable. SOS will still work, location won't be shared.",
                        color = AryaaColors.Slate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        color = AryaaColors.Saffron,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { dismissLocationWarning = true }
                            .padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // --- MAIN CENTER CONTENT ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
        ) {
            SosButton(
                state = uiState,
                onHoldStart = { viewModel.onHoldStart() },
                onHoldRelease = { viewModel.onHoldRelease() },
                modifier = Modifier.testTag("sos_button")
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Subtext instructions mapping the hold-to-activate states
            when (val state = uiState) {
                is SosUiState.Idle, is SosUiState.Holding -> {
                    Text(
                        text = "Hold 3 seconds to activate",
                        color = AryaaColors.Slate,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal
                    )
                }
                is SosUiState.Countdown -> {
                    Text(
                        text = "Release to cancel",
                        color = AryaaColors.Amber,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium
                    )
                }
                is SosUiState.Triggering -> {
                    Text(
                        text = "Triggering emergency alerts...",
                        color = AryaaColors.Slate,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal
                    )
                }
                is SosUiState.Active -> {
                    val clipboardManager = LocalClipboardManager.current
                    Text(
                        text = "Emergency Alert Active",
                        color = AryaaColors.Crimson,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${state.contacts.size} contacts have been notified.",
                        color = AryaaColors.SlateLight,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    if (!state.w3wAddress.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "///${state.w3wAddress.removePrefix("///")}",
                                style = AryaaMono,
                                color = AryaaColors.Saffron,
                                modifier = Modifier.testTag("w3w_address")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.w3wAddress))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy w3w address",
                                    tint = AryaaColors.Saffron,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    if (state.contacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Notified: " + state.contacts.joinToString { it.name },
                            color = AryaaColors.Slate,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    val accuracy = state.accuracy
                    if (accuracy != null) {
                        val roundedAccuracy = Math.round(accuracy)
                        val accuracyText = if (roundedAccuracy > 1000) {
                            val kmValue = String.format(java.util.Locale.US, "%.1f", accuracy / 1000.0)
                            "Accuracy: ~${kmValue}km"
                        } else {
                            "Accuracy: ~${roundedAccuracy}m"
                        }

                        val color = if (accuracy <= 100.0) {
                            AryaaColors.Slate
                        } else {
                            AryaaColors.Amber
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = accuracyText,
                            color = color,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                is SosUiState.Cancelling -> {
                    Text(
                        text = "Cancelling SOS...",
                        color = AryaaColors.Slate,
                        fontSize = 16.sp
                    )
                }
                is SosUiState.Cancelled -> {
                    Text(
                        text = "SOS Event Cancelled",
                        color = AryaaColors.Emerald,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                is SosUiState.Error -> {
                    Text(
                        text = "Error occurred. Tap button to retry.",
                        color = AryaaColors.Crimson,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // --- BOTTOM ACTION BUTTONS ---
        val currentState = uiState
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentState is SosUiState.Active) {
                val accuracy = currentState.accuracy
                if (accuracy != null && accuracy > 500.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AryaaColors.AmberDim, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ Location may be imprecise (weak signal, possibly indoors). Consider calling first to confirm exact location.",
                            color = AryaaColors.Amber,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (currentState is SosUiState.Active) {
                Button(
                    onClick = { showNearbySheet = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AryaaColors.Blue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "📍 Nearby Help",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showNearbySheet) {
                    val lat = currentState.latitude ?: 18.5204
                    val lng = currentState.longitude ?: 73.8567
                    com.as307.aryaa.ui.screens.nearby.NearbyServicesSheet(
                        latitude = lat,
                        longitude = lng,
                        api = api,
                        onDismiss = { showNearbySheet = false }
                    )
                }
            }

            // Persistent Call 112 Button
            Button(
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    context.startActivity(dialIntent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AryaaColors.Crimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CALL 112",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (currentState is SosUiState.Active) {
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
                                        // Wait for the pointer to be released
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
