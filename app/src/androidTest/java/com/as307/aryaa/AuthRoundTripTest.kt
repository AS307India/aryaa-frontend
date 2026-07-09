package com.as307.aryaa

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.as307.aryaa.data.local.TokenManager
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

@RunWith(AndroidJUnit4::class)
class AuthRoundTripTest {

    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tokenStorage = TokenManager(context)
        val json = Json { ignoreUnknownKeys = true }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("X-Aryaa-Test", "true")
                    .build()
                chain.proceed(req)
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(AryaaApi::class.java)
        repository = AuthRepositoryImpl(api, tokenStorage, json)
    }

    @Test
    fun testRealLoginRoundTrip() = runTest {
        // Register/Login a test account against the live backend
        val email = "roundtrip_${System.currentTimeMillis()}@test.com"
        val phone = "9" + (100000000 + (0..899999999).random()).toString()
        val signupResult = repository.signup("RoundTrip User", email, phone, "password123")
        assertTrue(signupResult.isSuccess)
        assertNotNull(signupResult.getOrNull()?.token)

        val loginResult = repository.login(email, "password123")
        assertTrue(loginResult.isSuccess)
        assertNotNull(loginResult.getOrNull()?.token)
    }
}
