package com.as307.aryaa.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.as307.aryaa.MainActivity
import com.as307.aryaa.R
import com.as307.aryaa.data.repository.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AryaaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository

    @Inject
    lateinit var emergencyStateHolder: com.as307.aryaa.ui.screens.emergency.EmergencyStateHolder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        com.as307.aryaa.util.TestEnv.logDebug("FCM_SERVICE", "onNewToken: $token")
        serviceScope.launch {
            val result = fcmTokenRepository.registerToken(token)
            if (result.isSuccess) {
                com.as307.aryaa.util.TestEnv.logDebug("FCM_SERVICE", "Successfully registered new FCM token on boot/refresh")
            } else {
                com.as307.aryaa.util.TestEnv.logError("FCM_SERVICE", "Failed to register new FCM token on boot/refresh", result.exceptionOrNull())
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        com.as307.aryaa.util.TestEnv.logDebug("FCM_SERVICE", "onMessageReceived from: ${message.from}")
        com.as307.aryaa.util.TestEnv.logDebug("FCM_SERVICE", "Message data payload: ${message.data}")

        if (message.data["type"] == "SOS_CANCEL") {
            val sosEventId = message.data["sosEventId"] ?: ""
            if (sosEventId.isNotBlank()) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(sosEventId.hashCode())

                // Dismiss response screen if it is active for this event
                val active = emergencyStateHolder.activeEmergency.value
                if (active != null && active.sosEventId == sosEventId) {
                    emergencyStateHolder.clear()
                }
            }
            return
        }

        if (message.data["type"] == "SOS_ALERT") {
            val userName = message.data["victimName"] ?: message.data["userName"] ?: "A trusted contact"
            val lat = message.data["lat"] ?: message.data["latitude"] ?: ""
            val lng = message.data["lng"] ?: message.data["longitude"] ?: ""
            val w3w = message.data["w3w"] ?: message.data["w3wAddress"] ?: ""
            val triggeredAt = message.data["triggeredAt"] ?: ""
            val userPhone = message.data["userPhone"] ?: ""
            val sosEventId = message.data["eventId"] ?: message.data["sosEventId"] ?: ""
            val accuracy = message.data["accuracy"] ?: ""
            val tier = message.data["tier"] ?: "FAMILY"

            val locationString = when {
                w3w.isNotBlank() -> "///$w3w ($lat, $lng)"
                lat.isNotBlank() && lng.isNotBlank() -> "Location: $lat, $lng"
                else -> "Location unavailable"
            }

            val timeStr = if (triggeredAt.isNotBlank()) {
                try {
                    val instant = java.time.Instant.parse(triggeredAt)
                    val zonedDateTime = instant.atZone(java.time.ZoneId.of("Asia/Kolkata"))
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    zonedDateTime.format(formatter) + " IST"
                } catch (e: Exception) {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                        .withZone(java.time.ZoneId.of("Asia/Kolkata"))
                    formatter.format(java.time.Instant.now()) + " IST"
                }
            } else {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(java.time.ZoneId.of("Asia/Kolkata"))
                formatter.format(java.time.Instant.now()) + " IST"
            }

            val defaultTitle = "🆘 $userName needs help!"
            val defaultBody = "$locationString at $timeStr"

            val title = message.notification?.title ?: defaultTitle
            val body = message.notification?.body ?: defaultBody

            showIncomingSosNotification(
                title = title,
                body = body,
                userPhone = userPhone,
                sosEventId = sosEventId,
                userName = userName,
                latitude = lat,
                longitude = lng,
                w3wAddress = w3w,
                triggeredAt = triggeredAt,
                accuracy = accuracy,
                tier = tier
            )
        }
    }

    private fun showIncomingSosNotification(
        title: String,
        body: String,
        userPhone: String,
        sosEventId: String,
        userName: String,
        latitude: String,
        longitude: String,
        w3wAddress: String,
        triggeredAt: String,
        accuracy: String,
        tier: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = sosEventId.hashCode()

        // Deep Link Action - Target MainActivity with OPEN_EMERGENCY_RESPONSE action
        val viewIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.as307.aryaa.action.OPEN_EMERGENCY_RESPONSE"
            putExtra("sosEventId", sosEventId)
            putExtra("userName", userName)
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
            putExtra("w3wAddress", w3wAddress)
            putExtra("triggeredAt", triggeredAt)
            putExtra("userPhone", userPhone)
            putExtra("accuracy", accuracy)
            putExtra("tier", tier)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val viewPendingIntent = PendingIntent.getActivity(
            this,
            notificationId, // Unique request code per notification
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = Uri.parse("android.resource://$packageName/${R.raw.aryaa_emergency_alert}")

        // Builder configured for urgent, alarm-level notifications
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sos)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(0xFFFF6B1A.toInt()) // Saffron color #FF6B1A
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setFullScreenIntent(viewPendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(viewPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "View",
                viewPendingIntent
            )

        // Call Action - Dial Triggerer's phone number
        if (userPhone.isNotBlank()) {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$userPhone"))
            val callPendingIntent = PendingIntent.getActivity(
                this,
                notificationId + 1, // Unique call request code
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_call,
                "Call",
                callPendingIntent
            )
        }

        Log.d("SOUND_DEBUG", "Posting notification id=$notificationId on channel=$CHANNEL_ID soundUri=$soundUri")
        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "aryaa_sos_incoming_v4"
    }
}
