package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ContactDto(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val userId: String,
    val createdAt: String,
    val updatedAt: String,
    val hasFcmToken: Boolean = false
)
