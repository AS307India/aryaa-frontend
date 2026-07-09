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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

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
                // Empty state fallback (should not normally occur unless dismissed)
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
                    Spacer(modifier = Modifier.height(16.dp))
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
                }
            }

            // MIDDLE SECTION — Location card
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AryaaColors.NavyMid
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "LOCATION",
                        color = AryaaColors.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val lat = emergency.latitude
                    val lng = emergency.longitude
                    val w3w = emergency.w3wAddress

                    if (lat != null && lng != null) {
                        if (!w3w.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString("///$w3w"))
                                        Toast.makeText(context, "Copied address ///$w3w", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Text(
                                    text = "///$w3w",
                                    color = AryaaColors.Saffron,
                                    fontSize = 24.sp,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = AryaaColors.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString("$lat, $lng"))
                                    Toast.makeText(context, "Copied coordinates $lat, $lng", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Lat: $lat",
                                    color = AryaaColors.White,
                                    fontSize = 16.sp,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                Text(
                                    text = "Lng: $lng",
                                    color = AryaaColors.White,
                                    fontSize = 16.sp,
                                    fontFamily = JetBrainsMonoFamily
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                tint = AryaaColors.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        val accuracy = emergency.accuracy
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
                                fontFamily = InterFamily
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "Location unavailable",
                                color = AryaaColors.Saffron,
                                fontSize = 18.sp,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try calling to find their location",
                                color = AryaaColors.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontFamily = InterFamily
                            )
                        }
                    }
                }
            }

            // BOTTOM SECTION — Action buttons (stacked, full-width)
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val accuracy = emergency.accuracy
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
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val hasLocation = (emergency.latitude != null && emergency.longitude != null)

                // 1. PRIMARY ACTION — "OPEN IN GOOGLE MAPS"
                Button(
                    onClick = {
                        val latVal = emergency.latitude
                        val lngVal = emergency.longitude
                        if (latVal != null && lngVal != null) {
                            val labelEncoded = Uri.encode(emergency.userName)
                            val gmmIntentUri = Uri.parse("geo:0,0?q=$latVal,$lngVal($labelEncoded)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latVal,$lngVal"))
                                context.startActivity(webIntent)
                            }
                        }
                    },
                    enabled = hasLocation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981), // Emerald green
                        disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.4f),
                        contentColor = AryaaColors.White,
                        disabledContentColor = AryaaColors.White.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (hasLocation) "OPEN IN GOOGLE MAPS" else "LOCATION UNAVAILABLE",
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // 2. SECONDARY ACTION — "CALL [USER NAME]"
                Button(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${emergency.userPhone}")
                        }
                        context.startActivity(dialIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B1A), // Saffron
                        contentColor = AryaaColors.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = "Call",
                        tint = AryaaColors.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CALL ${emergency.userName.uppercase()}",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // 3. TERTIARY ACTION — "SHARE LOCATION"
                OutlinedButton(
                    onClick = {
                        val latVal = emergency.latitude
                        val lngVal = emergency.longitude
                        val shareText = "🆘 ${emergency.userName} needs help!\n\n" +
                                (if (latVal != null && lngVal != null) "Location: https://www.google.com/maps?q=$latVal,$lngVal\n" else "") +
                                (if (!emergency.w3wAddress.isNullOrBlank()) "What3Words: ///${emergency.w3wAddress}\n" else "") +
                                "Received via ARYAA at $timeStr"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    },
                    border = BorderStroke(1.dp, AryaaColors.White.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AryaaColors.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = AryaaColors.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SHARE LOCATION",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                // 4. DISMISS BUTTON
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
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