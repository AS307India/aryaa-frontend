package com.as307.aryaa.data.repository

import com.as307.aryaa.data.remote.dto.DeadZoneResponse

interface DeadZoneRepository {
    suspend fun startDeadZone(durationMinutes: Int, latitude: Double?, longitude: Double?, accuracy: Double?): Result<DeadZoneResponse>
    suspend fun checkIn(checkInId: String): Result<DeadZoneResponse>
    suspend fun cancel(checkInId: String): Result<DeadZoneResponse>
    suspend fun getStatus(): Result<DeadZoneResponse?>
}
