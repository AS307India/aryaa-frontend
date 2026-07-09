package com.as307.aryaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SosContactSnapshot(
    val name: String,
    val phone: String
) : java.io.Serializable
