package com.as307.aryaa.ui.screens.sos

import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.remote.dto.SosCancelResponse
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import com.as307.aryaa.data.remote.dto.SosHistoryItem
import com.as307.aryaa.data.remote.dto.SosResponse
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.data.repository.SosError
import com.as307.aryaa.data.repository.SosRepository
import com.as307.aryaa.service.SosServiceManager
import io.mockk.mockk
import io.mockk.coEvery
import com.as307.aryaa.data.local.ProfilePreferences
import com.as307.aryaa.data.local.db.ActiveSosDao
import com.as307.aryaa.data.local.db.ActiveSosEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSosRepo: FakeSosRepository
    private lateinit var fakeContactsRepo: FakeContactsRepository
    private lateinit var fakeLocationProvider: FakeLocationProvider
    private lateinit var fakeSosServiceManager: FakeSosServiceManager
    private lateinit var fakeActiveSosDao: FakeActiveSosDao
    private lateinit var profilePreferences: ProfilePreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSosRepo = FakeSosRepository()
        fakeContactsRepo = FakeContactsRepository(emptyList())
        fakeLocationProvider = FakeLocationProvider()
        fakeSosServiceManager = FakeSosServiceManager()
        fakeActiveSosDao = FakeActiveSosDao()
        profilePreferences = mockk(relaxed = true)
        coEvery { profilePreferences.getSosHoldDuration() } returns 3
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun triggerSos_success_transitionsToActiveAndPersistsState() = runTest {
        val vm = SosViewModel(fakeSosRepo, fakeContactsRepo, fakeLocationProvider, fakeSosServiceManager, fakeActiveSosDao, profilePreferences)
        advanceUntilIdle()

        assertEquals(SosUiState.Idle, vm.uiState.value)
        assertNull(fakeActiveSosDao.activeSos)

        vm.onHoldStart()
        // Wait through the delay loops: 3 -> 2 -> 1 (3 seconds total)
        advanceTimeBy(3500)

        // Verifies the trigger completed and UI transitioned to Active
        val state = vm.uiState.value
        assertTrue(state is SosUiState.Active)
        assertEquals("event-123", (state as SosUiState.Active).sosEventId)

        // Verify active SOS state was saved to local database
        assertNotNull(fakeActiveSosDao.activeSos)
        assertEquals("event-123", fakeActiveSosDao.activeSos?.sosEventId)
        assertEquals("///filled.count.soap", fakeActiveSosDao.activeSos?.w3wAddress)
        assertEquals(1, fakeActiveSosDao.insertCalls)
    }

    @Test
    fun releaseHold_duringCountdown_cancelsCountdownAndReturnsIdle() = runTest {
        val vm = SosViewModel(fakeSosRepo, fakeContactsRepo, fakeLocationProvider, fakeSosServiceManager, fakeActiveSosDao, profilePreferences)
        advanceUntilIdle()

        vm.onHoldStart()
        advanceTimeBy(1200) // Advances past the first tick (Countdown(3))

        // Hold released
        vm.onHoldRelease()
        advanceUntilIdle()

        // Verify we went back to Idle and trigger was never called, and nothing saved to DB
        assertEquals(SosUiState.Idle, vm.uiState.value)
        assertEquals(0, fakeSosRepo.triggerCallCount)
        assertNull(fakeActiveSosDao.activeSos)
    }

    @Test
    fun cancelSos_success_transitionsToCancelledAndClearsPersistedState() = runTest {
        val vm = SosViewModel(fakeSosRepo, fakeContactsRepo, fakeLocationProvider, fakeSosServiceManager, fakeActiveSosDao, profilePreferences)
        advanceUntilIdle()

        // Force transition to Active state programmatically for test
        vm.onHoldStart()
        advanceTimeBy(3500)

        assertTrue(vm.uiState.value is SosUiState.Active)
        assertNotNull(fakeActiveSosDao.activeSos)

        vm.onCancelSos()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is SosUiState.Cancelled)

        // Verify local database state is cleared
        assertNull(fakeActiveSosDao.activeSos)
        assertEquals(1, fakeActiveSosDao.clearCalls)
    }

    @Test
    fun duressCancel_success_transitionsToCancelledAndClearsPersistedState() = runTest {
        val vm = SosViewModel(fakeSosRepo, fakeContactsRepo, fakeLocationProvider, fakeSosServiceManager, fakeActiveSosDao, profilePreferences)
        advanceUntilIdle()

        // Force transition to Active state programmatically for test
        vm.onHoldStart()
        advanceTimeBy(3500)

        assertTrue(vm.uiState.value is SosUiState.Active)
        assertNotNull(fakeActiveSosDao.activeSos)

        vm.onDuressCancel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is SosUiState.Cancelled)

        // Verify local database state is cleared
        assertNull(fakeActiveSosDao.activeSos)
        assertEquals(1, fakeActiveSosDao.clearCalls)
    }

    @Test
    fun triggerSos_alreadyActive_surfacesError() = runTest {
        fakeSosRepo.shouldFailWithAlreadyActive = true
        val vm = SosViewModel(fakeSosRepo, fakeContactsRepo, fakeLocationProvider, fakeSosServiceManager, fakeActiveSosDao, profilePreferences)
        advanceUntilIdle()

        vm.onHoldStart()
        advanceTimeBy(3500)

        val state = vm.uiState.value
        assertTrue(state is SosUiState.Error)
        assertEquals(SosError.AlreadyActive, (state as SosUiState.Error).error)
        assertNull(fakeActiveSosDao.activeSos)
    }

    @Test
    fun init_withPersistedActiveSosState_restoresStateAndStartsService() = runTest {
        // Pre-populate database with an active SOS state
        fakeActiveSosDao.activeSos = ActiveSosEntity(
            sosEventId = "event-persisted",
            triggeredAt = "2026-07-01T12:00:00Z",
            w3wAddress = "///filled.count.soap",
            contactsJson = "[]",
            latitude = 12.9,
            longitude = 77.5
        )

        // Pre-populate mock repository history with matching active event
        fakeSosRepo.historyToReturn = listOf(
            SosHistoryItem(
                id = "event-persisted",
                status = "ACTIVE",
                latitude = 12.9,
                longitude = 77.5,
                address = null,
                w3wAddress = "///filled.count.soap",
                triggeredAt = "2026-07-01T12:00:00Z",
                cancelledAt = null,
                contacts = emptyList()
            )
        )

        // Instantiate ViewModel
        val vm = SosViewModel(fakeSosRepo, fakeContactsRepo, fakeLocationProvider, fakeSosServiceManager, fakeActiveSosDao, profilePreferences)
        advanceUntilIdle()

        // Verify state is restored to Active
        val state = vm.uiState.value
        assertTrue(state is SosUiState.Active)
        assertEquals("event-persisted", (state as SosUiState.Active).sosEventId)
        assertEquals("///filled.count.soap", (state as SosUiState.Active).w3wAddress)

        // Verify foreground service is automatically restarted
        assertEquals(1, fakeSosServiceManager.startCalls.size)
        assertEquals("event-persisted", fakeSosServiceManager.startCalls[0].first)
    }

    // --- Fakes ---

    class FakeActiveSosDao : ActiveSosDao {
        var activeSos: ActiveSosEntity? = null
        var insertCalls = 0
        var clearCalls = 0

        override suspend fun insertActiveSos(entity: ActiveSosEntity) {
            insertCalls++
            activeSos = entity
        }

        override suspend fun getActiveSos(): ActiveSosEntity? = activeSos

        override suspend fun clearActiveSos() {
            clearCalls++
            activeSos = null
        }
    }

    class FakeSosRepository : SosRepository {
        var triggerCallCount = 0
        var shouldFailWithAlreadyActive = false
        var historyToReturn: List<SosHistoryItem> = emptyList()

        override suspend fun triggerSos(lat: Double?, lng: Double?, address: String?, accuracy: Double?): Result<SosResponse> {
            triggerCallCount++
            return if (shouldFailWithAlreadyActive) {
                Result.failure(SosError.AlreadyActive)
            } else {
                Result.success(
                    SosResponse(
                        sosEventId = "event-123",
                        status = "ACTIVE",
                        triggeredAt = "2026-07-01T12:00:00Z",
                        contacts = emptyList(),
                        w3wAddress = "///filled.count.soap"
                    )
                )
            }
        }

        override suspend fun cancelSos(sosEventId: String): Result<SosCancelResponse> {
            return Result.success(SosCancelResponse(sosEventId, "CANCELLED", "2026-07-01T12:05:00Z"))
        }

        override suspend fun duressCancel(sosEventId: String): Result<SosCancelResponse> {
            return Result.success(SosCancelResponse(sosEventId, "CANCELLED", "2026-07-01T12:05:00Z"))
        }

        override suspend fun getSosHistory(): Result<List<SosHistoryItem>> {
            return Result.success(historyToReturn)
        }

        override suspend fun sendLocationUpdate(
            sosEventId: String, lat: Double, lng: Double, timestamp: String
        ): Result<Unit> = Result.success(Unit)
    }

    class FakeSosServiceManager : SosServiceManager(mockk(relaxed = true)) {
        val startCalls = mutableListOf<Pair<String, List<SosContactSnapshot>>>()
        val cancelCalls = mutableListOf<Unit>()

        override fun startSos(sosEventId: String, contacts: List<SosContactSnapshot>, w3wAddress: String?) {
            startCalls.add(Pair(sosEventId, contacts))
        }

        override fun cancelSos() {
            cancelCalls.add(Unit)
        }

        val duressCancelCalls = mutableListOf<Unit>()
        override fun duressCancel() {
            duressCancelCalls.add(Unit)
        }
    }

    class FakeContactsRepository(private val contacts: List<ContactDto>) : ContactsRepository {
        override fun getContacts(): Flow<List<ContactDto>> = flowOf(contacts)
        override suspend fun addContact(name: String, phone: String, relationship: String): Result<ContactDto> = Result.failure(Exception())
        override suspend fun removeContact(id: String): Result<Unit> = Result.failure(Exception())
    }

    class FakeLocationProvider : LocationProvider {
        var locationToReturn: android.location.Location? = null
        override suspend fun getLastKnownLocation(): android.location.Location? = locationToReturn
    }
}
