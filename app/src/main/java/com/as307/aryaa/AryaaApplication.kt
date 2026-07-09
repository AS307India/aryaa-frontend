package com.as307.aryaa

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AryaaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannels()

        val isUnderTest = com.as307.aryaa.util.TestEnv.isUnderTest
        android.util.Log.d("AryaaTest", "AryaaApplication onCreate isUnderTest: $isUnderTest")
        if (isUnderTest) {
            try {
                val masterKey = MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val sharedPreferences = EncryptedSharedPreferences.create(
                    this,
                    "aryaa_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                sharedPreferences.edit().clear().commit()
            } catch (e: Exception) {
                // Ignore any keystore errors in unit test environments
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val activeChannel = NotificationChannel(
                "aryaa_sos_active",
                "SOS Active",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ongoing active SOS notification channel for ARYAA"
            }
            manager.createNotificationChannel(activeChannel)

            // Delete old channel to ensure custom sound setting takes effect on upgrade
            manager.deleteNotificationChannel("aryaa_sos_incoming")

            val soundUri = android.net.Uri.parse("android.resource://$packageName/${R.raw.aryaa_emergency_alert}")
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val incomingChannel = NotificationChannel(
                "aryaa_sos_incoming_v2",
                "Emergency SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when a trusted contact needs help. Cannot be silenced."
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                enableLights(true)
                lightColor = 0xFFEF4444.toInt() // Crimson
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            manager.createNotificationChannel(incomingChannel)
        }
    }
}
