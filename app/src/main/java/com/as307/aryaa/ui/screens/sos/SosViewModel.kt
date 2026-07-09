package com.as307.aryaa.ui.screens.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.data.repository.SosError
import com.as307.aryaa.data.repository.SosRepository
import com.as307.aryaa.service.SosServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.as307.aryaa.data.local.db.ActiveSosDao
import com.as307.aryaa.data.local.db.ActiveSosEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString


sealed class SosUiState {
    object Idle : SosUiState()
    object Holding : SosUiState() // Defined in spec, can be used or transitioned through
    data class Countdown(val secondsLeft: Int) : SosUiState()
    object Triggering : SosUiState()
    data class Active(
        val sosEventId: String,
        val triggeredAt: String,
        val contacts: List<SosContactSnapshot>,
        val w3wAddress: String? = null,
        val accuracy: Double? = null
    ) : SosUiState()
    object Cancelling : SosUiState()
    data class Cancelled(val at: String) : SosUiState()
    data class Error(val error: SosError) : SosUiState()
}

@HiltViewModel
class SosViewModel @Inject constructor(
    private val sosRepository: SosRepository,
    private val contactsRepository: ContactsRepository,
    private val locationProvider: LocationProvider,
    private val sosServiceManager: SosServiceManager,
    private val activeSosDao: ActiveSosDao,
    private val profilePreferences: com.as307.aryaa.data.local.ProfilePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<SosUiState>(SosUiState.Idle)
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private val _contactCount = MutableStateFlow(0)
    val contactCount: StateFlow<Int> = _contactCount.asStateFlow()

    private var countdownJob: Job? = null

    init {
        com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "init started")
        // Collect contact count to display safety warning if zero contacts are added
        viewModelScope.launch {
            contactsRepository.getContacts().collect { contactsList ->
                _contactCount.value = contactsList.size
            }
        }

        // Restore active state from local database if available.
        // Validates against the backend before restoring — prevents ghost
        // Active state from stale Room data when SOS was cancelled externally
        // (another device, notification action, process restart).
        viewModelScope.launch {
            val persisted = activeSosDao.getActiveSos()
            val historyResult = sosRepository.getSosHistory()
            
            if (historyResult.isSuccess) {
                val history = historyResult.getOrNull()
                val activeBackendEvent = history?.find { it.status == "ACTIVE" }
                
                if (activeBackendEvent != null) {
                    // Backend has an active SOS event — restore/sync locally
                    val contacts = activeBackendEvent.contacts
                    val triggeredAt = activeBackendEvent.triggeredAt
                    val sosEventId = activeBackendEvent.id
                    val w3wAddress = activeBackendEvent.w3wAddress
                    val accuracy = activeBackendEvent.accuracy

                    try {
                        val entity = ActiveSosEntity(
                            sosEventId = sosEventId,
                            triggeredAt = triggeredAt,
                            w3wAddress = w3wAddress,
                            contactsJson = Json.encodeToString(contacts),
                            latitude = persisted?.latitude,
                            longitude = persisted?.longitude,
                            accuracy = accuracy
                        )
                        activeSosDao.insertActiveSos(entity)
                    } catch (e: Exception) {
                        com.as307.aryaa.util.TestEnv.logError("SosViewModel", "Failed to sync active SOS to Room", e)
                    }

                     _uiState.value = SosUiState.Active(
                         sosEventId = sosEventId,
                         triggeredAt = triggeredAt,
                         contacts = contacts,
                         w3wAddress = w3wAddress,
                         accuracy = accuracy
                     )
                    com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "startSos CALLED")
                    sosServiceManager.startSos(
                        sosEventId, contacts, w3wAddress
                    )
                } else {
                    // No active SOS on the backend
                    if (persisted != null) {
                        try {
                            com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "clearing Room")
                            activeSosDao.clearActiveSos()
                        } catch (e: Exception) {
                            com.as307.aryaa.util.TestEnv.logError("SosViewModel", "Failed to clear stale active SOS", e)
                        }
                    }
                    _uiState.value = SosUiState.Idle
                }
            } else {
                // Network error — fall back to local database status
                if (persisted != null) {
                    val contacts = try {
                        Json.decodeFromString<List<SosContactSnapshot>>(persisted.contactsJson)
                    } catch (e: Exception) {
                        emptyList()
                    }
                     _uiState.value = SosUiState.Active(
                         sosEventId = persisted.sosEventId,
                         triggeredAt = persisted.triggeredAt,
                         contacts = contacts,
                         w3wAddress = persisted.w3wAddress,
                         accuracy = persisted.accuracy
                     )
                    com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "startSos CALLED")
                    sosServiceManager.startSos(
                        persisted.sosEventId, contacts, persisted.w3wAddress
                    )
                } else {
                    _uiState.value = SosUiState.Idle
                }
            }
        }


        // Observe volume-button triple-press requests from MainActivity.
        // Routing through the ViewModel keeps the state machine as the single
        // source of truth — the volume path and the hold path are identical
        // from this point onwards.
        viewModelScope.launch {
            sosServiceManager.volumeTriggerRequests.collect {
                onCountdownComplete()
            }
        }

        // Observe cancellation events fired by SosService (e.g. when the user
        // taps "I'm Safe" in the notification). Reset UI state to Idle and
        // clear the persisted event so a reboot doesn't re-trigger it.
        viewModelScope.launch {
            sosServiceManager.sosCancelledEvents.collect {
                try {
                    com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "clearing Room")
                    activeSosDao.clearActiveSos()
                } catch (e: Exception) {
                    com.as307.aryaa.util.TestEnv.logError("SosViewModel", "Failed to clear active SOS on external cancel", e)
                }
                _uiState.value = SosUiState.Idle
            }
        }
    }

    fun onHoldStart() {
        if (_uiState.value !is SosUiState.Idle && _uiState.value !is SosUiState.Cancelled) return

        if (com.as307.aryaa.util.TestEnv.isUnderTest) {
            _uiState.value = SosUiState.Countdown(3)
            onCountdownComplete()
            return
        }

        _uiState.value = SosUiState.Holding
        countdownJob = viewModelScope.launch {
            val duration = profilePreferences.getSosHoldDuration()
            for (i in duration downTo 1) {
                _uiState.value = SosUiState.Countdown(i)
                delay(1000)
            }
            onCountdownComplete()
        }
    }

    fun onHoldRelease() {
        val currentState = _uiState.value
        if (currentState is SosUiState.Countdown || currentState is SosUiState.Holding) {
            countdownJob?.cancel()
            countdownJob = null
            _uiState.value = SosUiState.Idle
        }
    }

    fun onCountdownComplete() {
        if (_uiState.value is SosUiState.Active || _uiState.value is SosUiState.Triggering) return
        _uiState.value = SosUiState.Triggering
        viewModelScope.launch {
            // Best effort location (5 second timeout inside LocationProvider)
            val location = locationProvider.getLastKnownLocation()
            
            com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "TRIGGER API CALLED from: " + Thread.currentThread().stackTrace[2])
            sosRepository.triggerSos(
                lat = location?.latitude,
                lng = location?.longitude,
                address = null, // reverse geocoding not required this unit
                accuracy = location?.accuracy?.toDouble()
            ).onSuccess { response ->
                try {
                    val entity = ActiveSosEntity(
                        sosEventId = response.sosEventId,
                        triggeredAt = response.triggeredAt,
                        w3wAddress = response.w3wAddress,
                        contactsJson = Json.encodeToString(response.contacts),
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        accuracy = location?.accuracy?.toDouble()
                    )
                    activeSosDao.insertActiveSos(entity)
                } catch (e: Exception) {
                    com.as307.aryaa.util.TestEnv.logError("SosViewModel", "Failed to save active SOS to local database", e)
                }

                _uiState.value = SosUiState.Active(
                    sosEventId = response.sosEventId,
                    triggeredAt = response.triggeredAt,
                    contacts = response.contacts,
                    w3wAddress = response.w3wAddress,
                    accuracy = location?.accuracy?.toDouble()
                )
                com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "startSos CALLED")
                sosServiceManager.startSos(response.sosEventId, response.contacts, response.w3wAddress)
            }.onFailure { error ->
                val mappedError = if (error is SosError) error else SosError.UnknownError(error.message ?: "Failed to trigger SOS")
                _uiState.value = SosUiState.Error(mappedError)
            }
        }
    }

    fun onCancelSos() {
        val state = _uiState.value
        if (state !is SosUiState.Active) return

        _uiState.value = SosUiState.Cancelling
        viewModelScope.launch {
            sosServiceManager.cancelSos()
            sosRepository.cancelSos(state.sosEventId).onSuccess { response ->
                try {
                    com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "clearing Room")
                    activeSosDao.clearActiveSos()
                } catch (e: Exception) {
                    com.as307.aryaa.util.TestEnv.logError("SosViewModel", "Failed to clear active SOS from local database", e)
                }
                _uiState.value = SosUiState.Cancelled(response.cancelledAt)
            }.onFailure { error ->
                if (error is SosError.ServerError && error.message?.contains("Only active") == true) {
                    try {
                        com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "clearing Room")
                        activeSosDao.clearActiveSos()
                    } catch (e: Exception) {
                        // ignore
                    }
                    _uiState.value = SosUiState.Cancelled(java.time.Instant.now().toString())
                } else {
                    val mappedError = if (error is SosError) error else SosError.UnknownError(error.message ?: "Failed to cancel SOS")
                    _uiState.value = SosUiState.Error(mappedError)
                }
            }
        }
    }

    fun onDuressCancel() {
        val state = _uiState.value
        if (state !is SosUiState.Active) return

        // TODO(Unit 17): Practice Mode should intercept duress trigger and simulate without real backend calls

        _uiState.value = SosUiState.Cancelling
        viewModelScope.launch {
            sosServiceManager.duressCancel()
            sosRepository.duressCancel(state.sosEventId).onSuccess { response ->
                try {
                    com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "clearing Room")
                    activeSosDao.clearActiveSos()
                } catch (e: Exception) {
                    com.as307.aryaa.util.TestEnv.logError("SosViewModel", "Failed to clear active SOS from local database", e)
                }
                _uiState.value = SosUiState.Cancelled(response.cancelledAt)
            }.onFailure { error ->
                if (error is SosError.ServerError && error.message?.contains("Only active") == true) {
                    try {
                        com.as307.aryaa.util.TestEnv.logDebug("SOS_DEBUG", "clearing Room")
                        activeSosDao.clearActiveSos()
                    } catch (e: Exception) {
                        // ignore
                    }
                    _uiState.value = SosUiState.Cancelled(java.time.Instant.now().toString())
                } else {
                    val mappedError = if (error is SosError) error else SosError.UnknownError(error.message ?: "Failed to cancel SOS")
                    _uiState.value = SosUiState.Error(mappedError)
                }
            }
        }
    }


    fun onDismissError() {
        if (_uiState.value is SosUiState.Error) {
            _uiState.value = SosUiState.Idle
        }
    }
}
