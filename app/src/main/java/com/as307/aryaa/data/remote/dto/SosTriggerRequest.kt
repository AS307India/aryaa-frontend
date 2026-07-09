package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SosTriggerRequest(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val accuracy: Double? = null
)
