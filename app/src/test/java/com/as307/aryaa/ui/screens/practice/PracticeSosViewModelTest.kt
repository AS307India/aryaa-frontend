package com.as307.aryaa.ui.screens.practice

import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.remote.dto.SosCancelResponse
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.ui.screens.sos.SosUiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeContactsRepo: FakeContactsRepository
    private lateinit var fakeLocationProvider: FakeLocationProvider
    private lateinit var profilePreferences: com.as307.aryaa.data.local.ProfilePreferences
    private lateinit var fakeTokenStorage: com.as307.aryaa.data.local.TokenStorage
    private lateinit var fakeApi: FakeAryaaApi

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeContactsRepo = FakeContactsRepository(
            listOf(
                ContactDto("c1", "Alice", "111", "Family", "u1", "", "", hasFcmToken = true),
                ContactDto("c2", "Bob", "222", "Friend", "u1", "", "", hasFcmToken = false)
            )
        )
        fakeLocationProvider = FakeLocationProvider()
        profilePreferences = mockk(relaxed = true)
        coEvery { profilePreferences.getSosHoldDuration() } returns 3
        fakeTokenStorage = mockk(relaxed = true)
        coEvery { fakeTokenStorage.getUserName() } returns "Charlie"
        fakeApi = FakeAryaaApi()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun countdownComplete_simulatesActiveStateWithoutCallingTriggerSos() = runTest {
        val vm = PracticeSosViewModel(fakeContactsRepo, fakeLocationProvider, profilePreferences, fakeTokenStorage, fakeApi)
        advanceUntilIdle()

        assertEquals(SosUiState.Idle, vm.uiState.value)

        vm.onHoldStart()
        advanceTimeBy(3500) // Finish 3 seconds hold countdown

        // Verify simulated state is Active with practice prefix
        val state = vm.uiState.value
        assertTrue(state is SosUiState.Active)
        val active = state as SosUiState.Active
        assertTrue(active.sosEventId.startsWith("practice-"))
        assertEquals("///practice.mode.only", active.w3wAddress)

        // Verify real location provider and contacts were read
        assertEquals(1, fakeLocationProvider.callCount)
        assertEquals(2, vm.simulatedContacts.value.size)
        assertEquals("Alice", vm.simulatedContacts.value[0].name)
        assertTrue(vm.simulatedContacts.value[0].hasFcmToken)

        // Verify API was called to fetch registration details but trigger was NEVER called
        assertEquals(1, fakeApi.getContactsCount)
        assertEquals(0, fakeApi.triggerSosCount)
    }

    @Test
    fun cancelSos_transitionsToIdleAndEmitsSummaryEvent() = runTest {
        val vm = PracticeSosViewModel(fakeContactsRepo, fakeLocationProvider, profilePreferences, fakeTokenStorage, fakeApi)
        advanceUntilIdle()

        vm.onHoldStart()
        advanceTimeBy(3500)

        val events = mutableListOf<PracticeSosViewModel.PracticeNavigationEvent>()
        val collectJob = launch {
            vm.navigationEvents.collect { events.add(it) }
        }

        // Simulated cancel
        vm.onCancelSos()
        advanceUntilIdle()

        assertEquals(SosUiState.Idle, vm.uiState.value)

        // Verify summary event emitted
        assertEquals(1, events.size)
        val event = events.first()
        assertTrue(event is PracticeSosViewModel.PracticeNavigationEvent.NavigateToSummary)
        val summary = event as PracticeSosViewModel.PracticeNavigationEvent.NavigateToSummary
        assertEquals(3, summary.holdDuration)
        assertEquals(2, summary.contactsCount)
        assertEquals(10, summary.accuracy)
        assertEquals(false, summary.duressPracticed)

        collectJob.cancel()
    }

    @Test
    fun duressCancel_transitionsToIdleAndEmitsSummaryWithDuressTrue() = runTest {
        val vm = PracticeSosViewModel(fakeContactsRepo, fakeLocationProvider, profilePreferences, fakeTokenStorage, fakeApi)
        advanceUntilIdle()

        vm.onHoldStart()
        advanceTimeBy(3500)

        val events = mutableListOf<PracticeSosViewModel.PracticeNavigationEvent>()
        val collectJob = launch {
            vm.navigationEvents.collect { events.add(it) }
        }

        // Simulated duress cancel
        vm.onDuressCancel()
        advanceUntilIdle()

        assertEquals(SosUiState.Idle, vm.uiState.value)

        // Verify summary event emitted with duress = true
        assertEquals(1, events.size)
        val event = events.first()
        val summary = event as PracticeSosViewModel.PracticeNavigationEvent.NavigateToSummary
        assertEquals(true, summary.duressPracticed)

        collectJob.cancel()
    }

    // Fakes
    class FakeContactsRepository(val list: List<ContactDto>) : ContactsRepository {
        override fun getContacts(): Flow<List<ContactDto>> = flowOf(list)
        override suspend fun addContact(name: String, phone: String, relationship: String) = Result.failure<ContactDto>(Exception())
        override suspend fun removeContact(id: String) = Result.failure<Unit>(Exception())
    }

    class FakeLocationProvider : LocationProvider {
        var callCount = 0
        override suspend fun getLastKnownLocation(): android.location.Location? {
            callCount++
            return null
        }
    }

    class FakeAryaaApi : AryaaApi {
        var getContactsCount = 0
        var triggerSosCount = 0

        override suspend fun getContacts(): Response<List<ContactDto>> {
            getContactsCount++
            return Response.success(
                listOf(
                    ContactDto("c1", "Alice", "111", "Family", "u1", "", "", hasFcmToken = true),
                    ContactDto("c2", "Bob", "222", "Friend", "u1", "", "", hasFcmToken = false)
                )
            )
        }

        override suspend fun signup(request: com.as307.aryaa.data.remote.dto.SignupRequest) = throw UnsupportedOperationException()
        override suspend fun login(request: com.as307.aryaa.data.remote.dto.LoginRequest) = throw UnsupportedOperationException()
        override suspend fun addContact(request: com.as307.aryaa.data.remote.dto.AddContactRequest) = throw UnsupportedOperationException()
        override suspend fun deleteContact(id: String) = throw UnsupportedOperationException()
        override suspend fun triggerSos(request: com.as307.aryaa.data.remote.dto.SosTriggerRequest): Response<com.as307.aryaa.data.remote.dto.SosResponse> {
            triggerSosCount++
            return throw UnsupportedOperationException()
        }
        override suspend fun cancelSos(request: com.as307.aryaa.data.remote.dto.SosCancelRequest) = throw UnsupportedOperationException()
        override suspend fun duressCancel(request: com.as307.aryaa.data.remote.dto.SosCancelRequest) = throw UnsupportedOperationException()
        override suspend fun getSosHistory() = throw UnsupportedOperationException()
        override suspend fun updateLocation(request: com.as307.aryaa.data.remote.dto.SosLocationUpdateRequest) = throw UnsupportedOperationException()
        override suspend fun registerFcmToken(request: com.as307.aryaa.data.remote.dto.FcmTokenRequest) = throw UnsupportedOperationException()
        override suspend fun startDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneStartRequest) = throw UnsupportedOperationException()
        override suspend fun checkInDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest) = throw UnsupportedOperationException()
        override suspend fun cancelDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest) = throw UnsupportedOperationException()
        override suspend fun getDeadZoneStatus(): Response<com.as307.aryaa.data.remote.dto.DeadZoneStatusContainer> = throw UnsupportedOperationException()
    }
}
