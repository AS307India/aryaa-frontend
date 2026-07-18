package com.as307.aryaa.data.remote.dto

data class ActiveIncomingSosResponse(
    val hasActiveIncoming: Boolean,
    val eventId: String? = null,
    val victimName: String? = null,
    val tier: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val w3w: String? = null,
    val accuracy: Double? = null,
    val triggeredAt: String? = null
)
