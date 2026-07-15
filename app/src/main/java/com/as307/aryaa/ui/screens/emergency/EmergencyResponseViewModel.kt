package com.as307.aryaa.ui.screens.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.PlaybookDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyResponseViewModel @Inject constructor(
    private val emergencyStateHolder: EmergencyStateHolder,
    private val api: AryaaApi
) : ViewModel() {

    val activeEmergency: StateFlow<EmergencySosData?> = emergencyStateHolder.activeEmergency

    private val _playbookState = MutableStateFlow<PlaybookDto?>(null)
    val playbookState: StateFlow<PlaybookDto?> = _playbookState.asStateFlow()

    private val _isResponding = MutableStateFlow(false)
    val isResponding: StateFlow<Boolean> = _isResponding.asStateFlow()

    init {
        viewModelScope.launch {
            activeEmergency.collect { emergency ->
                if (emergency != null) {
                    fetchPlaybook(emergency.sosEventId)
                } else {
                    _playbookState.value = null
                    _isResponding.value = false
                }
            }
        }
    }

    fun fetchPlaybook(eventId: String) {
        viewModelScope.launch {
            try {
                val response = api.getPlaybook(eventId)
                if (response.isSuccessful) {
                    _playbookState.value = response.body()
                    
                    // Check if self is already in responders list (e.g. if we reopened the playbook)
                    // We can verify by looking at the responses list, but since we don't have
                    // the current user's phone directly in the viewmodel easily without more DI,
                    // we can just let it check or let the button reflect when clicked.
                }
            } catch (e: Exception) {
                android.util.Log.e("PLAYBOOK_VM", "Failed to fetch playbook: ${e.message}")
            }
        }
    }

    fun respondToSos(eventId: String) {
        viewModelScope.launch {
            try {
                val response = api.respondToSos(eventId)
                if (response.isSuccessful) {
                    _isResponding.value = true
                    fetchPlaybook(eventId)
                }
            } catch (e: Exception) {
                android.util.Log.e("PLAYBOOK_VM", "Failed to respond to SOS: ${e.message}")
            }
        }
    }

    fun dismiss() {
        emergencyStateHolder.clear()
    }
}