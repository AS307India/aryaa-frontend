package com.as307.aryaa.ui.screens.fakecall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.local.FakeCallPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FakeCallUiState {
    object Idle : FakeCallUiState()
    data class Ringing(val callerName: String) : FakeCallUiState()
    data class InCall(val callerName: String, val duration: String) : FakeCallUiState()
    object Ended : FakeCallUiState()
}

@HiltViewModel
class FakeCallViewModel @Inject constructor(
    private val preferences: FakeCallPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<FakeCallUiState>(FakeCallUiState.Idle)
    val uiState: StateFlow<FakeCallUiState> = _uiState.asStateFlow()

    private val _countdownState = MutableStateFlow(0)
    val countdownState: StateFlow<Int> = _countdownState.asStateFlow()

    private var countdownJob: Job? = null
    private var durationJob: Job? = null

    fun scheduleFakeCall() {
        countdownJob?.cancel()
        durationJob?.cancel()
        _uiState.value = FakeCallUiState.Idle

        countdownJob = viewModelScope.launch {
            val callerName = preferences.getCallerName()
            val delaySeconds = preferences.getCallerDelay()

            if (delaySeconds <= 0) {
                _uiState.value = FakeCallUiState.Ringing(callerName)
            } else {
                _countdownState.value = delaySeconds
                var countdown = delaySeconds
                while (countdown > 0) {
                    delay(1000)
                    countdown--
                    _countdownState.value = countdown
                }
                _uiState.value = FakeCallUiState.Ringing(callerName)
            }
        }
    }

    fun answerCall() {
        countdownJob?.cancel()
        val currentCallerName = when (val state = _uiState.value) {
            is FakeCallUiState.Ringing -> state.callerName
            else -> "Maa"
        }
        _uiState.value = FakeCallUiState.InCall(currentCallerName, "0:00")

        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            var seconds = 0
            while (true) {
                delay(1000)
                seconds++
                val mins = seconds / 60
                val secs = seconds % 60
                val durationStr = String.format("%d:%02d", mins, secs)
                _uiState.value = FakeCallUiState.InCall(currentCallerName, durationStr)
            }
        }
    }

    fun endCall() {
        countdownJob?.cancel()
        durationJob?.cancel()
        _uiState.value = FakeCallUiState.Ended
    }

    fun cancelScheduled() {
        countdownJob?.cancel()
        _uiState.value = FakeCallUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        durationJob?.cancel()
    }
}
