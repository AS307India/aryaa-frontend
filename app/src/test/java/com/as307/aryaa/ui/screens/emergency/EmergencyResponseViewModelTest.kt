package com.as307.aryaa.ui.screens.emergency

import org.junit.Assert.*
import org.junit.Test

class EmergencyResponseViewModelTest {

    @Test
    fun testActiveEmergencyState_propagatesCorrectly() {
        val stateHolder = EmergencyStateHolder()
        val viewModel = EmergencyResponseViewModel(stateHolder)

        assertNull(viewModel.activeEmergency.value)

        val emergencyData = EmergencySosData(
            sosEventId = "event-id-999",
            userName = "Bob",
            userPhone = "+919876543210",
            latitude = 12.9716,
            longitude = 77.5946,
            w3wAddress = "location.test.words",
            triggeredAt = "2026-07-08T12:00:00Z"
        )

        stateHolder.setActive(emergencyData)
        assertEquals(emergencyData, viewModel.activeEmergency.value)

        viewModel.dismiss()
        assertNull(viewModel.activeEmergency.value)
    }
}