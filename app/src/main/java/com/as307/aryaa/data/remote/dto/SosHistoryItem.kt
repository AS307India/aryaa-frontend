package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SosHistoryItem(
    val id: String,
    val status: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val w3wAddress: String? = null,
    val triggeredAt: String,
    val cancelledAt: String? = null,
    val contacts: List<SosContactSnapshot>,
    val accuracy: Double? = null
)
