package com.as307.aryaa.data.repository

import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.SosCancelResponse
import com.as307.aryaa.data.remote.dto.SosHistoryItem
import com.as307.aryaa.data.remote.dto.SosResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class SosRepositoryImplTest {

    private lateinit var fakeApi: FakeAryaaSosApi
    private lateinit var repository: SosRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        fakeApi = FakeAryaaSosApi()
        repository = SosRepositoryImpl(fakeApi, json)
    }

    @Test
    fun triggerSos_success_returnsSuccessResult() = runTest {
        fakeApi.triggerResponse = Response.success(
            SosResponse("event-1", "ACTIVE", "2026-07-01T12:00:00Z", emptyList(), null, 12.5)
        )

        val result = repository.triggerSos(0.0, 0.0, null, 12.5)
        assertTrue(result.isSuccess)
        assertEquals("event-1", result.getOrNull()?.sosEventId)
    }

    @Test
    fun triggerSos_409AlreadyActive_returnsAlreadyActiveError() = runTest {
        val errorJson = """{"error":"Conflict","message":"You already have an active SOS event"}"""
        fakeApi.triggerResponse = Response.error(409, errorJson.toResponseBody(null))

        val result = repository.triggerSos(0.0, 0.0, null, 12.5)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SosError.AlreadyActive)
    }

    @Test
    fun cancelSos_403Forbidden_returnsForbiddenError() = runTest {
        val errorJson = """{"error":"Forbidden","message":"You do not own this SOS event"}"""
        fakeApi.cancelResponse = Response.error(403, errorJson.toResponseBody(null))

        val result = repository.cancelSos("event-1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SosError.Forbidden)
    }

    @Test
    fun cancelSos_404NotFound_returnsNotFoundError() = runTest {
        val errorJson = """{"error":"Not Found","message":"SOS event not found"}"""
        fakeApi.cancelResponse = Response.error(404, errorJson.toResponseBody(null))

        val result = repository.cancelSos("event-1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SosError.NotFound)
    }

    @Test
    fun cancelSos_401Unauthorized_returnsUnauthorizedError() = runTest {
        val errorJson = """{"error":"Unauthorized","message":"Invalid token"}"""
        fakeApi.cancelResponse = Response.error(401, errorJson.toResponseBody(null))

        val result = repository.cancelSos("event-1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SosError.Unauthorized)
    }

    // --- Fake Api ---

    class FakeAryaaSosApi : AryaaApi {
        var triggerResponse: Response<SosResponse>? = null
        var cancelResponse: Response<SosCancelResponse>? = null
        var historyResponse: Response<List<SosHistoryItem>> = Response.success(emptyList())

        override suspend fun signup(request: com.as307.aryaa.data.remote.dto.SignupRequest) = Response.error<com.as307.aryaa.data.remote.dto.AuthResponse>(500, "".toResponseBody(null))
        override suspend fun login(request: com.as307.aryaa.data.remote.dto.LoginRequest) = Response.error<com.as307.aryaa.data.remote.dto.AuthResponse>(500, "".toResponseBody(null))
        override suspend fun getContacts() = Response.success(emptyList<com.as307.aryaa.data.remote.dto.ContactDto>())
        override suspend fun addContact(request: com.as307.aryaa.data.remote.dto.AddContactRequest) = Response.error<com.as307.aryaa.data.remote.dto.ContactDto>(500, "".toResponseBody(null))
        override suspend fun deleteContact(id: String) = Response.success(Unit)

        override suspend fun triggerSos(request: com.as307.aryaa.data.remote.dto.SosTriggerRequest): Response<SosResponse> {
            return triggerResponse ?: throw IllegalStateException("Response not set")
        }

        override suspend fun cancelSos(request: com.as307.aryaa.data.remote.dto.SosCancelRequest): Response<SosCancelResponse> {
            return cancelResponse ?: throw IllegalStateException("Response not set")
        }

        override suspend fun duressCancel(request: com.as307.aryaa.data.remote.dto.SosCancelRequest): Response<SosCancelResponse> {
            return cancelResponse ?: throw IllegalStateException("Response not set")
        }

        override suspend fun getSosHistory(): Response<List<SosHistoryItem>> = historyResponse
        override suspend fun updateLocation(request: com.as307.aryaa.data.remote.dto.SosLocationUpdateRequest) =
            Response.success(Unit)
        override suspend fun registerFcmToken(request: com.as307.aryaa.data.remote.dto.FcmTokenRequest) =
            Response.success(Unit)
        override suspend fun startDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneStartRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.DeadZoneResponse("", ""))
        override suspend fun checkInDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.DeadZoneResponse("", ""))
        override suspend fun cancelDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.DeadZoneResponse("", ""))
        override suspend fun getDeadZoneStatus() =
            Response.success(com.as307.aryaa.data.remote.dto.DeadZoneStatusContainer(null))
    }
}
