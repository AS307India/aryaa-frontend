package com.as307.aryaa.data.repository

interface FcmTokenRepository {
    suspend fun registerToken(token: String): Result<Unit>
    suspend fun fetchAndRegisterToken(): Result<Unit>
}
