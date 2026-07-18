package com.as307.aryaa.data.repository

import com.as307.aryaa.data.remote.dto.SosCancelResponse
import com.as307.aryaa.data.remote.dto.SosHistoryItem
import com.as307.aryaa.data.remote.dto.SosResponse

interface SosRepository {
    suspend fun triggerSos(lat: Double?, lng: Double?, address: String?, accuracy: Double?): Result<SosResponse>
    suspend fun cancelSos(sosEventId: String): Result<SosCancelResponse>
    suspend fun duressCancel(sosEventId: String): Result<SosCancelResponse>
    suspend fun getSosHistory(): Result<List<SosHistoryItem>>
    suspend fun sendLocationUpdate(sosEventId: String, lat: Double, lng: Double, timestamp: String): Result<Unit>
    suspend fun getActiveIncoming(): Result<com.as307.aryaa.data.remote.dto.ActiveIncomingSosResponse>
}
