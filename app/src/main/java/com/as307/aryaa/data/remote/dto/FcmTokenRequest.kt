package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenRequest(
    val fcmToken: String
)
