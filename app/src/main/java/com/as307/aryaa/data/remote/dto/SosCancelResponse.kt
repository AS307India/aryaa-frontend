package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SosCancelResponse(
    val sosEventId: String,
    val status: String,
    val cancelledAt: String
)
