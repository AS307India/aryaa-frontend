package com.as307.aryaa.ui.screens.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.ui.screens.sos.SosUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

import com.as307.aryaa.data.local.TokenStorage

@HiltViewModel
class PracticeSosViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val locationProvider: LocationProvider,
    private val profilePreferences: com.as307.aryaa.data.local.ProfilePreferences,
    private val tokenStorage: TokenStorage,
    private val api: AryaaApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<SosUiState>(SosUiState.Idle)
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private val _contactCount = MutableStateFlow(0)
    val contactCount: StateFlow<Int> = _contactCount.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _simulatedContacts = MutableStateFlow<List<ContactDto>>(emptyList())
    val simulatedContacts: StateFlow<List<ContactDto>> = _simulatedContacts.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<PracticeNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<PracticeNavigationEvent> = _navigationEvents.asSharedFlow()

    private var countdownJob: Job? = null
    private var holdDurationSeconds = 0

    sealed class PracticeNavigationEvent {
        data class NavigateToSummary(
            val holdDuration: Int,
            val contactsCount: Int,
            val accuracy: Int,
            val duressPracticed: Boolean
        ) : PracticeNavigationEvent()
    }

    init {
        viewModelScope.launch {
            _userName.value = tokenStorage.getUserName() ?: "A user"
        }
        viewModelScope.launch {
            contactsRepository.getContacts().collect { contactsList ->
                _contactCount.value = contactsList.size
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
            holdDurationSeconds = duration
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
            // Get user's actual location using the real location provider (for authenticity)
            val location = locationProvider.getLastKnownLocation()

            // Fetch real contacts directly from the backend to get registration hasFcmToken field
            var contactsList: List<ContactDto> = emptyList()
            try {
                val response = api.getContacts()
                if (response.isSuccessful) {
                    contactsList = response.body() ?: emptyList()
                } else {
                    contactsList = contactsRepository.getContacts().first()
                }
            } catch (e: Exception) {
                try {
                    contactsList = contactsRepository.getContacts().first()
                } catch (_: Exception) {}
            }
            _simulatedContacts.value = contactsList

            // Convert to snapshot entities
            val snapshots = contactsList.map {
                SosContactSnapshot(
                    name = it.name,
                    phone = it.phone
                )
            }

            val fakeEventId = "practice-" + UUID.randomUUID().toString().take(8)

            _uiState.value = SosUiState.Active(
                sosEventId = fakeEventId,
                triggeredAt = java.time.Instant.now().toString(),
                contacts = snapshots,
                w3wAddress = "///practice.mode.only",
                accuracy = location?.accuracy?.toDouble() ?: 10.0,
                latitude = location?.latitude ?: 18.5204,
                longitude = location?.longitude ?: 73.8567
            )
        }
    }

    fun onCancelSos() {
        val state = _uiState.value
        if (state !is SosUiState.Active) return

        _uiState.value = SosUiState.Cancelling
        viewModelScope.launch {
            val duration = holdDurationSeconds
            val count = _contactCount.value
            val accuracy = (state.accuracy ?: 10.0).toInt()
            
            _uiState.value = SosUiState.Idle
            _navigationEvents.tryEmit(
                PracticeNavigationEvent.NavigateToSummary(
                    holdDuration = duration,
                    contactsCount = count,
                    accuracy = accuracy,
                    duressPracticed = false
                )
            )
        }
    }

    fun onDuressCancel() {
        val state = _uiState.value
        if (state !is SosUiState.Active) return

        _uiState.value = SosUiState.Cancelling
        viewModelScope.launch {
            // Log internally (debug only)
            com.as307.aryaa.util.TestEnv.logDebug("PRACTICE_MODE", "Duress gesture practiced correctly")
            
            val duration = holdDurationSeconds
            val count = _contactCount.value
            val accuracy = (state.accuracy ?: 10.0).toInt()
            
            _uiState.value = SosUiState.Idle
            _navigationEvents.tryEmit(
                PracticeNavigationEvent.NavigateToSummary(
                    holdDuration = duration,
                    contactsCount = count,
                    accuracy = accuracy,
                    duressPracticed = true
                )
            )
        }
    }
}
