package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SosResponse(
    val sosEventId: String,
    val status: String,
    val triggeredAt: String,
    val contacts: List<SosContactSnapshot>,
    val w3wAddress: String? = null,
    val accuracy: Double? = null
)
