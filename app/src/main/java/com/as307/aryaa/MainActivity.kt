package com.as307.aryaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.data.repository.FcmTokenRepository
import com.as307.aryaa.data.repository.DeadZoneRepository
import com.as307.aryaa.ui.navigation.AryaaNavGraph
import com.as307.aryaa.ui.navigation.Destination
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.AryaaTheme
import com.as307.aryaa.util.VolumeTriggerHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStorage: TokenStorage
    @Inject lateinit var contactsRepository: ContactsRepository
    @Inject lateinit var fcmTokenRepository: FcmTokenRepository
    @Inject lateinit var sosRepository: com.as307.aryaa.data.repository.SosRepository
    @Inject lateinit var deadZoneRepository: DeadZoneRepository
    @Inject lateinit var sosServiceManager: com.as307.aryaa.service.SosServiceManager
    @Inject lateinit var fakeCallPreferences: com.as307.aryaa.data.local.FakeCallPreferences
    @Inject lateinit var profilePreferences: com.as307.aryaa.data.local.ProfilePreferences
    @Inject lateinit var emergencyStateHolder: com.as307.aryaa.ui.screens.emergency.EmergencyStateHolder
    @Inject lateinit var safetyLimitsPreferences: com.as307.aryaa.data.local.SafetyLimitsPreferences
    @Inject lateinit var locationSharePreferences: com.as307.aryaa.data.local.LocationSharePreferences
    @Inject lateinit var locationShareManager: com.as307.aryaa.service.LocationShareManager
    @Inject lateinit var api: com.as307.aryaa.data.remote.AryaaApi

    private var isVolumeTriggerEnabled = true

    // Stateless handler extracted for testability — no Android framework deps.
    // NOTE: This volume trigger only works when the app is in the foreground.
    // Background triggers (screen off) require AccessibilityService, which
    // Google Play may flag — explicitly out of scope.
    private val volumeTriggerHandler = VolumeTriggerHandler(windowMs = 2000L, requiredPresses = 3) {
        triggerSosFromVolumeButtons()
    }
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            android.util.Log.d("MainActivity", "Permission ${it.key} granted: ${it.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12+ SplashScreen API
        val splashScreen = installSplashScreen()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        super.onCreate(savedInstanceState)

        // Request required runtime permissions at startup
        val requiredPermissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.SEND_SMS
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(requiredPermissions.toTypedArray())

        // Configure edge-to-edge system bar styling.
        // Status bar uses navy background with light icons (i.e. dark style).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AryaaColors.Navy.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AryaaColors.NavyMid.toArgb())
        )

        // Collect volume button trigger settings flow
        lifecycleScope.launch {
            profilePreferences.getVolumeButtonTriggerFlow().collect { enabled ->
                isVolumeTriggerEnabled = enabled
            }
        }

        // Register FCM token on cold start if user is already logged in
        lifecycleScope.launch {
            if (tokenStorage.getToken() != null) {
                val result = fcmTokenRepository.fetchAndRegisterToken()
                android.util.Log.d("MainActivity", "Cold start FCM token registration status: $result")

                // Defensive WorkManager rescheduling check
                deadZoneRepository.getStatus().onSuccess { checkIn ->
                    if (checkIn != null) {
                        rescheduleDeadZoneReminder(checkIn.gracePeriodEnd!!, checkIn.checkInId)
                    }
                }
            }
        }

        handleEmergencyIntent(intent)
 
        val hasEmergencyExtras = intent?.hasExtra("sosEventId") == true && !intent.getStringExtra("sosEventId").isNullOrBlank()
        val isEmergency = intent?.action == "com.as307.aryaa.action.OPEN_EMERGENCY_RESPONSE" || hasEmergencyExtras
        val startDestination = if (isEmergency) {
            Destination.Home.route
        } else {
            intent.getStringExtra("start_destination") ?: Destination.Splash.route
        }

        setContent {
            AryaaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AryaaColors.Navy
                ) {
                    AryaaNavGraph(
                        tokenStorage = tokenStorage,
                        contactsRepository = contactsRepository,
                        sosRepository = sosRepository,
                        deadZoneRepository = deadZoneRepository,
                        sosServiceManager = sosServiceManager,
                        fakeCallPreferences = fakeCallPreferences,
                        startDestination = startDestination,
                        emergencyStateHolder = emergencyStateHolder,
                        safetyLimitsPreferences = safetyLimitsPreferences,
                        locationSharePreferences = locationSharePreferences,
                        locationShareManager = locationShareManager,
                        api = api
                    )
                }
            }
        }
    }

    private fun rescheduleDeadZoneReminder(gracePeriodEndIso: String, checkInId: String) {
        try {
            val endMillis = java.time.Instant.parse(gracePeriodEndIso).toEpochMilli()
            val delayMs = maxOf(0L, endMillis - System.currentTimeMillis())

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.as307.aryaa.service.work.DeadZoneCheckInWorker>()
                .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "deadzone_reminder_$checkInId",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
            android.util.Log.d("MainActivity", "Defensively rescheduled WorkManager deadzone_reminder_$checkInId")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to reschedule WorkManager", e)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEmergencyIntent(intent)
    }

    private fun handleEmergencyIntent(intent: android.content.Intent?) {
        val action = intent?.action
        val keys = intent?.extras?.keySet()?.joinToString()
        android.util.Log.d("EMERGENCY_DEBUG", "handleEmergencyIntent started action=$action keys=$keys")
        val hasEmergencyExtras = intent?.hasExtra("sosEventId") == true && !intent.getStringExtra("sosEventId").isNullOrBlank()
        val isEmergency = action == "com.as307.aryaa.action.OPEN_EMERGENCY_RESPONSE" || hasEmergencyExtras
        if (isEmergency) {
            val eventId = intent.getStringExtra("sosEventId")
            val userName = intent.getStringExtra("userName")
            val userPhone = intent.getStringExtra("userPhone")
            val latitude = intent.getStringExtra("latitude")
            val longitude = intent.getStringExtra("longitude")
            val w3wAddress = intent.getStringExtra("w3wAddress")
            val triggeredAt = intent.getStringExtra("triggeredAt")
            val accuracy = intent.getStringExtra("accuracy")
            val tier = intent.getStringExtra("tier")

            android.util.Log.d("EMERGENCY_DEBUG", "Read launching intent extra values directly: " +
                "sosEventId=$eventId, userName=$userName, userPhone=$userPhone, " +
                "latitude=$latitude, longitude=$longitude, w3wAddress=$w3wAddress, " +
                "triggeredAt=$triggeredAt, accuracy=$accuracy, tier=$tier")

            val sosData = com.as307.aryaa.ui.screens.emergency.EmergencySosData(
                sosEventId = eventId ?: "",
                userName = userName ?: "Unknown",
                userPhone = userPhone ?: "",
                latitude = latitude?.toDoubleOrNull(),
                longitude = longitude?.toDoubleOrNull(),
                w3wAddress = w3wAddress,
                triggeredAt = triggeredAt ?: "",
                accuracy = accuracy?.toDoubleOrNull(),
                tier = tier ?: "FAMILY"
            )
            android.util.Log.d("EMERGENCY_DEBUG", "Extracted SosData: $sosData")
            emergencyStateHolder.setActive(sosData)
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Triple-press volume button trigger logic.
        // We choose KEYCODE_VOLUME_DOWN rather than KEYCODE_VOLUME_UP because
        // volume-down is more natural to reach in a panic grip (phone face-down in palm,
        // thumb or fingers reaching naturally for the lower physical button).
        if (isVolumeTriggerEnabled && keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeTriggerHandler.onPress()
            // If the handler fired, it already called triggerSosFromVolumeButtons().
            // We don't know synchronously whether it fired, so we always propagate
            // unless the SOS is already active. Since the handler clears on fire, the
            // list will be empty — consume the event after a trigger by checking size.
            return false // Let Android pass it to super; the trigger fires asynchronously
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Signals SosViewModel (via SosServiceManager.volumeTriggerRequests SharedFlow)
     * to begin the SOS trigger flow. All API calls, state transitions, and service
     * starts run through the ViewModel's onCountdownComplete() — identical to the
     * hold-countdown path. The Activity never touches the repository directly.
     */
    private fun triggerSosFromVolumeButtons() {
        sosServiceManager.emitVolumeRequest()
    }
}
