package com.as307.aryaa.data.remote

import com.as307.aryaa.data.local.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val urlPath = originalRequest.url.encodedPath

        val requestBuilder = originalRequest.newBuilder()
        if (com.as307.aryaa.util.TestEnv.isUnderTest) {
            requestBuilder.header("X-Aryaa-Test", "true")
        }

        // Skip adding token for registration and login
        val request = if (urlPath.contains("/api/auth/register") || urlPath.contains("/api/auth/login")) {
            requestBuilder.build()
        } else {
            val token = runBlocking { tokenStorage.getToken() }
            if (!token.isNullOrBlank()) {
                requestBuilder
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                requestBuilder.build()
            }
        }

        val response = chain.proceed(request)

        if (response.code == 401 && !urlPath.contains("/api/auth/register") && !urlPath.contains("/api/auth/login")) {
            runBlocking {
                tokenStorage.clearToken()
            }
            SessionManager.triggerSessionExpired()
        }

        return response
    }
}
