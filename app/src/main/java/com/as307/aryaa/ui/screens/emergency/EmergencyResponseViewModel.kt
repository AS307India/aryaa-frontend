package com.as307.aryaa.ui.screens.emergency

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EmergencyResponseViewModel @Inject constructor(
    private val emergencyStateHolder: EmergencyStateHolder
) : ViewModel() {

    val activeEmergency: StateFlow<EmergencySosData?> = emergencyStateHolder.activeEmergency

    fun dismiss() {
        emergencyStateHolder.clear()
    }
}