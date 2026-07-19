package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NearbyServiceResponse(
    val name: String,
    val lat: Double,
    val lng: Double,
    val distanceMeters: Int,
    val phone: String? = null,
    val address: String? = null
)
