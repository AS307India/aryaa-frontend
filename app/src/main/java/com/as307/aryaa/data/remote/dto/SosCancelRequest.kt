package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SosCancelRequest(
    val sosEventId: String
)
