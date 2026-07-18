package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationShareStartRequest(
    val durationMinutes: Int,
    val contactIds: List<String>
)

@Serializable
data class LocationShareStartResponse(
    val sessionId: String,
    val shareToken: String,
    val shareUrl: String
)

@Serializable
data class LocationShareUpdateRequest(
    val lat: Double,
    val lng: Double,
    val accuracy: Double?,
    val timestamp: String
)
