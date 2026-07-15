package com.as307.aryaa.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.as307.aryaa.MainActivity
import com.as307.aryaa.R
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import com.as307.aryaa.data.repository.SosRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SosService : Service() {

    @Inject
    lateinit var sosRepository: SosRepository

    @Inject
    lateinit var smsDispatcher: SmsDispatcher

    @Inject
    lateinit var tokenStorage: TokenStorage

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var sosServiceManager: SosServiceManager

    @Inject
    lateinit var activeSosDao: com.as307.aryaa.data.local.db.ActiveSosDao

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    
    private var currentSosEventId: String? = null
    private val pendingUpdates = mutableListOf<PendingLocationUpdate>()

    data class PendingLocationUpdate(
        val latitude: Double,
        val longitude: Double,
        val timestamp: String
    )

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("SOS_NOTIF", "onStartCommand: action=" + intent?.action 
             + ", extras=" + intent?.extras?.keySet()?.joinToString())
        android.util.Log.d("SOS_DEBUG", "service started, action=" + intent?.action)
        if (intent == null) return START_STICKY

        when (intent.action) {
            ACTION_START_SOS -> {
                val sosEventId = intent.getStringExtra(EXTRA_SOS_EVENT_ID) ?: return START_STICKY
                @Suppress("UNCHECKED_CAST")
                val contacts = intent.getSerializableExtra(EXTRA_CONTACTS) as? List<SosContactSnapshot> ?: emptyList()
                val w3wAddress = intent.getStringExtra(EXTRA_W3W_ADDRESS)
                currentSosEventId = sosEventId
                startSosForeground(sosEventId, contacts, w3wAddress)
            }
            ACTION_CANCEL_SOS -> {
                handleCancelSos(intent)
            }
            ACTION_DURESS_CANCEL -> {
                handleDuressCancel()
            }
        }
        return START_STICKY
    }

    private fun startSosForeground(sosEventId: String, contacts: List<SosContactSnapshot>, w3wAddress: String?) {
        // 1. Show notification immediately (within 5 seconds limit)
        val notification = createSosNotification(sosEventId)
        val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
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

        // 2. Dispatch SMS Alerts asynchronously
        serviceScope.launch {
            val userName = tokenStorage.getUserName() ?: "A user"
            val triggeredAt = java.time.Instant.now().toString()
            
            // Best effort location for SMS dispatch (last known)
            val loc = locationProvider.getLastKnownLocation()
            
            smsDispatcher.sendSosAlerts(
                contacts = contacts,
                userName = userName,
                triggeredAt = triggeredAt,
                latitude = loc?.latitude,
                longitude = loc?.longitude,
                w3wAddress = w3wAddress
            )
        }

        // 3. Start continuous location updates (every 30 seconds)
        startLocationUpdates(sosEventId)

        // 4. Launch Lock Screen Escalation Prompt Timers (2-min, 5-min, and 15-min)
        serviceScope.launch {
            kotlinx.coroutines.delay(2 * 60 * 1000L) // 2 minutes
            showEscalationNotification(
                id = 2002,
                title = "⚠️ SOS active for 2 minutes",
                body = "Still in danger? Tap to call 112 immediately."
            )
        }

        serviceScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000L) // 5 minutes
            showEscalationNotification(
                id = 2005,
                title = "⚠️ SOS active for 5 minutes",
                body = "If you need police assistance, tap to call 112 now."
            )
        }

        serviceScope.launch {
            kotlinx.coroutines.delay(15 * 60 * 1000L) // 15 minutes
            showEscalationNotification(
                id = 2015,
                title = "⚠️ SOS active for 15 minutes",
                body = "Escalate to emergency services if needed — tap to call 112."
            )
        }
    }


    private fun startLocationUpdates(sosEventId: String) {
        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L // 30 seconds
        ).apply {
            setMinUpdateIntervalMillis(15000L) // 15 seconds
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                onLocationUpdated(sosEventId, location.latitude, location.longitude)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            android.util.Log.e("SosService", "Missing location permissions for continuous updates", e)
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun onLocationUpdated(sosEventId: String, lat: Double, lng: Double) {
        val now = java.time.Instant.now().toString()
        val update = PendingLocationUpdate(lat, lng, now)

        synchronized(pendingUpdates) {
            pendingUpdates.add(update)
        }

        flushPendingUpdates(sosEventId)
    }

    private fun flushPendingUpdates(sosEventId: String) {
        serviceScope.launch {
            val toSend = synchronized(pendingUpdates) {
                pendingUpdates.toList()
            }

            var successCount = 0
            for (pending in toSend) {
                val result = sosRepository.sendLocationUpdate(
                    sosEventId = sosEventId,
                    lat = pending.latitude,
                    lng = pending.longitude,
                    timestamp = pending.timestamp
                )
                if (result.isSuccess) {
                    successCount++
                } else {
                    // Stop flushing on network failure, retry on next callback
                    break
                }
            }

            synchronized(pendingUpdates) {
                for (i in 0 until successCount) {
                    if (pendingUpdates.isNotEmpty()) {
                        pendingUpdates.removeAt(0)
                    }
                }
            }
        }
    }

    private fun handleCancelSos(intent: Intent?) {
        stopLocationUpdates()

        val eventId = intent?.getStringExtra(EXTRA_SOS_EVENT_ID) ?: currentSosEventId
        android.util.Log.d("SOS_NOTIF", "handleCancelSos ENTERED, eventId param=" + eventId)
        
        if (eventId != null) {
            serviceScope.launch {
                try {
                    android.util.Log.d("SOS_NOTIF", "clearing Room now")
                    activeSosDao.clearActiveSos()
                    android.util.Log.d("SOS_NOTIF", "calling backend cancel with id=" + eventId)
                    val result = sosRepository.cancelSos(eventId)
                    android.util.Log.d("SOS_NOTIF", "cancel completed: " + result)
                    sosServiceManager.emitCancelledEvent()
                } catch (e: Exception) {
                    android.util.Log.e("SOS_NOTIF", "cancel failed", e)
                } finally {
                    stopForegroundIfSupported()
                    stopSelf()
                }
            }
        } else {
            stopForegroundIfSupported()
            stopSelf()
        }
    }

    private fun handleDuressCancel() {
        android.util.Log.d("SOS_NOTIF", "handleDuressCancel ENTERED")
        
        // 1. Create a quiet channel if on Android O+ (importance low, silent)
        val channelId = "aryaa_sync"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ARYAA Sync Services"
            val descriptionText = "Handles silent data synchronization in the background"
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Build disguised notification using standard/neutral colors and R.drawable.ic_notification_sync
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val disguisedNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_sync)
            .setContentTitle("ARYAA")
            .setContentText("Syncing your data")
            .setColor(0xFF8892A4.toInt()) // Slate neutral grey
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .build()

        // 3. Update the foreground notification with the disguised one!
        startForeground(NOTIFICATION_ID, disguisedNotification)

        // 4. Set a timer/coroutine to auto-resolve and stop this service after 2 hours!
        serviceScope.launch {
            kotlinx.coroutines.delay(2 * 60 * 60 * 1000L) // 2 hours
            android.util.Log.d("SOS_NOTIF", "Auto-resolving duress service after 2 hours")
            stopLocationUpdates()
            stopForegroundIfSupported()
            stopSelf()
        }
    }

    private fun stopForegroundIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createSosNotification(sosEventId: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, SosService::class.java).apply {
            action = ACTION_CANCEL_SOS
            putExtra(EXTRA_SOS_EVENT_ID, sosEventId)
        }
        android.util.Log.d("SOS_NOTIF", "building cancel PendingIntent, action=" 
            + cancelIntent.action + ", eventId=" + sosEventId)
        
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "aryaa_sos_active")
            .setSmallIcon(R.drawable.ic_notification_sos)
            .setContentTitle("SOS Active — ARYAA")
            .setContentText("Your trusted contacts have been alerted. Tap to open.")
            .setColor(0xFFEF4444.toInt()) // Crimson (#EF4444)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_notification_sos,
                "I'm Safe",
                cancelPendingIntent
            )
            .build()
    }

    private fun showEscalationNotification(id: Int, title: String, body: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dialPendingIntent = PendingIntent.getActivity(
            this,
            id,
            dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = Uri.parse("android.resource://$packageName/${R.raw.aryaa_emergency_alert}")

        val notification = NotificationCompat.Builder(this, "aryaa_sos_active")
            .setSmallIcon(R.drawable.ic_notification_sos)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(0xFFEF4444.toInt()) // Crimson
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setFullScreenIntent(dialPendingIntent, true) // Overlay on lock screen
            .setAutoCancel(true)
            .setContentIntent(dialPendingIntent)
            .build()

        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(id, notification)
    }

    override fun onDestroy() {
        stopLocationUpdates()
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(2002)
        notificationManager.cancel(2005)
        notificationManager.cancel(2015)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SOS = "com.as307.aryaa.action.START_SOS"
        const val ACTION_CANCEL_SOS = "com.as307.aryaa.action.CANCEL_SOS"
        const val ACTION_DURESS_CANCEL = "com.as307.aryaa.action.DURESS_CANCEL"
        const val ACTION_UPDATE_LOCATION = "com.as307.aryaa.action.UPDATE_LOCATION"

        const val EXTRA_SOS_EVENT_ID = "com.as307.aryaa.extra.SOS_EVENT_ID"
        const val EXTRA_CONTACTS = "com.as307.aryaa.extra.CONTACTS"
        const val EXTRA_W3W_ADDRESS = "com.as307.aryaa.extra.W3W_ADDRESS"

        private const val NOTIFICATION_ID = 1001
    }
}
