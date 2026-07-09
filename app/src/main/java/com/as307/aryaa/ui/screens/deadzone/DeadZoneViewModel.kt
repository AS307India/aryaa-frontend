package com.as307.aryaa.ui.screens.deadzone

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.local.DeadZonePreferences
import com.as307.aryaa.data.repository.DeadZoneRepository
import com.as307.aryaa.service.work.DeadZoneCheckInWorker
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
        val gracePeriodEnd: String
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
            if (id != null && expected != null) {
                android.util.Log.d("DEADZONE_DEBUG",
                    "Restored Pending from cache: id=$id expected=$expected grace=$grace")
                _uiState.value = DeadZoneUiState.Pending(id, expected, grace ?: "")
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
                _uiState.value = DeadZoneUiState.Pending(checkIn.checkInId, checkIn.expectedBackAt!!, checkIn.gracePeriodEnd!!)
            } else {
                android.util.Log.d("DEADZONE_DEBUG", "Backend reports no active checkIn -> Idle")
                _uiState.value = DeadZoneUiState.Idle
                cancelWorkManagerJob()
            }
        }.onFailure { error ->
            android.util.Log.e("DEADZONE_DEBUG", "syncStatusWithBackend() failed", error)
            // Keep local cached state if network fails
        }
    }

    fun startCheckIn(durationMinutes: Int) {
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
                accuracy = location?.accuracy?.toDouble()
            ).onSuccess { response ->
                // Schedule the WorkManager reminder
                scheduleWorkManagerReminder(response.gracePeriodEnd!!, response.checkInId)
                _uiState.value = DeadZoneUiState.Pending(response.checkInId, response.expectedBackAt!!, response.gracePeriodEnd!!)
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
            deadZoneRepository.checkIn(checkInId).onSuccess {
                cancelWorkManagerJob()
                _uiState.value = DeadZoneUiState.Idle
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
                cancelWorkManagerJob()
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

    private fun scheduleWorkManagerReminder(gracePeriodEndIso: String, checkInId: String) {
        try {
            val endMillis = Instant.parse(gracePeriodEndIso).toEpochMilli()
            val delayMs = maxOf(0L, endMillis - System.currentTimeMillis())

            val workRequest = OneTimeWorkRequestBuilder<DeadZoneCheckInWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "deadzone_reminder_$checkInId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            com.as307.aryaa.util.TestEnv.logDebug("DEADZONE_WORK", "Scheduled work deadzone_reminder_$checkInId with delay $delayMs ms")
        } catch (e: Exception) {
            com.as307.aryaa.util.TestEnv.logError("DEADZONE_WORK", "Failed to schedule WorkManager job: " + e.message)
        }
    }

    private suspend fun cancelWorkManagerJob() {
        try {
            val id = preferences.getActiveCheckInId()
            if (id != null) {
                WorkManager.getInstance(context).cancelUniqueWork("deadzone_reminder_$id")
            }
        } catch (_: Exception) {}
    }
}
