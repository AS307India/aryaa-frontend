package com.as307.aryaa.ui.screens.deadzone

import android.content.Context
import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.local.DeadZonePreferences
import com.as307.aryaa.data.remote.dto.DeadZoneResponse
import com.as307.aryaa.data.remote.dto.DeadZoneStatusContainer
import com.as307.aryaa.data.repository.DeadZoneRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeadZoneViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val deadZoneRepository = mockk<DeadZoneRepository>(relaxed = true)
    private val locationProvider = mockk<LocationProvider>(relaxed = true)
    private val preferences = mockk<DeadZonePreferences>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private lateinit var viewModel: DeadZoneViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        // Default to no active session
        coEvery { preferences.getActiveCheckInId() } returns null
        coEvery { preferences.getExpectedBackAt() } returns null
        coEvery { deadZoneRepository.getStatus() } returns Result.success(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(android.util.Log::class)
    }

    @Test
    fun init_restoresActiveSession_whenCacheHasData() = runTest {
        // Cache reports active session
        coEvery { preferences.getActiveCheckInId() } returns "checkin-123"
        coEvery { preferences.getExpectedBackAt() } returns "2026-07-09T09:00:00Z"

        // Backend also confirms it's still active (prevents sync from resetting to Idle)
        coEvery { deadZoneRepository.getStatus() } returns Result.success(
            DeadZoneResponse(
                checkInId = "checkin-123",
                status = "PENDING",
                startedAt = "2026-07-09T08:00:00Z",
                expectedBackAt = "2026-07-09T09:00:00Z",
                gracePeriodEnd = "2026-07-09T09:30:00Z"
            )
        )

        viewModel = DeadZoneViewModel(deadZoneRepository, locationProvider, preferences, context)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Pending state, got: $state", state is DeadZoneUiState.Pending)
        assertEquals("checkin-123", (state as DeadZoneUiState.Pending).checkInId)
    }

    @Test
    fun startCheckIn_triggersLocationAndStartsBackend() = runTest {
        val fakeResponse = DeadZoneResponse(
            checkInId = "new-checkin-id",
            status = "PENDING",
            startedAt = "2026-07-09T08:00:00Z",
            expectedBackAt = "2026-07-09T09:00:00Z",
            gracePeriodEnd = "2026-07-09T09:30:00Z"
        )

        coEvery {
            deadZoneRepository.startDeadZone(any(), any(), any(), any())
        } returns Result.success(fakeResponse)

        viewModel = DeadZoneViewModel(deadZoneRepository, locationProvider, preferences, context)
        testScheduler.advanceUntilIdle()

        viewModel.startCheckIn(60)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Pending state, got: $state", state is DeadZoneUiState.Pending)
        assertEquals("new-checkin-id", (state as DeadZoneUiState.Pending).checkInId)
        coVerify { deadZoneRepository.startDeadZone(60, any(), any(), any()) }
    }

    @Test
    fun checkIn_triggersRepositoryAndResets() = runTest {
        // Simulate a restored active session (cache + backend both agree)
        val activeCheckIn = DeadZoneResponse(
            checkInId = "checkin-123",
            status = "PENDING",
            startedAt = "2026-07-09T08:00:00Z",
            expectedBackAt = "2026-07-09T09:00:00Z",
            gracePeriodEnd = "2026-07-09T09:30:00Z"
        )

        coEvery { preferences.getActiveCheckInId() } returns "checkin-123"
        coEvery { preferences.getExpectedBackAt() } returns "2026-07-09T09:00:00Z"
        coEvery { deadZoneRepository.getStatus() } returns Result.success(activeCheckIn)

        coEvery {
            deadZoneRepository.checkIn("checkin-123")
        } returns Result.success(
            DeadZoneResponse(
                checkInId = "checkin-123",
                status = "CHECKED_IN"
            )
        )

        viewModel = DeadZoneViewModel(deadZoneRepository, locationProvider, preferences, context)
        testScheduler.advanceUntilIdle()

        // Verify we're in Pending state before calling checkIn
        assertTrue("Expected Pending before checkIn, got: ${viewModel.uiState.value}",
            viewModel.uiState.value is DeadZoneUiState.Pending)

        viewModel.checkIn()
        testScheduler.advanceUntilIdle()

        assertEquals(DeadZoneUiState.Idle, viewModel.uiState.value)
        coVerify { deadZoneRepository.checkIn("checkin-123") }
    }
}
