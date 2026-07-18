package com.as307.aryaa.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily
import com.as307.aryaa.ui.theme.JetBrainsMonoFamily
import com.as307.aryaa.ui.theme.PlayfairDisplayFamily
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EmergencyResponseScreen(
    onDismiss: () -> Unit,
    viewModel: EmergencyResponseViewModel = hiltViewModel()
) {
    val emergencyOpt by viewModel.activeEmergency.collectAsState()
    val playbookOpt by viewModel.playbookState.collectAsState()
    val isResponding by viewModel.isResponding.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(emergencyOpt) {
        if (emergencyOpt == null) {
            onDismiss()
        }
    }

    // Pulse animation for Crimson alert banner
    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_scale"
    )

    Scaffold(
        containerColor = AryaaColors.Navy,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val emergency = emergencyOpt
            if (emergency == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active emergency alert",
                        color = AryaaColors.White,
                        fontSize = 18.sp,
                        fontFamily = InterFamily
                    )
                }
                return@Scaffold
            }

            // Formatting Trigger Time to IST
            val timeStr = try {
                val instant = Instant.parse(emergency.triggeredAt)
                val zonedDateTime = instant.atZone(ZoneId.of("Asia/Kolkata"))
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                zonedDateTime.format(formatter) + " IST"
            } catch (e: Exception) {
                emergency.triggeredAt
            }

            // TOP SECTION — Alert banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(AryaaColors.Crimson)
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Alert",
                        tint = AryaaColors.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "EMERGENCY ALERT",
                        color = AryaaColors.White,
                        fontSize = 32.sp,
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${emergency.userName} needs help",
                        color = AryaaColors.White,
                        fontSize = 22.sp,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Alert received at $timeStr",
                        color = AryaaColors.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    // Differentiated Visual styling for closest Local Responder
                    if (emergency.tier == "LOCAL_RESPONDER") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AryaaColors.Saffron)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🚨 LOCAL RESPONDER — You are closest nearby!",
                                color = AryaaColors.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playbook Coordination Status: "I am Responding!" Action Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isResponding) {
                    Button(
                        onClick = { viewModel.respondToSos(emergency.sosEventId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AryaaColors.Saffron,
                            contentColor = AryaaColors.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Text(
                            text = "👉 I AM RESPONDING!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFamily
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF10B981)) // Emerald
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AryaaColors.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "✓ YOU ARE RESPONDING (coordinating rescue)",
                                color = AryaaColors.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Playbook Steps
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Text(
                    text = "RESCUER PLAYBOOK",
                    color = AryaaColors.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                val lat = playbookOpt?.latitude ?: emergency.latitude
                val lng = playbookOpt?.longitude ?: emergency.longitude
                val w3w = playbookOpt?.w3wAddress ?: emergency.w3wAddress
                val victimPhone = playbookOpt?.victimPhone ?: emergency.userPhone

                // Step 1: Call Victim
                Card(
                    colors = CardDefaults.cardColors(containerColor = AryaaColors.NavyMid),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "STEP 1: Check On Victim",
                            color = AryaaColors.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Call ${emergency.userName} immediately to confirm the alert context and check if they are safe.",
                            color = AryaaColors.Slate,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$victimPhone"))
                                context.startActivity(dialIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = AryaaColors.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CALL ${emergency.userName.uppercase()}", color = AryaaColors.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Step 2: Contact Local Responders
                Card(
                    colors = CardDefaults.cardColors(containerColor = AryaaColors.NavyMid),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "STEP 2: Coordinate Nearby Responders",
                            color = AryaaColors.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Below are the active responders coordinating to rescue ${emergency.userName}:",
                            color = AryaaColors.Slate,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val responders = playbookOpt?.responders ?: emptyList()
                        if (responders.isEmpty()) {
                            Text(
                                text = "No other responders have checked in yet.",
                                color = AryaaColors.Slate.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                responders.forEach { responder ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AryaaColors.Navy.copy(alpha = 0.4f))
                                            .padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = AryaaColors.Saffron,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(responder.name, color = AryaaColors.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text(responder.phone, color = AryaaColors.Slate, fontSize = 11.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${responder.phone}"))
                                                context.startActivity(dialIntent)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Phone, contentDescription = "Call responder", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 3: Call 112 with Template
                Card(
                    colors = CardDefaults.cardColors(containerColor = AryaaColors.NavyMid),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "STEP 3: Escalate to Police (112)",
                            color = AryaaColors.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Call official emergency dispatch services. Copied coordinates and template copy pre-fills for quick speech copy-paste.",
                            color = AryaaColors.Slate,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val template = "Calling on behalf of ${emergency.userName}, last location " +
                                (if (lat != null && lng != null) "$lat, $lng" else "unknown") +
                                (if (!w3w.isNullOrBlank()) ", what3words: ///$w3w" else "")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AryaaColors.Navy)
                                .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = template,
                                color = AryaaColors.Slate,
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMonoFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(template))
                                Toast.makeText(context, "Speech template copied to clipboard!", Toast.LENGTH_SHORT).show()
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(dialIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AryaaColors.Crimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CALL 112 & COPY SPEECH TEMPLATE", color = AryaaColors.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Step 4: Coordinate with others
                Card(
                    colors = CardDefaults.cardColors(containerColor = AryaaColors.NavyMid),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "STEP 4: Share & Coordinate Rescue",
                            color = AryaaColors.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Share the emergency details with official emergency dispatch or local community groups.",
                            color = AryaaColors.Slate,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val responderNames = (playbookOpt?.responders ?: emptyList()).map { it.name }
                                val responderSummary = if (responderNames.isEmpty()) "None yet" else responderNames.joinToString(", ")
                                val shareText = "🆘 ${emergency.userName} needs help!\n\n" +
                                        (if (lat != null && lng != null) "Location: https://www.google.com/maps?q=$lat,$lng\n" else "") +
                                        (if (!w3w.isNullOrBlank()) "What3Words: ///$w3w\n" else "") +
                                        "Active Responders: $responderSummary\n" +
                                        "Received via ARYAA at $timeStr"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                            },
                            border = BorderStroke(1.dp, AryaaColors.White.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AryaaColors.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = AryaaColors.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SHARE COORDINATION DETAILS", color = AryaaColors.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Maps Button (Floating / Auxiliary)
                val hasLocation = (lat != null && lng != null)
                Button(
                    onClick = {
                        if (lat != null && lng != null) {
                            val labelEncoded = Uri.encode(emergency.userName)
                            val gmmIntentUri = Uri.parse("geo:0,0?q=$lat,$lng($labelEncoded)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                                context.startActivity(webIntent)
                            }
                        }
                    },
                    enabled = hasLocation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(if (hasLocation) "OPEN LIVE MAP DIRECTION" else "LOCATION DIRECTION UNAVAILABLE", fontWeight = FontWeight.Bold)
                }

                // Dismiss Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dismiss",
                        color = AryaaColors.White.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable {
                                viewModel.dismiss()
                                onDismiss()
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}