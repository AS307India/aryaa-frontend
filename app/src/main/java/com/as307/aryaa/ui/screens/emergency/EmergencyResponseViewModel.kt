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
                android.util.Log.d("EMERGENCY_DEBUG", "fetchPlaybook started for eventId: $eventId")
                val response = api.getPlaybook(eventId)
                android.util.Log.d("EMERGENCY_DEBUG", "getPlaybook response code: ${response.code()} body: ${response.body()}")
                if (response.isSuccessful) {
                    _playbookState.value = response.body()
                } else {
                    android.util.Log.e("EMERGENCY_DEBUG", "getPlaybook returned error code: ${response.code()} message: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("EMERGENCY_DEBUG", "Failed to fetch playbook: ${e.message}", e)
            }
        }
    }

    fun respondToSos(eventId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("EMERGENCY_DEBUG", "respondToSos started for eventId: $eventId")
                val response = api.respondToSos(eventId)
                android.util.Log.d("EMERGENCY_DEBUG", "respondToSos response code: ${response.code()} body: ${response.body()}")
                if (response.isSuccessful) {
                    _isResponding.value = true
                    fetchPlaybook(eventId)
                } else {
                    android.util.Log.e("EMERGENCY_DEBUG", "respondToSos returned error code: ${response.code()} message: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("EMERGENCY_DEBUG", "Failed to respond to SOS: ${e.message}", e)
            }
        }
    }

    fun dismiss() {
        emergencyStateHolder.clear()
    }
}