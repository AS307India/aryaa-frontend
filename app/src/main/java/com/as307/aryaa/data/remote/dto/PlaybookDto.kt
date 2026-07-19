package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaybookDto(
    val id: String,
    val victimName: String,
    val victimPhone: String,
    val status: String,
    val publicTrackUrl: String? = null,
    val latitude: Double?,
    val longitude: Double?,
    val w3wAddress: String?,
    val accuracy: Double?,
    val triggeredAt: String,
    val responders: List<PlaybookResponderDto>
)

@Serializable
data class PlaybookResponderDto(
    val phone: String,
    val name: String,
    val respondedAt: String
)
