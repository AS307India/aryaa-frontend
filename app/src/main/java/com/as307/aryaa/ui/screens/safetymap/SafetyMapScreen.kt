package com.as307.aryaa.ui.screens.safetymap

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.SafetyMapPin
import com.as307.aryaa.data.remote.dto.SafetyReportRequest
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily
import kotlinx.coroutines.launch

private val CATEGORIES = listOf("HARASSMENT", "POOR_LIGHTING", "THEFT", "UNSAFE_ROAD", "OTHER")
private val CATEGORY_LABELS = mapOf(
    "HARASSMENT" to "Harassment",
    "POOR_LIGHTING" to "Poor Lighting",
    "THEFT" to "Theft / Robbery",
    "UNSAFE_ROAD" to "Unsafe Road",
    "OTHER" to "Other"
)
private val CATEGORY_EMOJI = mapOf(
    "HARASSMENT" to "⚠️",
    "POOR_LIGHTING" to "🌑",
    "THEFT" to "🔒",
    "UNSAFE_ROAD" to "🚧",
    "OTHER" to "📍"
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafetyMapScreen(api: AryaaApi) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pins by remember { mutableStateOf<List<SafetyMapPin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Bottom sheet — pin detail
    var selectedPin by remember { mutableStateOf<SafetyMapPin?>(null) }
    val pinSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Bottom sheet — report submission
    var showReportSheet by remember { mutableStateOf(false) }
    val reportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Report form state
    var reportCategory by remember { mutableStateOf("HARASSMENT") }
    var reportDescription by remember { mutableStateOf("") }
    var reportIsPublicSpace by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var reportLat by remember { mutableStateOf(20.5937) }  // Default: India center
    var reportLng by remember { mutableStateOf(78.9629) }
    var reportSubmitting by remember { mutableStateOf(false) }
    var reportStatusMessage by remember { mutableStateOf<String?>(null) }

    // Dispute state
    var disputeInProgress by remember { mutableStateOf(false) }
    var disputeStatusMessage by remember { mutableStateOf<String?>(null) }

    // WebView reference for JS calls
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun loadPins() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.getSafetyMapPins()
                if (response.isSuccessful) {
                    pins = response.body() ?: emptyList()
                    // Inject pins into the Leaflet map
                    val pinsJson = pins.joinToString(prefix = "[", postfix = "]") { pin ->
                        val disputedFlag = if (pin.disputed) "true" else "false"
                        val idsJson = pin.reportIds.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
                        "{\"lat\":${pin.latitude},\"lng\":${pin.longitude},\"category\":\"${pin.category}\",\"count\":${pin.reportCount},\"disputed\":$disputedFlag,\"reportIds\":$idsJson}"
                    }
                    webViewRef?.evaluateJavascript("window.loadPins($pinsJson);", null)
                } else {
                    errorMessage = "Failed to load safety map data."
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.localizedMessage}"
            }
            isLoading = false
        }
    }

    // Build the Leaflet HTML
    val leafletHtml = buildLeafletHtml()

    Box(modifier = Modifier.fillMaxSize()) {
        // WebView map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
                        override fun onPageFinished(view: WebView?, url: String?) {
                            loadPins()
                        }
                    }
                    // JS Bridge: called when user taps a pin on the map
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onPinTapped(lat: Double, lng: Double, category: String, count: Int, disputed: Boolean, reportIdsJson: String) {
                            val ids = reportIdsJson.trimStart('[').trimEnd(']').split(",").map { it.trim('"') }.filter { it.isNotEmpty() }
                            val pin = SafetyMapPin(
                                latitude = lat, longitude = lng,
                                category = category, reportCount = count,
                                disputed = disputed, reportIds = ids,
                                categoryBreakdown = emptyMap()
                            )
                            selectedPin = pin
                        }

                        @JavascriptInterface
                        fun onMapLongPress(lat: Double, lng: Double) {
                            reportLat = lat
                            reportLng = lng
                            showReportSheet = true
                        }
                    }, "AndroidBridge")

                    loadDataWithBaseURL(
                        "https://tile.openstreetmap.org",
                        leafletHtml,
                        "text/html",
                        "utf-8",
                        null
                    )
                    webViewRef = this
                }
            }
        )

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AryaaColors.Navy.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AryaaColors.Saffron)
            }
        }

        // Error banner
        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AryaaColors.Crimson.copy(alpha = 0.9f))
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(text = msg, color = AryaaColors.White, fontFamily = InterFamily, fontSize = 13.sp)
            }
        }

        // Header overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AryaaColors.Navy.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Column {
                Text(
                    text = "COMMUNITY SAFETY MAP",
                    color = AryaaColors.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${pins.size} active incident cluster${if (pins.size != 1) "s" else ""} · Long-press map to report",
                    color = AryaaColors.Slate,
                    fontFamily = InterFamily,
                    fontSize = 12.sp
                )
            }
        }

        // FAB — report incident
        FloatingActionButton(
            onClick = { showReportSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = AryaaColors.Saffron,
            contentColor = AryaaColors.White,
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Report Incident")
        }
    }

    // ── Pin detail bottom sheet ──────────────────────────────────────────────
    selectedPin?.let { pin ->
        ModalBottomSheet(
            onDismissRequest = {
                selectedPin = null
                disputeStatusMessage = null
            },
            sheetState = pinSheetState,
            containerColor = AryaaColors.NavyCard,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pin header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${CATEGORY_EMOJI[pin.category] ?: "📍"} ${CATEGORY_LABELS[pin.category] ?: pin.category}",
                        color = AryaaColors.White,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (pin.disputed) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFA726).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFFFA726), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Under Review", color = Color(0xFFFFA726), fontSize = 11.sp, fontFamily = InterFamily)
                        }
                    }
                }

                Text(
                    text = "${pin.reportCount} report${if (pin.reportCount != 1) "s" else ""} from ${pin.reportIds.size} verified unique reporter${if (pin.reportIds.size != 1) "s" else ""}",
                    color = AryaaColors.Slate,
                    fontFamily = InterFamily,
                    fontSize = 13.sp
                )

                Text(
                    text = "📍 %.4f, %.4f".format(pin.latitude, pin.longitude),
                    color = AryaaColors.Slate,
                    fontFamily = InterFamily,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Dispute section
                if (!pin.disputed) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AryaaColors.Navy)
                            .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🏠 Dispute this report?",
                            color = AryaaColors.White,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "If this location is incorrectly flagged and is within 50m of your registered home address, you can dispute it for admin review.",
                            color = AryaaColors.Slate,
                            fontFamily = InterFamily,
                            fontSize = 12.sp
                        )

                        disputeStatusMessage?.let {
                            Text(
                                text = it,
                                color = if (it.startsWith("✅")) Color(0xFF66BB6A) else AryaaColors.Crimson,
                                fontFamily = InterFamily,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                disputeInProgress = true
                                disputeStatusMessage = null
                                scope.launch {
                                    var anySuccess = false
                                    var lastError = ""
                                    for (reportId in pin.reportIds) {
                                        try {
                                            val resp = api.disputeSafetyReport(reportId)
                                            if (resp.isSuccessful) {
                                                anySuccess = true
                                            } else {
                                                lastError = when (resp.code()) {
                                                    403 -> "Your home address is not within 50m of this location, or this is a public-space report."
                                                    404 -> "Report not found."
                                                    else -> "Error ${resp.code()}"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            lastError = e.localizedMessage ?: "Network error"
                                        }
                                    }
                                    disputeStatusMessage = if (anySuccess) {
                                        "✅ Dispute filed. This pin is now marked Under Review."
                                    } else {
                                        "❌ $lastError"
                                    }
                                    if (anySuccess) loadPins()
                                    disputeInProgress = false
                                }
                            },
                            enabled = !disputeInProgress,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AryaaColors.NavyBorder,
                                contentColor = AryaaColors.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (disputeInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AryaaColors.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("File a Dispute", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFA726).copy(alpha = 0.08f))
                            .border(1.dp, Color(0xFFFFA726).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "⏳ This report is currently Under Review by an admin.",
                            color = Color(0xFFFFA726),
                            fontFamily = InterFamily,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ── Report submission bottom sheet ───────────────────────────────────────
    if (showReportSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showReportSheet = false
                reportStatusMessage = null
            },
            sheetState = reportSheetState,
            containerColor = AryaaColors.NavyCard,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = AryaaColors.Saffron, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REPORT SAFETY INCIDENT",
                        color = AryaaColors.White,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "📍 Reporting at: %.4f, %.4f".format(reportLat, reportLng),
                    color = AryaaColors.Slate,
                    fontFamily = InterFamily,
                    fontSize = 12.sp
                )

                // Category dropdown
                Box {
                    OutlinedTextField(
                        value = "${CATEGORY_EMOJI[reportCategory] ?: ""} ${CATEGORY_LABELS[reportCategory] ?: reportCategory}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category", color = AryaaColors.Slate) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryDropdownExpanded = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AryaaColors.White,
                            unfocusedTextColor = AryaaColors.White,
                            focusedBorderColor = AryaaColors.Saffron,
                            unfocusedBorderColor = AryaaColors.NavyBorder,
                            focusedContainerColor = AryaaColors.Navy,
                            unfocusedContainerColor = AryaaColors.Navy,
                            disabledTextColor = AryaaColors.White,
                            disabledBorderColor = AryaaColors.NavyBorder,
                            disabledContainerColor = AryaaColors.Navy
                        ),
                        enabled = false
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        containerColor = AryaaColors.NavyCard
                    ) {
                        CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${CATEGORY_EMOJI[cat]} ${CATEGORY_LABELS[cat]}",
                                        color = AryaaColors.White,
                                        fontFamily = InterFamily
                                    )
                                },
                                onClick = {
                                    reportCategory = cat
                                    // Auto-set isPublicSpace for road/lighting categories
                                    reportIsPublicSpace = cat == "POOR_LIGHTING" || cat == "UNSAFE_ROAD"
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reportDescription,
                    onValueChange = { reportDescription = it },
                    label = { Text("Description", color = AryaaColors.Slate) },
                    placeholder = { Text("Brief description of the incident...", color = AryaaColors.Slate.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AryaaColors.White,
                        unfocusedTextColor = AryaaColors.White,
                        focusedBorderColor = AryaaColors.Saffron,
                        unfocusedBorderColor = AryaaColors.NavyBorder,
                        focusedContainerColor = AryaaColors.Navy,
                        unfocusedContainerColor = AryaaColors.Navy
                    )
                )

                // isPublicSpace toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AryaaColors.Navy)
                        .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Public Space",
                            color = AryaaColors.White,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Streets, parks, public areas — cannot be disputed by nearby residents",
                            color = AryaaColors.Slate,
                            fontFamily = InterFamily,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = reportIsPublicSpace,
                        onCheckedChange = { reportIsPublicSpace = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AryaaColors.White,
                            checkedTrackColor = AryaaColors.Saffron,
                            uncheckedThumbColor = AryaaColors.Slate,
                            uncheckedTrackColor = AryaaColors.NavyCard
                        )
                    )
                }

                reportStatusMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (msg.startsWith("✅")) Color(0xFF66BB6A) else AryaaColors.Crimson,
                        fontFamily = InterFamily,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        if (reportDescription.isBlank()) {
                            reportStatusMessage = "❌ Please provide a description."
                            return@Button
                        }
                        reportSubmitting = true
                        reportStatusMessage = null
                        scope.launch {
                            try {
                                val resp = api.submitSafetyReport(
                                    SafetyReportRequest(
                                        category = reportCategory,
                                        description = reportDescription,
                                        latitude = reportLat,
                                        longitude = reportLng,
                                        isPublicSpace = reportIsPublicSpace
                                    )
                                )
                                if (resp.isSuccessful) {
                                    reportStatusMessage = "✅ Report submitted. Thank you for keeping your community safe."
                                    reportDescription = ""
                                    loadPins()
                                } else {
                                    reportStatusMessage = when (resp.code()) {
                                        429 -> "❌ You've reached the limit of 3 reports per day."
                                        401 -> "❌ Please log in again."
                                        else -> "❌ Submission failed (${resp.code()})."
                                    }
                                }
                            } catch (e: Exception) {
                                reportStatusMessage = "❌ Network error: ${e.localizedMessage}"
                            }
                            reportSubmitting = false
                        }
                    },
                    enabled = !reportSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AryaaColors.Saffron,
                        contentColor = AryaaColors.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (reportSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AryaaColors.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Submit Report",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun buildLeafletHtml(): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { background: #0d1b2a; }
  #map { width: 100vw; height: 100vh; }
  .custom-pin {
    display: flex; align-items: center; justify-content: center;
    border-radius: 50%; border: 3px solid white;
    font-size: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.5);
    cursor: pointer; transition: transform 0.15s;
  }
  .custom-pin:hover { transform: scale(1.15); }
  .leaflet-popup-content-wrapper {
    background: #1a2d42; color: #fff; border-radius: 12px;
    font-family: sans-serif; font-size: 13px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.4);
  }
  .leaflet-popup-tip { background: #1a2d42; }
</style>
</head>
<body>
<div id="map"></div>
<script>
const CATEGORY_COLORS = {
  'HARASSMENT': '#ef5350',
  'POOR_LIGHTING': '#5c6bc0',
  'THEFT': '#ff7043',
  'UNSAFE_ROAD': '#ffa726',
  'OTHER': '#78909c'
};
const CATEGORY_EMOJI = {
  'HARASSMENT': '⚠️',
  'POOR_LIGHTING': '🌑',
  'THEFT': '🔒',
  'UNSAFE_ROAD': '🚧',
  'OTHER': '📍'
};
const CATEGORY_LABELS = {
  'HARASSMENT': 'Harassment',
  'POOR_LIGHTING': 'Poor Lighting',
  'THEFT': 'Theft',
  'UNSAFE_ROAD': 'Unsafe Road',
  'OTHER': 'Other'
};

const map = L.map('map', { zoomControl: true }).setView([20.5937, 78.9629], 5);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  attribution: '© OpenStreetMap',
  maxZoom: 19
}).addTo(map);

