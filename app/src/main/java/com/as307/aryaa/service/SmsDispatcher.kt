package com.as307.aryaa.service

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Formats a UTC ISO-8601 timestamp to a human-readable IST string.
 * Extracted as a top-level function so it is testable without Android framework.
 */
internal fun formatSosTriggeredAt(isoString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(isoString) ?: return isoString
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        formatter.format(date) + " IST"
    } catch (e: Exception) {
        isoString
    }
}

/**
 * Builds the full SOS alert SMS body.
 * Extracted as a top-level function so it is testable without Android framework.
 */
internal fun buildSosMessage(
    userName: String,
    triggeredAt: String,
    latitude: Double?,
    longitude: Double?,
    w3wAddress: String? = null
): String {
    val formattedTime = formatSosTriggeredAt(triggeredAt)
    return if (w3wAddress != null && w3wAddress.isNotBlank()) {
        val cleanW3w = w3wAddress.removePrefix("///")
        "🆘 $userName needs help! SOS via ARYAA.\n" +
                "Location: ///$cleanW3w (what3words)\n" +
                "Coords: ${latitude ?: 0.0}, ${longitude ?: 0.0}\n" +
                "Time: $formattedTime\n" +
                "Open ///$cleanW3w in the what3words app to navigate."
    } else if (latitude != null && longitude != null) {
        "🆘 $userName needs help! SOS via ARYAA.\n" +
                "Location: $latitude, $longitude\n" +
                "Time: $formattedTime"
    } else {
        "🆘 $userName needs help! SOS via ARYAA.\n" +
                "Location unavailable.\n" +
                "Time: $formattedTime"
    }
}

interface SmsDispatcher {
    fun sendSosAlerts(
        contacts: List<SosContactSnapshot>,
        userName: String,
        triggeredAt: String,
        latitude: Double?,
        longitude: Double?,
        w3wAddress: String?
    )
}

@Singleton
class SmsDispatcherImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profilePreferences: com.as307.aryaa.data.local.ProfilePreferences
) : SmsDispatcher {

    override fun sendSosAlerts(
        contacts: List<SosContactSnapshot>,
        userName: String,
        triggeredAt: String,
        latitude: Double?,
        longitude: Double?,
        w3wAddress: String?
    ) {
        val alertsEnabled = kotlinx.coroutines.runBlocking {
            profilePreferences.getOfflineSmsAlerts()
        }
        if (!alertsEnabled) {
            android.util.Log.d("SmsDispatcher", "Offline SMS alerts disabled, skipping dispatch.")
            return
        }

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val message = buildSosMessage(userName, triggeredAt, latitude, longitude, w3wAddress)


        for (contact in contacts) {
            val phone = contact.phone
            try {
                if (message.length > 160) {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(phone, null, message, null, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("SmsDispatcher", "Failed to send SMS to ${contact.name} (${contact.phone})", e)
            }
        }
    }
}
