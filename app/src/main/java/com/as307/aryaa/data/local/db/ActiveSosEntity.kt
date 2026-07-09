package com.as307.aryaa.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_sos")
data class ActiveSosEntity(
    @PrimaryKey val sosEventId: String,
    val triggeredAt: String,
    val w3wAddress: String?,
    val contactsJson: String,  // JSON-serialized List<SosContactSnapshot> via kotlinx.serialization
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Double? = null
)
