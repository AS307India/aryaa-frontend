package com.as307.aryaa.ui.screens.emergency

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyStateHolder @Inject constructor() {
    private val _activeEmergency = MutableStateFlow<EmergencySosData?>(null)
    val activeEmergency: StateFlow<EmergencySosData?> = _activeEmergency.asStateFlow()

    fun setActive(data: EmergencySosData) {
        _activeEmergency.value = data
    }

    fun clear() {
        _activeEmergency.value = null
    }
}