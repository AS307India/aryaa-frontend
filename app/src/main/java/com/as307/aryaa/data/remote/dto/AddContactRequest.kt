package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddContactRequest(
    val name: String,
    val phone: String,
    val relationship: String,
    val isNearby: String = "SOMETIMES"
)
