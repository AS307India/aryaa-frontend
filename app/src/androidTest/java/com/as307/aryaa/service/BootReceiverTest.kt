package com.as307.aryaa.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.as307.aryaa.data.local.db.ActiveSosDao
import com.as307.aryaa.data.local.db.ActiveSosEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import com.as307.aryaa.MainActivity
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var activeSosDao: ActiveSosDao
    private lateinit var notificationManager: NotificationManager
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        
        // Grant location and notification permissions for Android 13/14 compatibility
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val pkg = context.packageName
        uiAutomation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_FINE_LOCATION)
        uiAutomation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            uiAutomation.grantRuntimePermission(pkg, Manifest.permission.POST_NOTIFICATIONS)
        }

        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Retrieve the Hilt-provided ActiveSosDao to write test data
        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiver.BootReceiverEntryPoint::class.java
        )
        activeSosDao = entryPoint.activeSosDao()
        
        // Clean up database and running services before test
        runBlocking {
            activeSosDao.clearActiveSos()
        }
        context.stopService(Intent(context, SosService::class.java))
        notificationManager.cancel(1001) // SosService NOTIFICATION_ID

        // Launch MainActivity to bring the process to the foreground,
        // bypassing Android 14+ background start restrictions on the foreground service
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() = runBlocking {
        scenario?.close()
        activeSosDao.clearActiveSos()
        context.stopService(Intent(context, SosService::class.java))
        notificationManager.cancel(1001)
    }

    private fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        val services = manager.getRunningServices(Integer.MAX_VALUE)
        return services.any { it.service.className == SosService::class.java.name }
    }

    @Test
    fun sendBootCompletedBroadcast_withActiveSosInRoom_startsSosService() = runBlocking {
        // 1. Insert ActiveSosEntity representing an ongoing emergency
        val entity = ActiveSosEntity(
            sosEventId = "event-boot-test-999",
            triggeredAt = "2026-07-01T12:00:00Z",
            w3wAddress = "///filled.count.soap",
            contactsJson = "[]",
            latitude = 12.9,
            longitude = 77.5
        )
        activeSosDao.insertActiveSos(entity)

        // 2. Instantiate and call BootReceiver onReceive directly
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED).apply {
            setPackage(context.packageName)
        }
        val receiver = BootReceiver()
        receiver.onReceive(context, intent)

        // 3. Wait a moment for async BroadcastReceiver coroutine to execute
        var serviceStarted = false
        for (i in 1..25) {
            Thread.sleep(200)
            if (isServiceRunning()) {
                serviceStarted = true
                break
            }
        }

        assertTrue("SosService should be running on foreground restart", serviceStarted)
    }

    @Test
    fun sendBootCompletedBroadcast_withNoActiveSosInRoom_doesNotStartService() = runBlocking {
        // 1. Instantiate and call BootReceiver onReceive directly (Room database is empty)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED).apply {
            setPackage(context.packageName)
        }
        val receiver = BootReceiver()
        receiver.onReceive(context, intent)

        // 2. Wait a moment and verify service is NOT started
        Thread.sleep(1500)
        assertFalse("SosService should not be running when no active SOS is present", isServiceRunning())
    }

    @Test
    fun sendOtherBroadcast_doesNotStartService() = runBlocking {
        // 1. Insert ActiveSosEntity
        val entity = ActiveSosEntity(
            sosEventId = "event-boot-test-999",
            triggeredAt = "2026-07-01T12:00:00Z",
            w3wAddress = "///filled.count.soap",
            contactsJson = "[]",
            latitude = 12.9,
            longitude = 77.5
        )
        activeSosDao.insertActiveSos(entity)

        // 2. Instantiate and call BootReceiver onReceive directly with other action
        val intent = Intent(Intent.ACTION_POWER_CONNECTED).apply {
            setPackage(context.packageName)
        }
        val receiver = BootReceiver()
        receiver.onReceive(context, intent)

        // 3. Wait a moment and verify service is NOT started
        Thread.sleep(1500)
        assertFalse("SosService should not be running on other broadcast actions", isServiceRunning())
    }
}
