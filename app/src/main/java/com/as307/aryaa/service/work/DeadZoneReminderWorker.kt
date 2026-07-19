package com.as307.aryaa.service.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.as307.aryaa.MainActivity
import com.as307.aryaa.R
import com.as307.aryaa.data.local.DeadZonePreferences
import com.as307.aryaa.service.DeadZoneReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class DeadZoneReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface DeadZoneReminderEntryPoint {
        fun deadZonePreferences(): DeadZonePreferences
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            DeadZoneReminderEntryPoint::class.java
        )
        val prefs = entryPoint.deadZonePreferences()

        val checkInId = prefs.getActiveCheckInId() ?: return Result.success()
        val mode = prefs.getMode() ?: "PLAIN"
        val destination = prefs.getDestination() ?: ""

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch the app when tapping the notification body
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPi = PendingIntent.getActivity(appContext, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        // Action intent to trigger the check-in call in the background
        val checkInActionIntent = Intent(appContext, DeadZoneReceiver::class.java).apply {
            action = DeadZoneReceiver.ACTION_CONFIRM_SAFE
            putExtra(DeadZoneReceiver.EXTRA_CHECKIN_ID, checkInId)
            putExtra(DeadZoneReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val checkInPi = PendingIntent.getBroadcast(
            appContext,
            2,
            checkInActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val content: String
        val actionText: String

        when (mode) {
            "SAFE_WALK" -> {
                title = "Safe Walk Arrival Check"
                content = if (destination.isNotBlank()) "Did you arrive at $destination? Confirm now." else "Did you arrive safely? Confirm now."
                actionText = "I'VE ARRIVED"
            }
            "HEARTBEAT" -> {
                title = "Safety Heartbeat Ping"
                content = "Safety interval completed. Confirm you are OK."
                actionText = "I'M OK"
            }
            else -> {
                title = "Dead Zone Safety Check"
                content = "Your Dead Zone timer has completed. Please check in."
                actionText = "I'M SAFE"
            }
        }

        val builder = NotificationCompat.Builder(appContext, "aryaa_deadzone_reminder")
            .setSmallIcon(R.drawable.ic_notification_sync)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFF59E0B.toInt()) // Saffron Amber
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, actionText, checkInPi)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
        android.util.Log.d("DEADZONE_WORK", "Posted local reminder notification for mode: $mode")

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 1004
    }
}
