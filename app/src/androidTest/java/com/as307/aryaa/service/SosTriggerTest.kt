package com.as307.aryaa.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SosTriggerTest {

    private val ctx by lazy { ApplicationProvider.getApplicationContext<Context>() }

    @Before
    fun grantPermissions() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.grantRuntimePermission(ctx.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        uiAutomation.grantRuntimePermission(ctx.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uiAutomation.grantRuntimePermission(ctx.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun triggerSosNotification() {
        val contacts = arrayListOf(
            SosContactSnapshot("Notification Test Contact", "+919000000001")
        )
        val startIntent = Intent(ctx, SosService::class.java).apply {
            action = SosService.ACTION_START_SOS
            putExtra(SosService.EXTRA_SOS_EVENT_ID, "test-event-from-notif")
            putExtra(SosService.EXTRA_CONTACTS, contacts)
        }
        ctx.startForegroundService(startIntent)
        
        // Wait for service to start and post notification
        Thread.sleep(5000)

        // Get the active notification and trigger its action
        val notificationManager = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        val sosNotification = activeNotifications.find { it.id == 1001 }
        
        if (sosNotification != null) {
            val notification = sosNotification.notification
            val actions = notification.actions
            val cancelAction = actions?.find { it.title.toString() == "I'm Safe" }
            
            if (cancelAction != null) {
                android.util.Log.d("SOS_NOTIF", "Found I'm Safe action, triggering PendingIntent...")
                cancelAction.actionIntent.send()
                
                // Wait for the service to process the intent
                Thread.sleep(5000)
            } else {
                android.util.Log.e("SOS_NOTIF", "Cancel action not found in notification!")
            }
        } else {
            android.util.Log.e("SOS_NOTIF", "SOS notification not found in active notifications!")
        }
    }
}
