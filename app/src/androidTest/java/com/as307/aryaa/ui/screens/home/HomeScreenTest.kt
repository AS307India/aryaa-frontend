package com.as307.aryaa.ui.screens.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.as307.aryaa.data.local.FakeCallPreferences
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.remote.dto.SosHistoryItem
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.data.repository.SosRepository
import com.as307.aryaa.service.SosServiceManager
import com.as307.aryaa.ui.theme.AryaaTheme
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class HomeScreenTest {

    companion object {
        init {
            com.as307.aryaa.util.TestEnv.isUnderTest = true
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_sosInactive_practiceCardEnabled() {
        val fakeSosRepo = FakeSosRepository(isActive = false)
        val fakeContactsRepo = FakeContactsRepository()
        var practiceClicked = false

        composeTestRule.setContent {
            AryaaTheme {
                HomeScreen(
                    tokenStorage = FakeTokenStorage(),
                    contactsRepository = fakeContactsRepo,
                    sosRepository = fakeSosRepo,
                    fakeCallPreferences = FakeFakeCallPreferences(),
                    sosServiceManager = FakeSosServiceManager(),
                    onNavigateToContacts = {},
                    onNavigateToSos = {},
                    onTriggerFakeCall = {},
                    onNavigateToPracticeMode = { practiceClicked = true }
                )
            }
        }

        // Verify card standard instruction subtitle is shown
        composeTestRule.onNodeWithText("Rehearse your SOS flow safely — nothing real is sent").assertIsDisplayed()

        // Click the practice card
        composeTestRule.onNodeWithText("Practice Mode").performClick()
        assertTrue(practiceClicked)
    }

    @Test
    fun homeScreen_sosActive_practiceCardDisabled() {
        val fakeSosRepo = FakeSosRepository(isActive = true)
        val fakeContactsRepo = FakeContactsRepository()
        var practiceClicked = false

        composeTestRule.setContent {
            AryaaTheme {
                HomeScreen(
                    tokenStorage = FakeTokenStorage(),
                    contactsRepository = fakeContactsRepo,
                    sosRepository = fakeSosRepo,
                    fakeCallPreferences = FakeFakeCallPreferences(),
                    sosServiceManager = FakeSosServiceManager(),
                    onNavigateToContacts = {},
                    onNavigateToSos = {},
                    onTriggerFakeCall = {},
                    onNavigateToPracticeMode = { practiceClicked = true }
                )
            }
        }

        // Verify warning subtitle is shown
        composeTestRule.onNodeWithText("Practice unavailable — you have an active emergency alert").assertIsDisplayed()

        // Click the practice card (should be disabled)
        composeTestRule.onNodeWithText("Practice Mode").performClick()
        assertTrue(!practiceClicked)
    }

    // Fakes
    class FakeContactsRepository : ContactsRepository {
        override fun getContacts(): Flow<List<ContactDto>> = flowOf(emptyList())
        override suspend fun addContact(name: String, phone: String, relationship: String) = Result.failure<ContactDto>(Exception())
        override suspend fun removeContact(id: String) = Result.failure<Unit>(Exception())
    }

    class FakeSosRepository(private val isActive: Boolean) : SosRepository {
        override suspend fun triggerSos(latitude: Double, longitude: Double, accuracy: Double, w3wAddress: String) = Result.failure<com.as307.aryaa.data.remote.dto.SosResponse>(Exception())
        override suspend fun cancelSos() = Result.failure<com.as307.aryaa.data.remote.dto.SosCancelResponse>(Exception())
        override suspend fun duressCancel() = Result.failure<com.as307.aryaa.data.remote.dto.SosCancelResponse>(Exception())
        override suspend fun getSosHistory(): Result<List<SosHistoryItem>> {
            val list = if (isActive) {
                listOf(SosHistoryItem("1", "ACTIVE", "2026-07-09T00:00:00Z", null, "///test.w3w", 10.0, emptyList()))
            } else {
                emptyList()
            }
            return Result.success(list)
        }
        override suspend fun updateLocation(latitude: Double, longitude: Double, accuracy: Double, w3wAddress: String) = Result.failure<Unit>(Exception())
    }

    class FakeTokenStorage : TokenStorage {
        override suspend fun saveToken(token: String) {}
        override suspend fun getToken(): String? = "dummy_token"
        override suspend fun clearToken() {}
        override suspend fun saveUserProfile(name: String, email: String, phone: String) {}
        override suspend fun getUserName(): String? = "Alice"
        override suspend fun getUserEmail(): String? = "alice@test.com"
        override suspend fun getUserPhone(): String? = "1234"
        override suspend fun clearUserProfile() {}
    }

    class FakeFakeCallPreferences : FakeCallPreferences {
        override suspend fun getFakeCallDelay(): Int = 0
        override suspend fun saveFakeCallDelay(seconds: Int) {}
        override suspend fun clear() {}
    }

    class FakeSosServiceManager : SosServiceManager {
        override fun startSosService(sosEventId: String, triggeredAt: String, contactsJson: String, initialW3w: String, initialLat: Double, initialLng: Double, initialAccuracy: Double) {}
        override fun stopSosService() {}
        override fun startSilentSyncService() {}
        override fun isServiceRunning(): Boolean = false
    }
}
