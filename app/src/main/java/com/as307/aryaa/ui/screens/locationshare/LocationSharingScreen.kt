package com.as307.aryaa.ui.screens.locationshare

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.data.local.LocationSharePreferences
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.remote.dto.LocationShareStartRequest
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.service.ActiveLocationShare
import com.as307.aryaa.service.LocationShareManager
import com.as307.aryaa.service.LocationShareService
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun LocationSharingScreen(
    contactsRepository: ContactsRepository,
    api: AryaaApi,
    locationSharePreferences: LocationSharePreferences,
    locationShareManager: LocationShareManager,
    activeLocationShare: ActiveLocationShare?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contacts by remember { mutableStateOf<List<ContactDto>>(emptyList()) }
    var selectedContactIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDuration by remember { mutableStateOf(30) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isActive = activeLocationShare != null

    LaunchedEffect(Unit) {
        contactsRepository.getContacts().first { list ->
            contacts = list
            true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AryaaColors.Navy)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = if (isActive) "📍 Location Sharing Active" else "📍 Share Your Location",
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = AryaaColors.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isActive)
                "Your location is being shared with ${activeLocationShare!!.contactCount} contact${if (activeLocationShare.contactCount != 1) "s" else ""}."
            else
                "Let a contact see where you are in real time — for your next walk, event, or trip.",
            fontFamily = InterFamily,
            fontSize = 14.sp,
            color = AryaaColors.SlateLight
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isActive) {
            // --- Active Session Card ---
            ActiveSessionCard(
                shareUrl = activeLocationShare!!.shareUrl,
                expiresAt = activeLocationShare.expiresAt,
                onStop = {
                    val intent = Intent(context, LocationShareService::class.java).apply {
                        action = LocationShareService.ACTION_STOP_SHARE
                    }
                    context.startService(intent)
                    onNavigateBack()
                }
            )
        } else {
            // --- Duration Picker ---
            Text(
                text = "Duration",
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AryaaColors.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(30 to "30 min", 60 to "1 hour", 120 to "2 hours").forEach { (value, label) ->
                    val isSelected = selectedDuration == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AryaaColors.Blue else AryaaColors.NavyCard)
                            .border(1.dp, if (isSelected) AryaaColors.Blue else AryaaColors.NavyBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedDuration = value },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) AryaaColors.White else AryaaColors.Slate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Contact Multi-Select ---
            Text(
                text = "Share with",
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AryaaColors.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (contacts.isEmpty()) {
                Text(
                    text = "No contacts found. Add contacts first.",
                    fontFamily = InterFamily,
                    fontSize = 14.sp,
                    color = AryaaColors.Slate
                )
            } else {
                contacts.forEach { contact ->
                    val isChecked = contact.id in selectedContactIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AryaaColors.NavyCard)
                            .border(1.dp, if (isChecked) AryaaColors.Blue else AryaaColors.NavyBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedContactIds = if (isChecked)
                                    selectedContactIds - contact.id
                                else
                                    selectedContactIds + contact.id
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                selectedContactIds = if (isChecked)
                                    selectedContactIds - contact.id
                                else
                                    selectedContactIds + contact.id
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AryaaColors.Blue,
                                uncheckedColor = AryaaColors.Slate,
                                checkmarkColor = AryaaColors.White
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = contact.name,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = AryaaColors.White
                            )
                            Text(
                                text = contact.phone,
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = AryaaColors.Slate
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            error?.let {
                Text(text = it, color = AryaaColors.Crimson, fontFamily = InterFamily, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- Start Button ---
            Button(
                onClick = {
                    if (selectedContactIds.isEmpty()) {
                        error = "Please select at least one contact."
                        return@Button
                    }
                    error = null
                    isLoading = true
                    scope.launch {
                        try {
                            val response = api.startLocationShare(
                                LocationShareStartRequest(
                                    durationMinutes = selectedDuration,
                                    contactIds = selectedContactIds.toList()
                                )
                            )
                            if (response.isSuccessful) {
                                val body = response.body()!!
                                val expiresAt = java.time.Instant.now()
                                    .plusSeconds((selectedDuration * 60).toLong())
                                    .toString()
                                locationSharePreferences.saveSession(
                                    sessionId = body.sessionId,
                                    shareToken = body.shareToken,
                                    shareUrl = body.shareUrl,
                                    expiresAt = expiresAt
                                )
                                locationShareManager.setActiveShare(
                                    ActiveLocationShare(
                                        sessionId = body.sessionId,
                                        shareUrl = body.shareUrl,
                                        expiresAt = expiresAt,
                                        contactCount = selectedContactIds.size
                                    )
                                )
                                // Start foreground service
                                val intent = Intent(context, LocationShareService::class.java).apply {
                                    action = LocationShareService.ACTION_START_SHARE
                                    putExtra(LocationShareService.EXTRA_SESSION_ID, body.sessionId)
                                    putExtra(LocationShareService.EXTRA_SHARE_URL, body.shareUrl)
                                    putExtra(LocationShareService.EXTRA_EXPIRES_AT, expiresAt)
                                    putExtra(LocationShareService.EXTRA_CONTACT_COUNT, selectedContactIds.size)
                                }
                                context.startForegroundService(intent)
                                onNavigateBack()
                            } else {
                                error = "Failed to start sharing. Please try again."
                            }
                        } catch (e: Exception) {
                            error = "Network error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && selectedContactIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AryaaColors.Blue,
                    contentColor = AryaaColors.White,
                    disabledContainerColor = AryaaColors.NavyBorder,
                    disabledContentColor = AryaaColors.Slate
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = AryaaColors.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Start Sharing",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSessionCard(
    shareUrl: String,
    expiresAt: String,
    onStop: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AryaaColors.NavyCard, RoundedCornerShape(12.dp))
            .border(1.dp, AryaaColors.Blue, RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "Share Link",
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = AryaaColors.Slate
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = shareUrl,
            fontFamily = InterFamily,
            fontSize = 13.sp,
            color = AryaaColors.Blue
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Copy link button
            Button(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Share URL", shareUrl)
                    clipboard.setPrimaryClip(clip)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AryaaColors.NavyBorder,
                    contentColor = AryaaColors.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Copy Link", fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
            // Share via system sheet
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Track my location: $shareUrl")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AryaaColors.Blue,
                    contentColor = AryaaColors.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Share", fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AryaaColors.NavyBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AryaaColors.Crimson,
                contentColor = AryaaColors.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Stop Sharing",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
