package com.as307.aryaa.data.remote

import com.as307.aryaa.data.local.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private var storedToken: String? = "test_jwt_token"
    private var sessionExpiredFired = false

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val fakeStorage = object : TokenStorage {
            override suspend fun saveToken(token: String) { storedToken = token }
            override suspend fun getToken(): String? = storedToken
            override suspend fun clearToken() { storedToken = null }
            override suspend fun saveUserProfile(name: String, email: String, phone: String) {}
            override suspend fun getUserName(): String? = null
            override suspend fun getUserEmail(): String? = null
            override suspend fun getUserPhone(): String? = null
            override suspend fun clearUserProfile() {}
        }

        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(fakeStorage))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun authenticatedRequest_hasAuthorizationHeader() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val request = Request.Builder()
            .url(server.url("/api/contacts"))
            .build()
        client.newCall(request).execute()
        val recorded = server.takeRequest()
        assertEquals("Bearer test_jwt_token", recorded.getHeader("Authorization"))
    }

    @Test
    fun loginRequest_hasNoAuthorizationHeader() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val request = Request.Builder()
            .url(server.url("/api/auth/login"))
            .build()
        client.newCall(request).execute()
        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun registerRequest_hasNoAuthorizationHeader() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val request = Request.Builder()
            .url(server.url("/api/auth/register"))
            .build()
        client.newCall(request).execute()
        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun authenticatedRequest_401_clearsToken() {
        server.enqueue(MockResponse().setResponseCode(401))
        val request = Request.Builder()
            .url(server.url("/api/contacts"))
            .build()
        client.newCall(request).execute()
        // Token should be cleared after 401 on authenticated endpoint
        val token = runBlocking { storedToken }
        assertNull(token)
    }
}
