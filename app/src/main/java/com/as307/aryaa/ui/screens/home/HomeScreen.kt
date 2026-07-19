package com.as307.aryaa.ui.screens.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.as307.aryaa.ui.theme.InterFamily
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.data.repository.SosRepository
import com.as307.aryaa.data.repository.DeadZoneRepository
import com.as307.aryaa.service.SosServiceManager
import com.as307.aryaa.service.ActiveLocationShare
import com.as307.aryaa.ui.theme.AryaaColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    tokenStorage: TokenStorage,
    contactsRepository: ContactsRepository,
    sosRepository: SosRepository,
    deadZoneRepository: DeadZoneRepository,
    fakeCallPreferences: com.as307.aryaa.data.local.FakeCallPreferences,
    sosServiceManager: SosServiceManager,
    onNavigateToContacts: () -> Unit,
    onNavigateToSos: () -> Unit,
    onTriggerFakeCall: (Int) -> Unit,
    onNavigateToPracticeMode: () -> Unit,
    onNavigateToDeadZone: () -> Unit,
    activeEmergency: com.as307.aryaa.ui.screens.emergency.EmergencySosData?,
    onNavigateToEmergencyResponse: () -> Unit,
    activeLocationShare: ActiveLocationShare?,
    onNavigateToLocationShare: () -> Unit,
    incomingLocationShare: com.as307.aryaa.ui.screens.locationshare.IncomingLocationShare?
) {
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("") }
    var contactCount by remember { mutableStateOf(0) }
    var isSosActive by remember { mutableStateOf(false) }
    var isDeadZoneActive by remember { mutableStateOf(false) }
    var deadZoneExpectedBackAt by remember { mutableStateOf<String?>(null) }

    var callerNameInput by remember { mutableStateOf("Maa") }
    var selectedDelay by remember { mutableStateOf(5) } // default 5 seconds

    LaunchedEffect(Unit) {
        scope.launch {
            userName = tokenStorage.getUserName() ?: "there"
        }
        scope.launch {
            contactsRepository.getContacts().first { contacts ->
                contactCount = contacts.size
                true
            }
        }
        scope.launch {
            sosRepository.getSosHistory().onSuccess { history ->
                isSosActive = history.any { it.status == "ACTIVE" }
            }
        }
        scope.launch {
            deadZoneRepository.getStatus().onSuccess { checkIn ->
                isDeadZoneActive = checkIn != null
                deadZoneExpectedBackAt = checkIn?.expectedBackAt
            }
        }
        scope.launch {
            callerNameInput = fakeCallPreferences.getCallerName()
        }
        scope.launch {
            selectedDelay = fakeCallPreferences.getCallerDelay()
        }
        // Observe cancellation events from the notification "I'm Safe" button
        // and clear the home banner immediately, without waiting for a recomposition
        // that would query the API again.
        scope.launch {
            sosServiceManager.sosCancelledEvents.collect {
                isSosActive = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AryaaColors.Navy)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Active SOS Banner (Crimson)
        if (isSosActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AryaaColors.Crimson)
                    .clickable(onClick = onNavigateToSos)
                    .padding(16.dp)
            ) {
                Text(
                    text = "🚨 EMERGENCY SOS ACTIVE — TAP TO MANAGE",
                    color = AryaaColors.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Incoming Emergency Alert Banner (Saffron/Orange)
        activeEmergency?.let { emergency ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AryaaColors.Saffron)
                    .clickable(onClick = onNavigateToEmergencyResponse)
                    .padding(16.dp)
            ) {
                Text(
                    text = "🚨 EMERGENCY ALERT: ${emergency.userName} needs help! Tap to view location & playbook.",
                    color = AryaaColors.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Active Location Sharing Banner (Royal Blue)
        activeLocationShare?.let { share ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AryaaColors.Blue)
                    .clickable(onClick = onNavigateToLocationShare)
                    .padding(16.dp)
            ) {
                Text(
                    text = "📍 Sharing location with ${share.contactCount} contact${if (share.contactCount != 1) "s" else ""} — tap to manage",
                    color = AryaaColors.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Incoming Location Sharing Banner (Royal Blue)
        incomingLocationShare?.let { incoming ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AryaaColors.Blue.copy(alpha = 0.85f))
                    .clickable(onClick = onNavigateToLocationShare)
                    .padding(16.dp)
            ) {
                Text(
                    text = "📍 ${incoming.sharerName} is sharing their live location — tap to view map",
                    color = AryaaColors.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Greeting header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = AryaaColors.Saffron,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ARYAA",
                color = AryaaColors.Saffron,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 2.sp
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Hi, ${if (userName.isBlank()) "there" else userName.split(" ").first()}",
                color = AryaaColors.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Text(
                text = "You're protected.",
                color = AryaaColors.Slate,
                fontSize = 16.sp
            )
        }

        // Trusted contacts summary card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AryaaColors.NavyCard)
                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onNavigateToContacts)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AryaaColors.Emerald.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.People,
                        contentDescription = null,
                        tint = AryaaColors.Emerald,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trusted Contacts",
                        color = AryaaColors.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (contactCount == 0) "Add contacts to get started"
                               else "$contactCount contact${if (contactCount > 1) "s" else ""} active",
                        color = if (contactCount == 0) AryaaColors.Amber else AryaaColors.Emerald,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AryaaColors.Slate,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Active SOS Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AryaaColors.NavyCard)
                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onNavigateToSos)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AryaaColors.Crimson.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = AryaaColors.Crimson,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Emergency SOS",
                        color = AryaaColors.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Trigger or manage active alerts",
                        color = AryaaColors.Slate,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AryaaColors.Slate,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Fake Call Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AryaaColors.NavyCard)
                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AryaaColors.Saffron.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = null,
                            tint = AryaaColors.Saffron,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fake Call",
                            color = AryaaColors.White,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Schedule a fake incoming call to safely exit a situation",
                            color = AryaaColors.Slate,
                            fontFamily = InterFamily,
                            fontSize = 13.sp
                        )
                    }
                }

                // Caller Name Input Row
                OutlinedTextField(
                    value = callerNameInput,
                    onValueChange = { callerNameInput = it },
                    label = { Text("Caller Name", color = AryaaColors.Slate) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AryaaColors.White,
                        unfocusedTextColor = AryaaColors.White,
                        focusedBorderColor = AryaaColors.Saffron,
                        unfocusedBorderColor = AryaaColors.NavyBorder,
                        focusedContainerColor = AryaaColors.Navy,
                        unfocusedContainerColor = AryaaColors.Navy
                    ),
                    singleLine = true
                )

                // Delay Selector Buttons Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Call delay",
                        color = AryaaColors.Slate,
                        fontFamily = InterFamily,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val delayOptions = listOf(
                            Pair("Now", 0),
                            Pair("5s", 5),
                            Pair("10s", 10),
                            Pair("30s", 30)
                        )
                        delayOptions.forEach { (label, value) ->
                            val isSelected = selectedDelay == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AryaaColors.Saffron else AryaaColors.Navy)
                                    .border(
                                        1.dp,
                                        if (isSelected) AryaaColors.Saffron else AryaaColors.NavyBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedDelay = value }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AryaaColors.White else AryaaColors.Slate,
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Action Call me Button
                Button(
                    onClick = {
                        scope.launch {
                            fakeCallPreferences.setCallerName(callerNameInput)
                            fakeCallPreferences.setCallerDelay(selectedDelay)
                            onTriggerFakeCall(selectedDelay)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AryaaColors.Saffron,
                        contentColor = AryaaColors.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Call me",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Diagnose Location button
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        try {
                            val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                            client.lastLocation.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val loc = task.result
                                    val isMock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        loc?.isMock == true
                                    } else {
                                        @Suppress("DEPRECATION")
                                        loc?.isFromMockProvider == true
                                    }
                                    android.util.Log.d("LOCATION_DEBUG", "Direct query raw: " + 
                                        loc?.latitude + ", " + loc?.longitude + 
                                        ", provider=" + loc?.provider + 
                                        ", time=" + loc?.time + 
                                        ", isFromMockProvider=" + isMock)
                                    android.widget.Toast.makeText(context, "Direct Loc: ${loc?.latitude}, ${loc?.longitude}, isMock=$isMock", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.util.Log.d("LOCATION_DEBUG", "Direct query task failed")
                                    android.widget.Toast.makeText(context, "Direct query task failed", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: SecurityException) {
                            android.util.Log.e("LOCATION_DEBUG", "SecurityException direct query", e)
                            android.widget.Toast.makeText(context, "Location permission missing", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AryaaColors.Slate,
                        contentColor = AryaaColors.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Diagnose Location",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Practice Mode Card
        val cardAlpha = if (isSosActive) 0.5f else 1.0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AryaaColors.NavyCard.copy(alpha = cardAlpha))
                .border(1.dp, AryaaColors.NavyBorder.copy(alpha = cardAlpha), RoundedCornerShape(20.dp))
                .clickable(enabled = !isSosActive) { onNavigateToPracticeMode() }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = if (isSosActive) 0.06f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6).copy(alpha = cardAlpha),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Practice Mode",
                        color = AryaaColors.White.copy(alpha = cardAlpha),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSosActive) {
                            "Practice unavailable — you have an active emergency alert"
                        } else {
                            "Rehearse your SOS flow safely — nothing real is sent"
                        },
                        color = if (isSosActive) AryaaColors.Crimson else AryaaColors.Slate,
                        fontFamily = InterFamily,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AryaaColors.Slate.copy(alpha = cardAlpha),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Dead Zone Check-In Card
        val deadZoneTimeStr = if (deadZoneExpectedBackAt != null) {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a").withZone(java.time.ZoneId.systemDefault())
                formatter.format(java.time.Instant.parse(deadZoneExpectedBackAt))
            } catch (_: Exception) {
                deadZoneExpectedBackAt
            }
        } else null

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AryaaColors.NavyCard)
                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(20.dp))
                .clickable { onNavigateToDeadZone() }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Timer, // Timer icon matching practice-info style
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dead Zone Check-In",
                            color = AryaaColors.White,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (isDeadZoneActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            // Small pending indicator dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(AryaaColors.Emerald)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDeadZoneActive && deadZoneTimeStr != null) {
                            "Active — expected back by $deadZoneTimeStr"
                        } else {
                            "Going off-grid? Set a check-in timer"
                        },
                        color = if (isDeadZoneActive) AryaaColors.Emerald else AryaaColors.Slate,
                        fontFamily = InterFamily,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AryaaColors.Slate,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Location Sharing Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AryaaColors.NavyCard)
                .border(
                    1.dp,
                    if (activeLocationShare != null) AryaaColors.Blue else AryaaColors.NavyBorder,
                    RoundedCornerShape(20.dp)
                )
                .clickable { onNavigateToLocationShare() }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AryaaColors.Blue.copy(alpha = if (activeLocationShare != null) 0.20f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📍",
                        fontSize = 22.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Location Sharing",
                            color = AryaaColors.White,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (activeLocationShare != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(AryaaColors.Blue)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (activeLocationShare != null) {
                            "Active — sharing with ${activeLocationShare.contactCount} contact${if (activeLocationShare.contactCount != 1) "s" else ""}"
                        } else {
                            "Share your live location with a contact"
                        },
                        color = if (activeLocationShare != null) AryaaColors.Blue else AryaaColors.Slate,
                        fontFamily = InterFamily,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = if (activeLocationShare != null) AryaaColors.Blue else AryaaColors.Slate,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
