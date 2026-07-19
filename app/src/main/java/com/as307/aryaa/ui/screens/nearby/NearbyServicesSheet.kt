package com.as307.aryaa.ui.screens.nearby

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.NearbyServiceResponse
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyServicesSheet(
    latitude: Double,
    longitude: Double,
    api: AryaaApi,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf("police") }
    var isLoading by remember { mutableStateOf(false) }
    var servicesList by remember { mutableStateOf<List<NearbyServiceResponse>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reload services when coordinates or type changes
    LaunchedEffect(latitude, longitude, selectedType) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val response = api.getNearbyServices(
                    lat = latitude,
                    lng = longitude,
                    type = selectedType
                )
                if (response.isSuccessful) {
                    servicesList = response.body() ?: emptyList()
                } else {
                    if (response.code() == 503) {
                        errorMessage = "Nearby services lookup is unavailable in production (API key missing)."
                    } else {
                        errorMessage = "Failed to fetch nearby services. Code: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Network error. Please check your internet connection."
            } finally {
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = AryaaColors.Navy,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AryaaColors.NavyBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // Title
            Text(
                text = "Nearby Help",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AryaaColors.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "police" to "👮 Police",
                    "hospital" to "🏥 Hospital",
                    "pharmacy" to "💊 Pharmacy",
                    "fire" to "🔥 Fire"
                )

                categories.forEach { (type, label) ->
                    val isSelected = selectedType == type
                    NearbyFilterChip(
                        selected = isSelected,
                        label = label,
                        onClick = { selectedType = type }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Body Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = AryaaColors.Blue)
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        color = AryaaColors.Crimson,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (servicesList.isEmpty()) {
                    Text(
                        text = "No services found nearby.",
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        color = AryaaColors.Slate
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(servicesList) { service ->
                            ServiceItemRow(service = service)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ServiceItemRow(service: NearbyServiceResponse) {
    val context = LocalContext.current
    val formattedDistance = if (service.distanceMeters >= 1000) {
        String.format("%.1f km", service.distanceMeters / 1000.0)
    } else {
        "${service.distanceMeters} m"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AryaaColors.NavyCard),
        border = BorderStroke(1.dp, AryaaColors.NavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AryaaColors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDistance,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AryaaColors.Blue
                )
                if (!service.address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = service.address,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = AryaaColors.Slate,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))

            // Action Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!service.phone.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phone}"))
                            context.startActivity(intent)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = AryaaColors.Blue.copy(alpha = 0.12f),
                            contentColor = AryaaColors.Blue
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = "Call",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val gmmIntentUri = Uri.parse("google.navigation:q=${service.lat},${service.lng}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${service.lat},${service.lng}"))
                            context.startActivity(webIntent)
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AryaaColors.NavyBorder,
                        contentColor = AryaaColors.SlateLight
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Filled.Directions,
                        contentDescription = "Navigate",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AryaaColors.Blue else AryaaColors.NavyCard)
            .border(1.dp, if (selected) AryaaColors.Blue else AryaaColors.NavyBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (selected) AryaaColors.White else AryaaColors.Slate
        )
    }
}
