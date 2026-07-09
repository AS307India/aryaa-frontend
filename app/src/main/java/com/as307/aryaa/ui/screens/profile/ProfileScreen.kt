package com.as307.aryaa.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.as307.aryaa.BuildConfig
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily
import com.as307.aryaa.ui.theme.PlayfairDisplayFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToContacts: () -> Unit,
    onNavigateToMedicalId: () -> Unit,
    onSignOutComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val contactCount by viewModel.contactCount.collectAsState()
    val sosHoldDuration by viewModel.sosHoldDuration.collectAsState()
    val volumeButtonTrigger by viewModel.volumeButtonTrigger.collectAsState()
    val offlineSmsAlerts by viewModel.offlineSmsAlerts.collectAsState()

    var showSignOutDialog by remember { mutableStateOf(false) }

    // Check SMS permission status
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Refresh permission check on resume
    LaunchedEffect(Unit) {
        hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ProfileNavigationEvent.NavigateToLogin -> onSignOutComplete()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AryaaColors.Navy
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = AryaaColors.Saffron,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROFILE",
                    color = AryaaColors.Saffron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp
                )
            }

            // SECTION 1: Account
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AryaaColors.NavyCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "A",
                        color = AryaaColors.Saffron,
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }

                Text(
                    text = userName,
                    color = AryaaColors.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = userEmail,
                    color = AryaaColors.Slate,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                if (userPhone.isNotBlank()) {
                    Text(
                        text = userPhone,
                        color = AryaaColors.Slate,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "Edit Profile",
                    color = AryaaColors.Saffron,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Profile editing coming soon")
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 12.dp)
                )
            }

            Divider(color = AryaaColors.NavyBorder, thickness = 1.dp)

            // SECTION 2: Safety settings
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Safety Settings",
                    color = AryaaColors.Slate,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )

                // Trusted Contacts Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToContacts)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        tint = AryaaColors.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Trusted Contacts",
                        color = AryaaColors.White,
                        fontFamily = InterFamily,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AryaaColors.NavyCard)
                            .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$contactCount",
                            color = AryaaColors.Saffron,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = AryaaColors.Slate,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // SOS Hold Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOS Hold Duration",
                        color = AryaaColors.White,
                        fontFamily = InterFamily,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 2, 3).forEach { sec ->
                            val isSelected = sosHoldDuration == sec
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AryaaColors.Saffron else AryaaColors.NavyCard)
                                    .border(
                                        1.dp,
                                        if (isSelected) AryaaColors.Saffron else AryaaColors.NavyBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.updateSosHoldDuration(sec) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    color = if (isSelected) AryaaColors.White else AryaaColors.Slate,
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Volume Button Trigger Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Volume Button Trigger",
                            color = AryaaColors.White,
                            fontFamily = InterFamily,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Intercept volume down to trigger SOS",
                            color = AryaaColors.Slate,
                            fontFamily = InterFamily,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = volumeButtonTrigger,
                        onCheckedChange = { viewModel.updateVolumeButtonTrigger(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AryaaColors.White,
                            checkedTrackColor = AryaaColors.Saffron,
                            uncheckedThumbColor = AryaaColors.Slate,
                            uncheckedTrackColor = AryaaColors.NavyCard
                        )
                    )
                }

                // Offline SMS Alerts Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline SMS Alerts",
                            color = AryaaColors.White,
                            fontFamily = InterFamily,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Dispatch emergency SMS alerts to contacts",
                            color = AryaaColors.Slate,
                            fontFamily = InterFamily,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = offlineSmsAlerts,
                        onCheckedChange = { viewModel.updateOfflineSmsAlerts(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AryaaColors.White,
                            checkedTrackColor = AryaaColors.Saffron,
                            uncheckedThumbColor = AryaaColors.Slate,
                            uncheckedTrackColor = AryaaColors.NavyCard
                        )
                    )
                }

                // SMS Permission Status (Read-only row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SMS Permission",
                        color = AryaaColors.White,
                        fontFamily = InterFamily,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (hasSmsPermission) "Granted" else "Not granted — tap to enable",
                        color = if (hasSmsPermission) AryaaColors.Emerald else AryaaColors.Amber,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            Divider(color = AryaaColors.NavyBorder, thickness = 1.dp)

            // SECTION 3: App
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "App Settings",
                    color = AryaaColors.Slate,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )

                // Medical ID Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToMedicalId)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Medical ID",
                        color = AryaaColors.Saffron,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = AryaaColors.Saffron,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // About ARYAA Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "About ARYAA",
                        color = AryaaColors.White,
                        fontFamily = InterFamily,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = AryaaColors.Slate,
                        fontFamily = InterFamily,
                        fontSize = 14.sp
                    )
                }

                // Privacy Policy Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val customTabsIntent = CustomTabsIntent
                                .Builder()
                                .build()
                            customTabsIntent.launchUrl(context, Uri.parse("https://aryaa.app/privacy"))
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Privacy Policy",
                        color = AryaaColors.White,
                        fontFamily = InterFamily,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.Launch,
                        contentDescription = "External link",
                        tint = AryaaColors.Slate,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Sign Out Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSignOutDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sign Out",
                        color = AryaaColors.Crimson,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = AryaaColors.NavyCard,
            title = {
                Text(
                    text = "Sign Out",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sign out of ARYAA? Your trusted contacts and Medical ID will remain saved.",
                    fontFamily = InterFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    }
                ) {
                    Text(
                        text = "Sign Out",
                        color = AryaaColors.Crimson,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = AryaaColors.Slate,
                        fontFamily = InterFamily
                    )
                }
            }
        )
    }
}
