package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileAddressRequest(
    val homeAddress: String? = null,
    val homeLatitude: Double? = null,
    val homeLongitude: Double? = null
)

@Serializable
data class SafetyReportRequest(
    val category: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val isPublicSpace: Boolean
)

@Serializable
data class SafetyMapPin(
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val reportCount: Int,
    val disputed: Boolean,
    val reportIds: List<String>,
    val categoryBreakdown: Map<String, Int>
)
