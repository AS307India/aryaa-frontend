package com.as307.aryaa.ui.screens.fakecall

import com.as307.aryaa.data.local.FakeCallPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakePreferences : FakeCallPreferences(io.mockk.mockk(relaxed = true)) {
    var callerName = "Maa"
    var callerDelay = 5

    override suspend fun getCallerName(): String = callerName
    override suspend fun setCallerName(name: String) { callerName = name }
    override suspend fun getCallerDelay(): Int = callerDelay
    override suspend fun setCallerDelay(delay: Int) { callerDelay = delay }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FakeCallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferences: FakePreferences
    private lateinit var viewModel: FakeCallViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferences = FakePreferences()
        viewModel = FakeCallViewModel(preferences)
    }

    @After
    fun tearDown() {
        viewModel.endCall()
        Dispatchers.resetMain()
    }

    @Test
    fun scheduleFakeCall_withZeroDelay_transitionsDirectlyToRinging() = runTest(testDispatcher) {
        preferences.callerName = "Maa"
        preferences.callerDelay = 0

        viewModel.scheduleFakeCall()
        try {
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is FakeCallUiState.Ringing)
            assertEquals("Maa", (state as FakeCallUiState.Ringing).callerName)
        } finally {
            viewModel.endCall()
        }
    }

    @Test
    fun scheduleFakeCall_with5SecondDelay_remainsIdleThenTransitionsToRinging() = runTest(testDispatcher) {
        preferences.callerName = "Maa"
        preferences.callerDelay = 5

        viewModel.scheduleFakeCall()
        try {
            testScheduler.runCurrent() // start coroutine up to delay

            // At start (0s), should be Idle
            assertEquals(FakeCallUiState.Idle, viewModel.uiState.value)

            // Advance 4 seconds, still Idle
            testScheduler.advanceTimeBy(4000)
            assertEquals(FakeCallUiState.Idle, viewModel.uiState.value)

            // Advance 1 more second (total 5s), transitions to Ringing
            testScheduler.advanceTimeBy(1000)
            testScheduler.runCurrent()
            assertEquals(FakeCallUiState.Ringing("Maa"), viewModel.uiState.value)
        } finally {
            viewModel.endCall()
        }
    }

    @Test
    fun answerCall_transitionsRingingToInCall() = runTest(testDispatcher) {
        preferences.callerName = "Maa"
        preferences.callerDelay = 0

        viewModel.scheduleFakeCall()
        try {
            testScheduler.advanceUntilIdle()

            viewModel.answerCall()

            val state = viewModel.uiState.value
            assertTrue(state is FakeCallUiState.InCall)
            assertEquals("Maa", (state as FakeCallUiState.InCall).callerName)
            assertEquals("0:00", (state as FakeCallUiState.InCall).duration)
        } finally {
            viewModel.endCall()
        }
    }

    @Test
    fun endCall_fromRinging_transitionsToEnded() = runTest(testDispatcher) {
        preferences.callerName = "Maa"
        preferences.callerDelay = 0

        viewModel.scheduleFakeCall()
        try {
            testScheduler.advanceUntilIdle()

            viewModel.endCall()
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value is FakeCallUiState.Ended)
        } finally {
            viewModel.endCall()
        }
    }

    @Test
    fun endCall_fromInCall_transitionsToEnded() = runTest(testDispatcher) {
        preferences.callerName = "Maa"
        preferences.callerDelay = 0

        viewModel.scheduleFakeCall()
        try {
            testScheduler.advanceUntilIdle()
            viewModel.answerCall()

            viewModel.endCall()
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value is FakeCallUiState.Ended)
        } finally {
            viewModel.endCall()
        }
    }

    @Test
    fun cancelScheduled_cancelsCountdown_staysIdle() = runTest(testDispatcher) {
        preferences.callerName = "Maa"
        preferences.callerDelay = 10

        viewModel.scheduleFakeCall()
        try {
            testScheduler.runCurrent() // start countdown
            testScheduler.advanceTimeBy(5000) // 5 seconds in
            
            viewModel.cancelScheduled()
            testScheduler.runCurrent() // let any pending cancel flows finish

            // Advance past the initial 10s mark to ensure countdown was cancelled
            testScheduler.advanceTimeBy(6000)
            
            assertTrue(viewModel.uiState.value is FakeCallUiState.Idle)
        } finally {
            viewModel.endCall()
        }
    }

    @Test
    fun inCall_durationCounter_incrementsCorrectly() = runTest(testDispatcher) {
        preferences.callerName = "Papa"
        preferences.callerDelay = 0

        viewModel.scheduleFakeCall()
        try {
            testScheduler.advanceUntilIdle()
            viewModel.answerCall()

            // Advance 62 seconds (62000 ms)
            testScheduler.advanceTimeBy(62000)
            testScheduler.runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is FakeCallUiState.InCall)
            assertEquals("1:02", (state as FakeCallUiState.InCall).duration)
        } finally {
            viewModel.endCall()
        }
    }
}
