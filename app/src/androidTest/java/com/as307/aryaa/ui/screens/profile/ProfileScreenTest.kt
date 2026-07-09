package com.as307.aryaa.ui.screens.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.as307.aryaa.data.local.ProfilePreferences
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.repository.AuthRepository
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.ui.theme.AryaaTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val contactsRepository: ContactsRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val preferences = FakeProfilePreferences()

    class FakeProfilePreferences : ProfilePreferences(mockk(relaxed = true)) {
        var holdDuration = 3
        var volumeTrigger = true
        var offlineSmsAlerts = true

        override suspend fun getSosHoldDuration(): Int = holdDuration
        override fun getSosHoldDurationFlow(): Flow<Int> = flowOf(holdDuration)
        override suspend fun setSosHoldDuration(duration: Int) { holdDuration = duration }

        override suspend fun getVolumeButtonTrigger(): Boolean = volumeTrigger
        override fun getVolumeButtonTriggerFlow(): Flow<Boolean> = flowOf(volumeTrigger)
        override suspend fun setVolumeButtonTrigger(enabled: Boolean) { volumeTrigger = enabled }

        override suspend fun getOfflineSmsAlert(): Boolean = offlineSmsAlerts
        override fun getOfflineSmsAlertFlow(): Flow<Boolean> = flowOf(offlineSmsAlerts)
        override suspend fun getOfflineSmsAlerts(): Boolean = offlineSmsAlerts
        override suspend fun setOfflineSmsAlert(enabled: Boolean) { offlineSmsAlerts = enabled }
    }

    @Before
    fun setUp() {
        coEvery { tokenStorage.getUserName() } returns "John Doe"
        coEvery { tokenStorage.getUserEmail() } returns "john@aryaa.app"
        coEvery { tokenStorage.getUserPhone() } returns "+919876543210"
        every { contactsRepository.getContacts() } returns flowOf(
            listOf(
                ContactDto("1", "Alice", "9876543210", "FAMILY", "user1", "", ""),
                ContactDto("2", "Bob", "9876543211", "FAMILY", "user1", "", ""),
                ContactDto("3", "Charlie", "9876543212", "FAMILY", "user1", "", "")
            )
        )
    }

    @Test
    fun profileScreen_rendersAccountDetailsAndSettings() {
        val viewModel = ProfileViewModel(tokenStorage, contactsRepository, authRepository, preferences)

        composeTestRule.setContent {
            AryaaTheme {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToContacts = {},
                    onNavigateToMedicalId = {},
                    onSignOutComplete = {}
                )
            }
        }

        // Verify account info
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("john@aryaa.app").assertIsDisplayed()
        composeTestRule.onNodeWithText("+919876543210").assertIsDisplayed()

        // Verify settings labels
        composeTestRule.onNodeWithText("Safety Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trusted Contacts").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed() // Contact count
        composeTestRule.onNodeWithText("SOS Hold Duration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volume Button Trigger").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline SMS Alerts").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMS Permission").assertIsDisplayed()

        // Verify app settings
        composeTestRule.onNodeWithText("Medical ID").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy Policy").assertExists()
        composeTestRule.onNodeWithText("Sign Out").assertExists()
    }
}
