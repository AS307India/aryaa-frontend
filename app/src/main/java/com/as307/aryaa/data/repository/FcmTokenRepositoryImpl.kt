package com.as307.aryaa.data.repository

import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FcmTokenRepositoryImpl @Inject constructor(
    private val api: AryaaApi
) : FcmTokenRepository {

    override suspend fun registerToken(token: String): Result<Unit> {
        return try {
            val response = api.registerFcmToken(FcmTokenRequest(token))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register FCM token: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAndRegisterToken(): Result<Unit> {
        return try {
            val token = suspendCancellableCoroutine<String?> { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resume(null)
                    }
                }
            }
            if (token != null) {
                registerToken(token)
            } else {
                Result.failure(Exception("FCM token retrieval failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
