package com.as307.aryaa.data.repository

import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.FcmTokenRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class FcmTokenRepositoryTest {

    private lateinit var mockApi: AryaaApi
    private lateinit var repository: FcmTokenRepositoryImpl

    @Before
    fun setUp() {
        mockApi = mockk()
        repository = FcmTokenRepositoryImpl(mockApi)
    }

    @Test
    fun registerToken_success_callsApiWithCorrectFormat() = runTest {
        val testToken = "test_fcm_token_123"
        coEvery { mockApi.registerFcmToken(any()) } returns Response.success(Unit)

        val result = repository.registerToken(testToken)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockApi.registerFcmToken(FcmTokenRequest(testToken))
        }
    }

    @Test
    fun registerToken_apiError_returnsFailure() = runTest {
        val testToken = "test_fcm_token_123"
        coEvery { mockApi.registerFcmToken(any()) } returns Response.error(
            500,
            "Internal Server Error".toResponseBody(null)
        )

        val result = repository.registerToken(testToken)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("Failed to register FCM token") == true)
    }

    @Test
    fun registerToken_exception_returnsFailure() = runTest {
        val testToken = "test_fcm_token_123"
        coEvery { mockApi.registerFcmToken(any()) } throws RuntimeException("Network crash")

        val result = repository.registerToken(testToken)

        assertTrue(result.isFailure)
        assertEquals("Network crash", result.exceptionOrNull()?.message)
    }
}
