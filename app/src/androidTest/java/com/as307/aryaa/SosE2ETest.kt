package com.as307.aryaa

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.as307.aryaa.data.local.TokenManager
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.AuthInterceptor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(AndroidJUnit4::class)
class SosE2ETest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            com.as307.aryaa.util.TestEnv.isUnderTest = true
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
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
        }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val timestamp = System.currentTimeMillis()
    private lateinit var api: AryaaApi

    @Before
    fun setUp() {
        // Grant location permission so SosService (foregroundServiceType=location) can start on API 34
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val pkg = ApplicationProvider.getApplicationContext<android.content.Context>().packageName
        uiAutomation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_FINE_LOCATION)
        uiAutomation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_COARSE_LOCATION)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tokenManager = TokenManager(context)
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
    fun sosE2E_triggerAndCancel_roundTrip() = runBlocking {
        // 1. Register a fresh user on the backend
        val email = "e2e_sos_$timestamp@test.com"
        val phone = "9" + (100000000 + (0..899999999).random()).toString()
        val signup = api.signup(com.as307.aryaa.data.remote.dto.SignupRequest("E2E SOS User", email, phone, "password123"))
        assertTrue("Signup should succeed", signup.isSuccessful)

        // 2. Wait up to 15 seconds for Splash screen redirect to Login screen
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithText("Email").fetchSemanticsNodes().isNotEmpty()
        }

        // Perform UI login flow
        composeTestRule.onNodeWithText("Email").performTextInput(email)
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("Login").performClick()
        composeTestRule.waitForIdle()

        // Wait up to 10 seconds for the Home screen to load
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("ARYAA").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify we transitioned to Home screen
        composeTestRule.onNodeWithText("ARYAA").assertIsDisplayed()

        // Safe to retrieve repositories from activity now that the compose hierarchy is stable
        val activity = composeTestRule.activity
        val contactsRepository = activity.contactsRepository
        val sosRepository = activity.sosRepository

        // 3. Add an emergency contact first to verify snapshot functionality
        val contactResult = contactsRepository.addContact("SOS Alert Contact", "9999999999", "FAMILY")
        assertTrue("Add contact should succeed", contactResult.isSuccess)

        // 4. Navigate to SOS tab
        composeTestRule.onNodeWithText("SOS").performClick()
        composeTestRule.waitForIdle()

        // Verify SOS Screen renders in Idle state
        composeTestRule.onNodeWithTag("sos_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hold 3 seconds to activate").assertIsDisplayed()

        // 5. Simulate hold gesture (long press for 0.5 seconds) on the SOS button
        composeTestRule.onNodeWithTag("sos_button").performTouchInput {
            longClick(durationMillis = 500)
        }
        composeTestRule.waitForIdle()

        // Wait up to 10 seconds for the Active state to display the Cancel button
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("I'm Safe — Cancel SOS").fetchSemanticsNodes().isNotEmpty()
        }

        // 6. Verify active status on the backend
        val historyBeforeCancel = sosRepository.getSosHistory().getOrNull()
        assertTrue("Should have history events", historyBeforeCancel?.isNotEmpty() == true)
        val activeEvent = historyBeforeCancel!!.first { it.status == "ACTIVE" }
        assertEquals("SOS Alert Contact", activeEvent.contacts.firstOrNull()?.name)

        // 7. Verify the UI has updated to Active state showing "I'm Safe — Cancel SOS"
        composeTestRule.onNodeWithText("I'm Safe — Cancel SOS").assertIsDisplayed()

        // 8. Cancel the SOS event via UI click
        composeTestRule.onNodeWithText("I'm Safe — Cancel SOS").performClick()
        composeTestRule.waitForIdle()

        // 9. Verify cancellation is stored on the backend
        val historyAfterCancel = sosRepository.getSosHistory().getOrNull()
        val cancelledEvent = historyAfterCancel!!.first { it.id == activeEvent.id }
        assertEquals("CANCELLED", cancelledEvent.status)
    }
}
