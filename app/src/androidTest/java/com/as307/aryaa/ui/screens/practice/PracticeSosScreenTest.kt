package com.as307.aryaa.ui.screens.practice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.ui.theme.AryaaTheme
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class PracticeSosScreenTest {

    companion object {
        init {
            com.as307.aryaa.util.TestEnv.isUnderTest = true
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun practiceSosScreen_displaysBannerAndSosButton() {
        val fakeContactsRepo = FakeContactsRepository(
            listOf(ContactDto("1", "Alice", "9876543210", "FAMILY", "user1", "", ""))
        )
        val fakeLocationProvider = FakeLocationProvider()
        val profilePrefs = FakeProfilePreferences()
        val fakeApi = FakeAryaaApi()
        val viewModel = PracticeSosViewModel(fakeContactsRepo, fakeLocationProvider, profilePrefs, mockk(relaxed = true), fakeApi)

        composeTestRule.setContent {
            AryaaTheme {
                PracticeSosScreen(
                    onNavigateBack = {},
                    onNavigateToSummary = { _, _, _, _ -> },
                    viewModel = viewModel
                )
            }
        }

        // Verify safety banner is visible
        composeTestRule.onNodeWithText("🎓 PRACTICE MODE — No real alerts will be sent").assertIsDisplayed()

        // Verify SOS button and instructions
        composeTestRule.onNodeWithText("SOS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hold the button for 3 seconds to practice triggering SOS.").assertIsDisplayed()
    }

    @Test
    fun practiceSosScreen_zeroContacts_displaysWarningBanner() {
        val fakeContactsRepo = FakeContactsRepository(emptyList())
        val fakeLocationProvider = FakeLocationProvider()
        val profilePrefs = FakeProfilePreferences()
        val fakeApi = FakeAryaaApi()
        val viewModel = PracticeSosViewModel(fakeContactsRepo, fakeLocationProvider, profilePrefs, mockk(relaxed = true), fakeApi)

        composeTestRule.setContent {
            AryaaTheme {
                PracticeSosScreen(
                    onNavigateBack = {},
                    onNavigateToSummary = { _, _, _, _ -> },
                    viewModel = viewModel
                )
            }
        }

        // Verify safety banner is visible
        composeTestRule.onNodeWithText("🎓 PRACTICE MODE — No real alerts will be sent").assertIsDisplayed()

        // Verify warning banner
        composeTestRule.onNodeWithText("⚠️ You have no trusted contacts added. Real alerts would fail to dispatch. Please add contacts in the Contacts tab.").assertIsDisplayed()
    }

    // Fakes
    class FakeContactsRepository(val list: List<ContactDto>) : ContactsRepository {
        override fun getContacts(): Flow<List<ContactDto>> = flowOf(list)
        override suspend fun addContact(name: String, phone: String, relationship: String) = Result.failure<ContactDto>(Exception())
        override suspend fun removeContact(id: String) = Result.failure<Unit>(Exception())
    }

    class FakeLocationProvider : LocationProvider {
        override suspend fun getLastKnownLocation(): android.location.Location? = null
    }

    class FakeProfilePreferences : com.as307.aryaa.data.local.ProfilePreferences {
        override suspend fun saveSosHoldDuration(duration: Int) {}
        override suspend fun getSosHoldDuration(): Int = 3
        override suspend fun clear() {}
    }

    class FakeAryaaApi : AryaaApi {
        override suspend fun getContacts(): Response<List<ContactDto>> {
            return Response.success(emptyList())
        }
        override suspend fun signup(request: com.as307.aryaa.data.remote.dto.SignupRequest) = throw UnsupportedOperationException()
        override suspend fun login(request: com.as307.aryaa.data.remote.dto.LoginRequest) = throw UnsupportedOperationException()
        override suspend fun addContact(request: com.as307.aryaa.data.remote.dto.AddContactRequest) = throw UnsupportedOperationException()
        override suspend fun deleteContact(id: String) = throw UnsupportedOperationException()
        override suspend fun triggerSos(request: com.as307.aryaa.data.remote.dto.SosTriggerRequest) = throw UnsupportedOperationException()
        override suspend fun cancelSos(request: com.as307.aryaa.data.remote.dto.SosCancelRequest) = throw UnsupportedOperationException()
        override suspend fun duressCancel(request: com.as307.aryaa.data.remote.dto.SosCancelRequest) = throw UnsupportedOperationException()
        override suspend fun getSosHistory() = throw UnsupportedOperationException()
        override suspend fun updateLocation(request: com.as307.aryaa.data.remote.dto.SosLocationUpdateRequest) = throw UnsupportedOperationException()
        override suspend fun registerFcmToken(request: com.as307.aryaa.data.remote.dto.FcmTokenRequest) = throw UnsupportedOperationException()
    }
}
