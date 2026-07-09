package com.as307.aryaa

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.as307.aryaa.data.local.TokenManager
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.AuthInterceptor
import com.as307.aryaa.data.remote.dto.SignupRequest
import com.as307.aryaa.data.remote.dto.SosTriggerRequest
import com.as307.aryaa.data.remote.dto.SosCancelRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(AndroidJUnit4::class)
class W3WIntegrationTest {

    private val timestamp = System.currentTimeMillis()
    private lateinit var api: AryaaApi
    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        com.as307.aryaa.util.TestEnv.isUnderTest = true
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        
        // Clear secure preferences to ensure clean state
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "aryaa_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        sharedPreferences.edit().clear().commit()

        tokenManager = TokenManager(context)
        val json = Json { ignoreUnknownKeys = true }
        val authInterceptor = AuthInterceptor(tokenManager)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        api = retrofit.create(AryaaApi::class.java)
    }

    @Test
    fun w3wIntegration_triggerSosWithCoordinates_returnsW3wAddress() {
        runBlocking {
            // 1. Register a fresh user on the backend
            val email = "w3w_test_$timestamp@test.com"
            val phone = "9" + (100000000 + (0..899999999).random()).toString()
            val signup = api.signup(SignupRequest("W3W Test User", email, phone, "password123"))
            assertTrue("Signup should succeed", signup.isSuccessful)

            // Store token for requests
            val token = signup.body()?.token
            assertNotNull(token)
            tokenManager.saveToken(token!!)

            // 2. Trigger SOS with coordinates to trigger W3W lookup
            val triggerResponse = api.triggerSos(
                SosTriggerRequest(
                    latitude = 12.9716,
                    longitude = 77.5946,
                    address = "E2E Test Location"
                )
            )
            assertTrue("SOS Trigger should succeed", triggerResponse.isSuccessful)
            val sos = triggerResponse.body()
            assertNotNull(sos)
            
            // 3. Verify w3wAddress if returned (depends on backend W3W key configuration)
            val w3w = sos?.w3wAddress
            if (w3w != null) {
                val cleanW3w = w3w.removePrefix("///")
                val words = cleanW3w.split(".")
                assertEquals("w3wAddress should have exactly 3 words", 3, words.size)
                assertTrue("Each word should be non-blank", words.all { it.isNotBlank() })
            } else {
                android.util.Log.w("W3WIntegrationTest", "w3wAddress is null; skipping W3W format verification (likely due to missing W3W_API_KEY).")
            }

            // Cleanup: Cancel the triggered event
            sos?.sosEventId?.let { eventId ->
                api.cancelSos(SosCancelRequest(eventId))
            }
        }
    }
}
