package com.as307.aryaa.data.repository

import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.AuthResponse
import com.as307.aryaa.data.remote.dto.LoginRequest
import com.as307.aryaa.data.remote.dto.SignupRequest
import com.as307.aryaa.data.remote.dto.UserDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import io.mockk.mockk

class AuthRepositoryImplTest {

    private lateinit var fakeApi: FakeAryaaApi
    private lateinit var fakeTokenStorage: FakeTokenStorage
    private lateinit var repository: AuthRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        fakeApi = FakeAryaaApi()
        fakeTokenStorage = FakeTokenStorage()
        repository = AuthRepositoryImpl(fakeApi, fakeTokenStorage, json, mockk(relaxed = true))
    }

    @Test
    fun signup_success_savesToken_and_emitsIsLoggedIn() = runTest {
        val userDto = UserDto("1", "User", "user@test.com", "9876543210", "2026-06-30T12:00:00Z")
        fakeApi.signupResponse = Response.success(AuthResponse("token123", userDto))

        val result = repository.signup("User", "user@test.com", "9876543210", "password")

        assertTrue(result.isSuccess)
        assertEquals("token123", result.getOrNull()?.token)
        assertEquals("token123", fakeTokenStorage.getToken())
        assertTrue(repository.isLoggedIn().first())
    }

    @Test
    fun login_unauthorized_returnsInvalidCredentials() = runTest {
        val errorJson = """{"error":"Unauthorized","message":"Invalid email or password"}"""
        fakeApi.loginResponse = Response.error(401, errorJson.toResponseBody())

        val result = repository.login("user@test.com", "wrong")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AuthError.InvalidCredentials)
    }

    @Test
    fun login_validationError_returnsValidationErrorWithMessage() = runTest {
        val errorJson = """{"error":"Bad Request","message":"email: Invalid email format"}"""
        fakeApi.loginResponse = Response.error(400, errorJson.toResponseBody())

        val result = repository.login("user@test.com", "password")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AuthError.ValidationError)
        assertEquals("email: Invalid email format", exception?.message)
    }

    @Test
    fun login_timeout_returnsNetworkError() = runTest {
        fakeApi.shouldThrowIOException = true

        val result = repository.login("user@test.com", "password")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AuthError.NetworkError)
    }
}

class FakeTokenStorage : TokenStorage {
    private var token: String? = null
    private var name: String? = null
    private var email: String? = null
    private var phone: String? = null

    override suspend fun saveToken(token: String) { this.token = token }
    override suspend fun getToken(): String? = token
    override suspend fun clearToken() { token = null }
    override suspend fun saveUserProfile(name: String, email: String, phone: String) { this.name = name; this.email = email; this.phone = phone }
    override suspend fun getUserName(): String? = name
    override suspend fun getUserEmail(): String? = email
    override suspend fun getUserPhone(): String? = phone
    override suspend fun clearUserProfile() { name = null; email = null; phone = null }
}

class FakeAryaaApi : AryaaApi {
    var signupResponse: Response<AuthResponse>? = null
    var loginResponse: Response<AuthResponse>? = null
    var shouldThrowIOException = false

    override suspend fun signup(request: SignupRequest): Response<AuthResponse> {
        if (shouldThrowIOException) throw java.io.IOException("Fake network timeout")
        return signupResponse ?: throw IllegalStateException("Signup response not set")
    }

    override suspend fun login(request: LoginRequest): Response<AuthResponse> {
        if (shouldThrowIOException) throw java.io.IOException("Fake network timeout")
        return loginResponse ?: throw IllegalStateException("Login response not set")
    }

    override suspend fun getContacts() = Response.success(emptyList<com.as307.aryaa.data.remote.dto.ContactDto>())
    override suspend fun addContact(request: com.as307.aryaa.data.remote.dto.AddContactRequest) =
        Response.success(com.as307.aryaa.data.remote.dto.ContactDto("","","","","","",""))
    override suspend fun deleteContact(id: String) = Response.success(Unit)

    override suspend fun triggerSos(request: com.as307.aryaa.data.remote.dto.SosTriggerRequest) =
        Response.success(com.as307.aryaa.data.remote.dto.SosResponse("","","", emptyList()))
    override suspend fun cancelSos(request: com.as307.aryaa.data.remote.dto.SosCancelRequest) =
        Response.success(com.as307.aryaa.data.remote.dto.SosCancelResponse("","",""))
    override suspend fun duressCancel(request: com.as307.aryaa.data.remote.dto.SosCancelRequest) =
        Response.success(com.as307.aryaa.data.remote.dto.SosCancelResponse("","",""))
    override suspend fun getSosHistory() =
        Response.success(emptyList<com.as307.aryaa.data.remote.dto.SosHistoryItem>())
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
