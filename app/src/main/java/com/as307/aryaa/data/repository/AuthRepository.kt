package com.as307.aryaa.data.repository

import com.as307.aryaa.data.remote.dto.AuthResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signup(name: String, email: String, phone: String, password: String): Result<AuthResponse>
    suspend fun login(email: String, password: String): Result<AuthResponse>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
}
