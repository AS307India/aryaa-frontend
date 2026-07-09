package com.as307.aryaa.service

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [SosService].
 *
 * On Android 14+ (targetSdk=34), starting a foreground service with
 * foregroundServiceType="location" requires location permission to be
 * runtime-granted to the test process — declaration in the manifest alone
 * is not sufficient. We grant it via UiAutomation in setUp().
 *
 * Verifies:
 * 1. The notification channel "aryaa_sos_active" is registered on app startup.
 * 2. The service starts without crashing on a valid ACTION_START_SOS intent.
 * 3. The service handles ACTION_CANCEL_SOS without crashing.
 */
@RunWith(AndroidJUnit4::class)
class SosServiceTest {

    private val ctx by lazy { ApplicationProvider.getApplicationContext<android.content.Context>() }

    @Before
    fun grantLocationPermission() {
        // Required on API 34+: FOREGROUND_SERVICE_LOCATION type requires runtime grant
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.grantRuntimePermission(ctx.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        uiAutomation.grantRuntimePermission(ctx.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @Test
    fun notificationChannel_aryaaSosActive_isRegistered() {
        val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
        val channel = nm?.getNotificationChannel("aryaa_sos_active")
        assertNotNull(
            "AryaaApplication must register 'aryaa_sos_active' notification channel on startup",
            channel
        )
    }

    @Test
    fun startSos_doesNotCrash() {
        val contacts = arrayListOf(
            SosContactSnapshot("Test Contact", "+919876543210")
        )
        val startIntent = Intent(ctx, SosService::class.java).apply {
            action = SosService.ACTION_START_SOS
            putExtra(SosService.EXTRA_SOS_EVENT_ID, "instrumented-test-event")
            putExtra(SosService.EXTRA_CONTACTS, contacts)
        }
        ctx.startForegroundService(startIntent)
        Thread.sleep(800)
        // Reaching here without SecurityException = pass
    }

    @Test
    fun cancelSos_doesNotCrash() {
        val contacts = arrayListOf(SosContactSnapshot("Cancel Contact", "+919000000000"))
        val startIntent = Intent(ctx, SosService::class.java).apply {
            action = SosService.ACTION_START_SOS
            putExtra(SosService.EXTRA_SOS_EVENT_ID, "cancel-test-event")
            putExtra(SosService.EXTRA_CONTACTS, contacts)
        }
        ctx.startForegroundService(startIntent)
        Thread.sleep(500)

        val cancelIntent = Intent(ctx, SosService::class.java).apply {
            action = SosService.ACTION_CANCEL_SOS
        }
        ctx.startService(cancelIntent)
        Thread.sleep(500)
        // Reaching here = cancel handled cleanly
    }
}
