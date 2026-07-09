package com.as307.aryaa.service

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented E2E test for the full SOS service flow.
 *
 * Prerequisites (must be met before running):
 * - Backend Fastify server running at 10.0.2.2:3000 (via `npm run dev`)
 * - Emulator has network access to the host machine
 *
 * On Android 14+ (targetSdk=34), FOREGROUND_SERVICE_LOCATION type requires
 * runtime location permission granted to the test process — we do this via
 * UiAutomation in setUp().
 */
@RunWith(AndroidJUnit4::class)
class SosE2EWithServiceTest {

    private val ctx by lazy { ApplicationProvider.getApplicationContext<android.content.Context>() }
    private val client = OkHttpClient()
    private val backendBase = "http://10.0.2.2:3000"

    @Before
    fun grantLocationPermission() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.grantRuntimePermission(ctx.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        uiAutomation.grantRuntimePermission(ctx.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uiAutomation.grantRuntimePermission(ctx.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun notificationChannel_existsAfterAppStart() {
        val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
        val channel = nm?.getNotificationChannel("aryaa_sos_active")

        assertNotNull(
            "AryaaApplication must register 'aryaa_sos_active' channel on startup",
            channel
        )
        assertEquals(
            "Channel importance must be HIGH for SOS alerts",
            android.app.NotificationManager.IMPORTANCE_HIGH,
            channel?.importance
        )
    }

    @Test
    fun backendLocationUpdateEndpoint_isReachable() {
        // Send an unauthenticated request — expect 401, not a ConnectException.
        // A 4xx proves the backend is up and reachable from the emulator.
        val body = """{"sosEventId":"test","latitude":12.97,"longitude":77.59,"timestamp":"2026-07-01T12:00:00Z"}"""
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$backendBase/api/sos/location-update")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val code = response.code
        response.close()

        assert(code in 400..499) {
            "Expected 4xx from backend (unauthenticated), got $code — backend may not be running at $backendBase"
        }
    }

    @Test
    fun backendSosCancelEndpoint_isReachable() {
        val body = """{"sosEventId":"test"}"""
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$backendBase/api/sos/cancel")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val code = response.code
        response.close()

        assert(code in 400..499) {
            "Expected 4xx from backend (unauthenticated), got $code — backend may not be running at $backendBase"
        }
    }

    @Test
    fun sosService_startsAndCancelsWithoutCrash() {
        val scenario = androidx.test.core.app.ActivityScenario.launch(com.as307.aryaa.MainActivity::class.java)
        try {
            val contacts = arrayListOf(
                SosContactSnapshot("E2E Contact", "+919000000001")
            )

            val startIntent = Intent(ctx, SosService::class.java).apply {
                action = SosService.ACTION_START_SOS
                putExtra(SosService.EXTRA_SOS_EVENT_ID, "e2e-test-event")
                putExtra(SosService.EXTRA_CONTACTS, contacts)
            }

            ctx.startForegroundService(startIntent)
            Thread.sleep(1000) // Let it initialise

            val cancelIntent = Intent(ctx, SosService::class.java).apply {
                action = SosService.ACTION_CANCEL_SOS
            }
            ctx.startService(cancelIntent)
            Thread.sleep(500)
        } finally {
            scenario.close()
        }
        // Reaching here without exception = pass
    }
}
