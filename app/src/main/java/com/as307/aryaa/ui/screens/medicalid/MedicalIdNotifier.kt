package com.as307.aryaa.ui.screens.medicalid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface MedicalIdNotifier {
    fun showNotification()
    fun cancelNotification()
}

@Singleton
class MedicalIdNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MedicalIdNotifier {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "aryaa_medical_id"
        private const val NOTIFICATION_ID = 2002
    }

    override fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medical ID Details",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows shortcut access for Medical ID details on lockscreen"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MedicalIdActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Medical ID available")
            .setContentText("Tap to view your Medical ID (visible on lock screen)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
