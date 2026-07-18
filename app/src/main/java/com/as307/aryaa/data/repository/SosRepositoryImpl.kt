package com.as307.aryaa.data.repository

import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.ApiError
import com.as307.aryaa.data.remote.dto.SosCancelRequest
import com.as307.aryaa.data.remote.dto.SosCancelResponse
import com.as307.aryaa.data.remote.dto.SosHistoryItem
import com.as307.aryaa.data.remote.dto.SosResponse
import com.as307.aryaa.data.remote.dto.SosTriggerRequest
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosRepositoryImpl @Inject constructor(
    private val api: AryaaApi,
    private val json: Json
) : SosRepository {

    override suspend fun triggerSos(
        lat: Double?,
        lng: Double?,
        address: String?,
        accuracy: Double?
    ): Result<SosResponse> {
        return safeCall { api.triggerSos(SosTriggerRequest(lat, lng, address, accuracy)) }
    }

    override suspend fun cancelSos(sosEventId: String): Result<SosCancelResponse> {
        return safeCall { api.cancelSos(SosCancelRequest(sosEventId)) }
    }

    override suspend fun duressCancel(sosEventId: String): Result<SosCancelResponse> {
        return safeCall { api.duressCancel(SosCancelRequest(sosEventId)) }
    }

    override suspend fun getSosHistory(): Result<List<SosHistoryItem>> {
        return safeCall { api.getSosHistory() }
    }

    override suspend fun sendLocationUpdate(
        sosEventId: String,
        lat: Double,
        lng: Double,
        timestamp: String
    ): Result<Unit> {
        return safeCall {
            api.updateLocation(
                com.as307.aryaa.data.remote.dto.SosLocationUpdateRequest(
                    sosEventId = sosEventId,
                    latitude = lat,
                    longitude = lng,
                    timestamp = timestamp
                )
            )
        }
    }

    override suspend fun getActiveIncoming(): Result<com.as307.aryaa.data.remote.dto.ActiveIncomingSosResponse> {
        return safeCall { api.getActiveIncoming() }
    }

    private suspend fun <T> safeCall(
        call: suspend () -> Response<T>
    ): Result<T> = try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.success(body)
            } else {
                Result.failure(SosError.ServerError("Empty response from server"))
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val parsed = errorBody?.let {
                try { json.decodeFromString<ApiError>(it) } catch (_: Exception) { null }
            }
            val error = when (response.code()) {
                401 -> SosError.Unauthorized
                403 -> SosError.Forbidden
                404 -> SosError.NotFound
                409 -> SosError.AlreadyActive
                else -> SosError.ServerError(parsed?.message ?: "Server error: ${response.code()}")
            }
            Result.failure(error)
        }
    } catch (e: IOException) {
        Result.failure(SosError.NetworkError)
    } catch (e: Exception) {
        Result.failure(SosError.UnknownError(e.message ?: "Unknown error"))
    }
}
