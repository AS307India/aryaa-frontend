package com.as307.aryaa.data.repository

import com.as307.aryaa.data.local.DeadZonePreferences
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest
import com.as307.aryaa.data.remote.dto.DeadZoneResponse
import com.as307.aryaa.data.remote.dto.DeadZoneStartRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadZoneRepositoryImpl @Inject constructor(
    private val api: AryaaApi,
    private val preferences: DeadZonePreferences
) : DeadZoneRepository {

    override suspend fun startDeadZone(
        durationMinutes: Int,
        latitude: Double?,
        longitude: Double?,
        accuracy: Double?,
        mode: String?,
        destination: String?,
        intervalMinutes: Int?
    ): Result<DeadZoneResponse> {
        return try {
            val request = DeadZoneStartRequest(
                durationMinutes = durationMinutes,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                mode = mode,
                destination = destination,
                intervalMinutes = intervalMinutes
            )
            android.util.Log.d("DEADZONE_DEBUG",
                "startDeadZone request: durationMinutes=$durationMinutes lat=$latitude lng=$longitude accuracy=$accuracy mode=$mode")
            val response = api.startDeadZone(request)
            android.util.Log.d("DEADZONE_DEBUG",
                "startDeadZone response: code=${response.code()} message='${response.message()}'")
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                android.util.Log.d("DEADZONE_DEBUG",
                    "startDeadZone response: gracePeriodEnd=${data.gracePeriodEnd} for durationMinutes=$durationMinutes")
                preferences.setActiveCheckInId(data.checkInId)
                preferences.setExpectedBackAt(data.expectedBackAt)
                preferences.setGracePeriodEnd(data.gracePeriodEnd)
                preferences.setMode(data.mode)
                preferences.setDestination(data.destination)
                preferences.setIntervalMinutes(data.intervalMinutes)
                preferences.setLocationShareSessionId(data.locationShareSessionId)
                Result.success(data)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.w("DEADZONE_DEBUG",
                    "startDeadZone failed: code=${response.code()} errorBody=$errorBody")
                Result.failure(Exception(
                    "Failed to start Dead Zone session: HTTP ${response.code()} - $errorBody"
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_DEBUG", "startDeadZone exception", e)
            Result.failure(e)
        }
    }

    override suspend fun checkIn(checkInId: String): Result<DeadZoneResponse> {
        return try {
            val response = api.checkInDeadZone(DeadZoneCheckInRequest(checkInId))
            android.util.Log.d("DEADZONE_DEBUG",
                "checkIn response: code=${response.code()} message='${response.message()}'")
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                if (data.status == "PENDING") {
                    // Heartbeat ping success: update new interval timers
                    preferences.setExpectedBackAt(data.expectedBackAt)
                    preferences.setGracePeriodEnd(data.gracePeriodEnd)
                } else {
                    // Standard session finished
                    preferences.clear()
                }
                Result.success(data)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.w("DEADZONE_DEBUG",
                    "checkIn failed: code=${response.code()} errorBody=$errorBody")
                Result.failure(Exception(
                    "Failed to check in: HTTP ${response.code()} - $errorBody"
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_DEBUG", "checkIn exception", e)
            Result.failure(e)
        }
    }

    override suspend fun cancel(checkInId: String): Result<DeadZoneResponse> {
        return try {
            val response = api.cancelDeadZone(DeadZoneCheckInRequest(checkInId))
            android.util.Log.d("DEADZONE_DEBUG",
                "cancel response: code=${response.code()} message='${response.message()}'")
            if (response.isSuccessful && response.body() != null) {
                preferences.clear()
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.w("DEADZONE_DEBUG",
                    "cancel failed: code=${response.code()} errorBody=$errorBody")
                Result.failure(Exception(
                    "Failed to cancel session: HTTP ${response.code()} - $errorBody"
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_DEBUG", "cancel exception", e)
            Result.failure(e)
        }
    }

    override suspend fun getStatus(): Result<DeadZoneResponse?> {
        return try {
            val response = api.getDeadZoneStatus()
            val container = response.body()
            android.util.Log.d("DEADZONE_DEBUG",
                "getStatus: code=${response.code()} container=$container")
            if (response.isSuccessful && container != null) {
                val checkIn = container.checkIn
                if (checkIn == null) {
                    preferences.clear()
                } else {
                    preferences.setActiveCheckInId(checkIn.checkInId)
                    preferences.setExpectedBackAt(checkIn.expectedBackAt)
                    preferences.setGracePeriodEnd(checkIn.gracePeriodEnd)
                    preferences.setMode(checkIn.mode)
                    preferences.setDestination(checkIn.destination)
                    preferences.setIntervalMinutes(checkIn.intervalMinutes)
                    preferences.setLocationShareSessionId(checkIn.locationShareSessionId)
                }
                Result.success(checkIn)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.w("DEADZONE_DEBUG",
                    "getStatus unsuccessful: code=${response.code()} errorBody=$errorBody")
                Result.failure(Exception(
                    "Failed to get check-in status: HTTP ${response.code()} - $errorBody"
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_DEBUG", "getStatus exception", e)
            Result.failure(e)
        }
    }
}