const markersLayer = L.layerGroup().addTo(map);

// Long-press to report
let pressTimer = null;
map.on('mousedown touchstart', function(e) {
  pressTimer = setTimeout(function() {
    const lat = e.latlng ? e.latlng.lat : e.touches[0].latLng.lat;
    const lng = e.latlng ? e.latlng.lng : e.touches[0].latLng.lng;
    AndroidBridge.onMapLongPress(lat, lng);
  }, 600);
});
map.on('mouseup touchend mousemove', function() {
  clearTimeout(pressTimer);
});

window.loadPins = function(pins) {
  markersLayer.clearLayers();
  pins.forEach(function(pin) {
    const color = CATEGORY_COLORS[pin.category] || '#78909c';
    const emoji = CATEGORY_EMOJI[pin.category] || '📍';
    const size = Math.min(48, 32 + pin.count * 2);
    const icon = L.divIcon({
      className: '',
      html: '<div class="custom-pin" style="width:' + size + 'px;height:' + size + 'px;background:' + color + (pin.disputed ? ';border-color:#ffa726' : '') + '">' + emoji + '</div>',
      iconSize: [size, size],
      iconAnchor: [size/2, size/2]
    });
    const marker = L.marker([pin.lat, pin.lng], { icon: icon }).addTo(markersLayer);
    marker.on('click', function() {
      const idsJson = JSON.stringify(pin.reportIds);
      AndroidBridge.onPinTapped(pin.lat, pin.lng, pin.category, pin.count, pin.disputed, idsJson);
    });
    if (pin.disputed) {
      marker.bindPopup('<b>' + emoji + ' ' + (CATEGORY_LABELS[pin.category]||pin.category) + '</b><br>' + pin.count + ' reports · <span style="color:#ffa726">Under Review</span>');
    }
  });
};
</script>
</body>
</html>
""".trimIndent()
