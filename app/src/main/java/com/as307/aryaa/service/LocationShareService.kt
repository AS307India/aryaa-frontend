package com.as307.aryaa.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.as307.aryaa.MainActivity
import com.as307.aryaa.R
import com.as307.aryaa.data.local.LocationSharePreferences
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.LocationShareUpdateRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class LocationShareService : Service() {

    @Inject lateinit var api: AryaaApi
    @Inject lateinit var tokenStorage: TokenStorage
    @Inject lateinit var locationSharePreferences: LocationSharePreferences
    @Inject lateinit var locationShareManager: LocationShareManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var currentSessionId: String? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START_SHARE -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_NOT_STICKY
                val shareUrl = intent.getStringExtra(EXTRA_SHARE_URL) ?: ""
                val expiresAt = intent.getStringExtra(EXTRA_EXPIRES_AT) ?: ""
                val contactCount = intent.getIntExtra(EXTRA_CONTACT_COUNT, 0)
                currentSessionId = sessionId
                startSharingForeground(sessionId, shareUrl, expiresAt, contactCount)
            }
            ACTION_STOP_SHARE -> {
                handleStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun startSharingForeground(
        sessionId: String,
        shareUrl: String,
        expiresAt: String,
        contactCount: Int
    ) {
        val notification = buildNotification(contactCount)

        val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (hasLocation) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                } else {
                    0
                }
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Update in-memory manager state
        locationShareManager.setActiveShare(
            ActiveLocationShare(
                sessionId = sessionId,
                shareUrl = shareUrl,
                expiresAt = expiresAt,
                contactCount = contactCount
            )
        )

        // Schedule auto-stop timer at expiry
        serviceScope.launch {
            try {
                val expiryInstant = Instant.parse(expiresAt)
                val delayMs = maxOf(0L, expiryInstant.toEpochMilli() - System.currentTimeMillis())
                kotlinx.coroutines.delay(delayMs)
                android.util.Log.d("LocationShareService", "Auto-stopping at expiry")
                handleStop()
            } catch (e: Exception) {
                android.util.Log.e("LocationShareService", "Expiry timer error", e)
            }
        }

        // Start periodic location updates (every 25 seconds — battery-friendly)
        startLocationUpdates(sessionId)
    }

    private fun startLocationUpdates(sessionId: String) {
        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            25_000L // 25 seconds — battery-friendly for sharing vs SOS high accuracy
        ).apply {
            setMinUpdateIntervalMillis(15_000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                pushLocationUpdate(sessionId, location.latitude, location.longitude, location.accuracy.toDouble())
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            android.util.Log.e("LocationShareService", "Missing location permissions", e)
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun pushLocationUpdate(sessionId: String, lat: Double, lng: Double, accuracy: Double) {
        serviceScope.launch {
            try {
                val token = tokenStorage.getToken() ?: return@launch
                api.updateLocationShare(
                    sessionId = sessionId,
                    request = LocationShareUpdateRequest(
                        lat = lat,
                        lng = lng,
                        accuracy = accuracy,
                        timestamp = Instant.now().toString()
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("LocationShareService", "Location push failed: ${e.message}")
            }
        }
    }

    private fun handleStop() {
        // 1. Clear in-memory state immediately (synchronous) so the UI
        //    updates on the very first tap — the StateFlow observers see
        //    null before we even touch the network or prefs.
        locationShareManager.clearActiveShare()

        val sessionId = currentSessionId
        stopLocationUpdates()

        // 2. Best-effort async cleanup: call API and clear prefs.
        //    We intentionally do this BEFORE stopSelf() so the scope is
        //    still alive when the coroutine runs.
        serviceScope.launch {
            if (sessionId != null) {
                try {
                    api.stopLocationShare(sessionId)
                } catch (e: Exception) {
                    android.util.Log.w("LocationShareService", "Stop API call failed: ${e.message}")
                }
            }
            try {
                locationSharePreferences.clearSession()
            } catch (e: Exception) {
                android.util.Log.w("LocationShareService", "Prefs clear failed: ${e.message}")
            }
            // 3. Stop the service only after async work is done
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(contactCount: Int): Notification {
        val channelId = "aryaa_location_share"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "ARYAA Location Sharing",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while actively sharing your location"
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_location_share", true)
        }
        val openPi = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, LocationShareService::class.java).apply {
            action = ACTION_STOP_SHARE
        }
        val stopPi = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_sync)
            .setContentTitle("📍 Sharing your location")
            .setContentText("Sharing with $contactCount contact${if (contactCount != 1) "s" else ""} — tap to manage")
            .setColor(0xFF3B82F6.toInt()) // Royal Blue
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPi)
            .addAction(0, "Stop", stopPi)
            .build()
    }

    override fun onDestroy() {
        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SHARE = "com.as307.aryaa.action.START_LOCATION_SHARE"
        const val ACTION_STOP_SHARE = "com.as307.aryaa.action.STOP_LOCATION_SHARE"
        const val EXTRA_SESSION_ID = "com.as307.aryaa.extra.LOCATION_SHARE_SESSION_ID"
        const val EXTRA_SHARE_URL = "com.as307.aryaa.extra.LOCATION_SHARE_URL"
        const val EXTRA_EXPIRES_AT = "com.as307.aryaa.extra.LOCATION_SHARE_EXPIRES_AT"
        const val EXTRA_CONTACT_COUNT = "com.as307.aryaa.extra.LOCATION_SHARE_CONTACT_COUNT"
        private const val NOTIFICATION_ID = 1003
    }
}
