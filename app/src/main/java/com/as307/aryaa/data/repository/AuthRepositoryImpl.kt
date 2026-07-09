package com.as307.aryaa.data.repository

import com.as307.aryaa.data.local.TokenManager
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.ApiError
import com.as307.aryaa.data.remote.dto.AuthResponse
import com.as307.aryaa.data.remote.dto.LoginRequest
import com.as307.aryaa.data.remote.dto.SignupRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.as307.aryaa.data.local.TokenStorage
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AryaaApi,
    private val tokenManager: TokenStorage,
    private val json: Json,
    private val fcmTokenRepository: FcmTokenRepository
) : AuthRepository {

    private val _isLoggedInState = MutableStateFlow(false)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val token = tokenManager.getToken()
            _isLoggedInState.value = token != null
        }
    }

    override suspend fun signup(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<AuthResponse> {
        val request = SignupRequest(name = name, email = email, phone = phone, password = password)
        return safeApiCall { api.signup(request) }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<AuthResponse> {
        val request = LoginRequest(email = email, password = password)
        return safeApiCall { api.login(request) }
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        tokenManager.clearUserProfile()
        _isLoggedInState.value = false
    }

    override fun isLoggedIn(): Flow<Boolean> = _isLoggedInState.asStateFlow()

    private suspend fun safeApiCall(
        call: suspend () -> Response<AuthResponse>
    ): Result<AuthResponse> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    tokenManager.saveToken(body.token)
                    tokenManager.saveUserProfile(body.user.name, body.user.email, body.user.phone)
                    _isLoggedInState.value = true
                    // Asynchronously fetch and register FCM token
                    CoroutineScope(Dispatchers.IO).launch {
                        fcmTokenRepository.fetchAndRegisterToken()
                    }
                    Result.success(body)
                } else {
                    Result.failure(AuthError.ServerError("Empty response body from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val parsedError = errorBody?.let {
                    try {
                        json.decodeFromString<ApiError>(it)
                    } catch (e: Exception) {
                        null
                    }
                }

                val error = when (response.code()) {
                    401 -> AuthError.InvalidCredentials
                    400, 409 -> AuthError.ValidationError(parsedError?.message ?: "Validation failed")
                    else -> AuthError.ServerError(parsedError?.message ?: "Server returned error: ${response.code()}")
                }
                Result.failure(error)
            }
        } catch (e: IOException) {
            Result.failure(AuthError.NetworkError)
        } catch (e: Exception) {
            Result.failure(AuthError.UnknownError(e.message ?: "An unknown error occurred"))
        }
    }
}
