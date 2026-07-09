package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SosLocationUpdateRequest(
    val sosEventId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)
