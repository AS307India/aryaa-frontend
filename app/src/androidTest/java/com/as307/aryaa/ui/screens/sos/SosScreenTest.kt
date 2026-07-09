package com.as307.aryaa.ui.screens.sos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.remote.dto.SosCancelResponse
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import com.as307.aryaa.data.remote.dto.SosHistoryItem
import com.as307.aryaa.data.remote.dto.SosResponse
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.data.repository.SosRepository
import com.as307.aryaa.service.SosServiceManager
import com.as307.aryaa.ui.theme.AryaaTheme
import com.as307.aryaa.data.local.db.ActiveSosDao
import com.as307.aryaa.data.local.db.ActiveSosEntity
import com.as307.aryaa.data.local.ProfilePreferences
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class SosScreenTest {

    companion object {
        init {
            com.as307.aryaa.util.TestEnv.isUnderTest = true
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sosScreen_idleState_displaysSosButtonAndInstructions() {
        val fakeSosRepo = FakeSosRepository()
        val fakeContactsRepo = FakeContactsRepository(
            listOf(ContactDto("1", "Alice", "9876543210", "FAMILY", "user1", "", ""))
        )
        val fakeLocationProvider = FakeLocationProvider()
        val fakeActiveSosDao = FakeActiveSosDao()
        val profilePrefs = FakeProfilePreferences()
        val viewModel = SosViewModel(fakeSosRepo, fakeContactsRepo, fakeLocationProvider, FakeSosServiceManager(), fakeActiveSosDao, profilePrefs)

        composeTestRule.setContent {
            AryaaTheme {
                SosScreen(
                    onNavigateToContacts = {},
                    viewModel = viewModel
                )
            }
        }

        // Verify circular SOS button text is displayed
        composeTestRule.onNodeWithText("SOS").assertIsDisplayed()

        // Verify instructions are visible
        composeTestRule.onNodeWithText("Hold 3 seconds to activate").assertIsDisplayed()
    }

    @Test
    fun sosScreen_zeroContacts_displaysWarningBanner() {
        val fakeSosRepo = FakeSosRepository()
        val fakeContactsRepo = FakeContactsRepository(emptyList())
        val fakeLocationProvider = FakeLocationProvider()
        val fakeActiveSosDao = FakeActiveSosDao()
        val profilePrefs = FakeProfilePreferences()
        val viewModel = SosViewModel(fakeSosRepo, fakeContactsRepo, fakeLocationProvider, FakeSosServiceManager(), fakeActiveSosDao, profilePrefs)

        composeTestRule.setContent {
            AryaaTheme {
                SosScreen(
                    onNavigateToContacts = {},
                    viewModel = viewModel
                )
            }
        }

        // Verify warning banner when 0 contacts are added
        composeTestRule.onNodeWithText("⚠️ No trusted contacts added yet. Tap here to add contacts so they can be notified.").assertIsDisplayed()
    }

    // --- Fakes ---

    class FakeActiveSosDao : ActiveSosDao {
        var activeSos: ActiveSosEntity? = null
        override suspend fun insertActiveSos(entity: ActiveSosEntity) { activeSos = entity }
        override suspend fun getActiveSos(): ActiveSosEntity? = activeSos
        override suspend fun clearActiveSos() { activeSos = null }
    }

    class FakeProfilePreferences : ProfilePreferences(mockk(relaxed = true)) {
        override suspend fun getSosHoldDuration(): Int = 3
    }

    class FakeSosRepository : SosRepository {
        override suspend fun triggerSos(lat: Double?, lng: Double?, address: String?): Result<SosResponse> =
            Result.success(SosResponse("event-1", "ACTIVE", "", emptyList(), null))
        override suspend fun cancelSos(sosEventId: String): Result<SosCancelResponse> =
            Result.success(SosCancelResponse(sosEventId, "CANCELLED", ""))
        override suspend fun getSosHistory(): Result<List<SosHistoryItem>> =
            Result.success(emptyList())
        override suspend fun sendLocationUpdate(
            sosEventId: String, lat: Double, lng: Double, timestamp: String
        ): Result<Unit> = Result.success(Unit)
    }

    class FakeSosServiceManager : SosServiceManager(ApplicationProvider.getApplicationContext()) {
        override fun startSos(sosEventId: String, contacts: List<SosContactSnapshot>, w3wAddress: String?) {}
        override fun cancelSos() {}
    }

    class FakeContactsRepository(private val contacts: List<ContactDto>) : ContactsRepository {
        override fun getContacts(): Flow<List<ContactDto>> = flowOf(contacts)
        override suspend fun addContact(name: String, phone: String, relationship: String) = Result.failure<ContactDto>(Exception())
        override suspend fun removeContact(id: String) = Result.failure<Unit>(Exception())
    }

    class FakeLocationProvider : LocationProvider {
        override suspend fun getLastKnownLocation(): android.location.Location? = null
    }
}
