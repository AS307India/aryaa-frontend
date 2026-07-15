package com.as307.aryaa.ui.screens.emergency

data class EmergencySosData(
    val sosEventId: String,
    val userName: String,
    val userPhone: String,
    val latitude: Double?,
    val longitude: Double?,
    val w3wAddress: String?,
    val triggeredAt: String,
    val accuracy: Double? = null,
    val tier: String = "FAMILY"
)