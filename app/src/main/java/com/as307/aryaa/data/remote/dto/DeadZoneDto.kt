package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeadZoneStartRequest(
    val durationMinutes: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Double? = null
)

@Serializable
data class DeadZoneCheckInRequest(
    val checkInId: String
)

@Serializable
data class DeadZoneResponse(
    val checkInId: String,
    val status: String,
    val startedAt: String? = null,
    val expectedBackAt: String? = null,
    val gracePeriodEnd: String? = null,
    val checkedInAt: String? = null
)

@Serializable
data class DeadZoneStatusContainer(
    val checkIn: DeadZoneResponse?
)
