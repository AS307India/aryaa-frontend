package com.as307.aryaa.ui.screens.deadzone

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.local.DeadZonePreferences
import com.as307.aryaa.data.repository.DeadZoneRepository
import com.as307.aryaa.service.work.DeadZoneCheckInWorker
import com.as307.aryaa.service.work.DeadZoneReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class DeadZoneUiState {
    object Idle : DeadZoneUiState()
    object SettingDuration : DeadZoneUiState()
    data class Pending(
        val checkInId: String,
        val expectedBackAt: String,
        val gracePeriodEnd: String,
        val mode: String = "PLAIN",
        val destination: String? = null,
        val intervalMinutes: Int? = null,
        val locationShareSessionId: String? = null
    ) : DeadZoneUiState()
    object CheckingIn : DeadZoneUiState()
    object Error : DeadZoneUiState()
}

@HiltViewModel
class DeadZoneViewModel @Inject constructor(
    private val deadZoneRepository: DeadZoneRepository,
    private val locationProvider: LocationProvider,
    private val preferences: DeadZonePreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeadZoneUiState>(DeadZoneUiState.Idle)
    val uiState: StateFlow<DeadZoneUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Restore from DataStore cache immediately for responsive UI
            val id       = preferences.getActiveCheckInId()
            val expected = preferences.getExpectedBackAt()
            val grace    = preferences.getGracePeriodEnd()
            val mode     = preferences.getMode() ?: "PLAIN"
            val dest     = preferences.getDestination()
            val interval = preferences.getIntervalMinutes()
            val shareId  = preferences.getLocationShareSessionId()

            if (id != null && expected != null) {
                android.util.Log.d("DEADZONE_DEBUG",
                    "Restored Pending from cache: id=$id expected=$expected grace=$grace mode=$mode")
                _uiState.value = DeadZoneUiState.Pending(id, expected, grace ?: "", mode, dest, interval, shareId)
            } else {
                android.util.Log.d("DEADZONE_DEBUG", "No cached check-in found, starting Idle")
            }

            // Sync with backend status to resolve stale check-ins
            syncStatusWithBackend()
        }
    }

    private suspend fun syncStatusWithBackend() {
        deadZoneRepository.getStatus().onSuccess { checkIn ->
            if (checkIn != null) {
                android.util.Log.d("DEADZONE_DEBUG", "Backend confirms active checkIn: id=${checkIn.checkInId}")
                _uiState.value = DeadZoneUiState.Pending(
                    checkInId = checkIn.checkInId,
                    expectedBackAt = checkIn.expectedBackAt!!,
                    gracePeriodEnd = checkIn.gracePeriodEnd!!,
                    mode = checkIn.mode ?: "PLAIN",
                    destination = checkIn.destination,
                    intervalMinutes = checkIn.intervalMinutes,
                    locationShareSessionId = checkIn.locationShareSessionId
                )
            } else {
                android.util.Log.d("DEADZONE_DEBUG", "Backend reports no active checkIn -> Idle")
                _uiState.value = DeadZoneUiState.Idle
                cancelWorkManagerJobs()
            }
        }.onFailure { error ->
            android.util.Log.e("DEADZONE_DEBUG", "syncStatusWithBackend() failed", error)
        }
    }

    fun startCheckIn(
        durationMinutes: Int,
        mode: String = "PLAIN",
        destination: String? = null,
        intervalMinutes: Int? = null
    ) {
        _uiState.value = DeadZoneUiState.CheckingIn
        viewModelScope.launch {
            val location = try {
                locationProvider.getLastKnownLocation()
            } catch (_: Exception) {
                null
            }

            deadZoneRepository.startDeadZone(
                durationMinutes = durationMinutes,
                latitude = location?.latitude,
                longitude = location?.longitude,
                accuracy = location?.accuracy?.toDouble(),
                mode = mode,
                destination = destination,
                intervalMinutes = intervalMinutes
            ).onSuccess { response ->
                // Schedule WorkManager notification reminder (expectedBackAt) & SOS trigger check (gracePeriodEnd)
                scheduleWorkManagerJobs(
                    expectedBackAtIso = response.expectedBackAt!!,
                    gracePeriodEndIso = response.gracePeriodEnd!!,
                    checkInId = response.checkInId
                )

                // If mode is SAFE_WALK, trigger the foreground LocationShareService
                if (mode == "SAFE_WALK" && response.locationShareSessionId != null) {
                    startLocationShareService(response)
                }

                _uiState.value = DeadZoneUiState.Pending(
                    checkInId = response.checkInId,
                    expectedBackAt = response.expectedBackAt,
                    gracePeriodEnd = response.gracePeriodEnd,
                    mode = response.mode ?: "PLAIN",
                    destination = response.destination,
                    intervalMinutes = response.intervalMinutes,
                    locationShareSessionId = response.locationShareSessionId
                )
            }.onFailure { error ->
                android.util.Log.e("DEADZONE_DEBUG", "startCheckIn() failed", error)
                _uiState.value = DeadZoneUiState.Error
            }
        }
    }

    fun checkIn() {
        val currentState = _uiState.value
        if (currentState !is DeadZoneUiState.Pending) return
        val checkInId = currentState.checkInId

        _uiState.value = DeadZoneUiState.CheckingIn
        viewModelScope.launch {
            deadZoneRepository.checkIn(checkInId).onSuccess { response ->
                if (response.status == "PENDING") {
                    // Heartbeat ping success: update internal state with shifted timers, keeping loop running
                    scheduleWorkManagerJobs(
                        expectedBackAtIso = response.expectedBackAt!!,
                        gracePeriodEndIso = response.gracePeriodEnd!!,
                        checkInId = checkInId
                    )
                    _uiState.value = DeadZoneUiState.Pending(
                        checkInId = checkInId,
                        expectedBackAt = response.expectedBackAt,
                        gracePeriodEnd = response.gracePeriodEnd,
                        mode = currentState.mode,
                        destination = currentState.destination,
                        intervalMinutes = currentState.intervalMinutes,
                        locationShareSessionId = currentState.locationShareSessionId
                    )
                } else {
                    // Plain check-in / Safe walk arrival completed
                    cancelWorkManagerJobs()
                    stopLocationShareService()
                    _uiState.value = DeadZoneUiState.Idle
                }
            }.onFailure { error ->
                android.util.Log.e("DEADZONE_DEBUG", "checkIn() failed", error)
                _uiState.value = DeadZoneUiState.Error
            }
        }
    }

    fun cancelSession() {
        val currentState = _uiState.value
        if (currentState !is DeadZoneUiState.Pending) return
        val checkInId = currentState.checkInId

        _uiState.value = DeadZoneUiState.CheckingIn
        viewModelScope.launch {
            deadZoneRepository.cancel(checkInId).onSuccess {
                cancelWorkManagerJobs()
                stopLocationShareService()
                _uiState.value = DeadZoneUiState.Idle
            }.onFailure { error ->
                android.util.Log.e("DEADZONE_DEBUG", "cancelSession() failed", error)
                _uiState.value = DeadZoneUiState.Error
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = DeadZoneUiState.Idle
    }

    fun enterSettingDuration() {
        _uiState.value = DeadZoneUiState.SettingDuration
    }

    private fun scheduleWorkManagerJobs(expectedBackAtIso: String, gracePeriodEndIso: String, checkInId: String) {
        try {
            val wm = WorkManager.getInstance(context)

            // 1. Notification reminder ping at expectedBackAt
            val expectedMillis = Instant.parse(expectedBackAtIso).toEpochMilli()
            val reminderDelay = maxOf(0L, expectedMillis - System.currentTimeMillis())
            val reminderRequest = OneTimeWorkRequestBuilder<DeadZoneReminderWorker>()
                .setInitialDelay(reminderDelay, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(
                "deadzone_reminder_$checkInId",
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )

            // 2. Escalation SOS check scan at gracePeriodEnd
            val graceMillis = Instant.parse(gracePeriodEndIso).toEpochMilli()
            val escalationDelay = maxOf(0L, graceMillis - System.currentTimeMillis())
            val escalationRequest = OneTimeWorkRequestBuilder<DeadZoneCheckInWorker>()
                .setInitialDelay(escalationDelay, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(
                "deadzone_escalation_$checkInId",
                ExistingWorkPolicy.REPLACE,
                escalationRequest
            )

            android.util.Log.d("DEADZONE_WORK", "Scheduled check-in jobs for id: $checkInId (reminder in $reminderDelay ms, escalation in $escalationDelay ms)")
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_WORK", "Failed to schedule WorkManager jobs: " + e.message)
        }
    }

    private suspend fun cancelWorkManagerJobs() {
        try {
            val id = preferences.getActiveCheckInId()
            if (id != null) {
                val wm = WorkManager.getInstance(context)
                wm.cancelUniqueWork("deadzone_reminder_$id")
                wm.cancelUniqueWork("deadzone_escalation_$id")
            }
        } catch (_: Exception) {}
    }

    private fun startLocationShareService(response: com.as307.aryaa.data.remote.dto.DeadZoneResponse) {
        val sessionId = response.locationShareSessionId ?: return
        val shareUrl = response.shareUrl ?: ""
        val expiresAt = response.expectedBackAt ?: ""
        
        val intent = Intent(context, com.as307.aryaa.service.LocationShareService::class.java).apply {
            action = com.as307.aryaa.service.LocationShareService.ACTION_START_SHARE
            putExtra(com.as307.aryaa.service.LocationShareService.EXTRA_SESSION_ID, sessionId)
            putExtra(com.as307.aryaa.service.LocationShareService.EXTRA_SHARE_URL, shareUrl)
            putExtra(com.as307.aryaa.service.LocationShareService.EXTRA_EXPIRES_AT, expiresAt)
            putExtra(com.as307.aryaa.service.LocationShareService.EXTRA_CONTACT_COUNT, 1) // default fallback
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
            android.util.Log.d("DEADZONE_DEBUG", "Auto-started LocationShareService for SAFE_WALK session: $sessionId")
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_DEBUG", "Failed to start LocationShareService", e)
        }
    }

    private fun stopLocationShareService() {
        try {
            val intent = Intent(context, com.as307.aryaa.service.LocationShareService::class.java).apply {
                action = com.as307.aryaa.service.LocationShareService.ACTION_STOP_SHARE
            }
            context.startService(intent)
            android.util.Log.d("DEADZONE_DEBUG", "Auto-stopped LocationShareService")
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_DEBUG", "Failed to stop LocationShareService", e)
        }
    }
}
