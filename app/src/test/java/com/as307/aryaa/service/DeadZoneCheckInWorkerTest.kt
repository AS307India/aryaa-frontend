package com.as307.aryaa.service

import com.as307.aryaa.data.remote.dto.DeadZoneResponse
import com.as307.aryaa.data.repository.DeadZoneRepository
import com.as307.aryaa.service.work.DeadZoneCheckInWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [DeadZoneCheckInWorker.executeWork].
 *
 * The worker's production path uses Hilt EntryPoint injection (requires a real
 * Android Application context). We test the extracted [executeWork] companion
 * function directly — it contains all business logic and is decoupled from
 * the Android framework.
 *
 * Requirements under test:
 *  1. getStatus() is called exactly once during normal execution.
 *  2. On network failure the worker returns Result.success(), NOT Result.retry()
 *     or Result.failure() — no infinite retry behavior.
 *  3. A PENDING backend session still results in Result.success() — the backend
 *     piggyback hook handles alerting, not the worker.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeadZoneCheckInWorkerTest {

    private val deadZoneRepository = mockk<DeadZoneRepository>(relaxed = true)

    @Test
    fun executeWork_callsGetStatus_andReturnsSuccess() = runTest {
        coEvery { deadZoneRepository.getStatus() } returns Result.success(null)

        val result = DeadZoneCheckInWorker.executeWork(deadZoneRepository)

        coVerify(exactly = 1) { deadZoneRepository.getStatus() }
        assertEquals(
            "Worker must return Result.success() on normal execution",
            androidx.work.ListenableWorker.Result.success(),
            result
        )
    }

    /**
     * Key requirement: a network failure must NOT trigger WorkManager retry.
     *
     * Rationale: the backend piggyback hook on the next authenticated request
     * already covers missed check-in detection. An infinite retry loop from
     * the Worker would drain battery without adding safety value.
     */
    @Test
    fun executeWork_whenNetworkFails_returnsSuccess_notRetry() = runTest {
        coEvery { deadZoneRepository.getStatus() } throws java.io.IOException("Simulated timeout")

        val result = DeadZoneCheckInWorker.executeWork(deadZoneRepository)

        assertEquals(
            "Worker must return Result.success() on network failure — not Result.retry()",
            androidx.work.ListenableWorker.Result.success(),
            result
        )
    }

    @Test
    fun executeWork_whenSessionPending_returnsSuccess() = runTest {
        val pendingSession = DeadZoneResponse(
            checkInId = "checkin-abc",
            status = "PENDING",
            startedAt = "2026-07-09T08:00:00Z",
            expectedBackAt = "2026-07-09T09:00:00Z",
            gracePeriodEnd = "2026-07-09T09:30:00Z"
        )
        coEvery { deadZoneRepository.getStatus() } returns Result.success(pendingSession)

        val result = DeadZoneCheckInWorker.executeWork(deadZoneRepository)

        coVerify(exactly = 1) { deadZoneRepository.getStatus() }
        assertEquals(
            "Worker must return Result.success() regardless of session state",
            androidx.work.ListenableWorker.Result.success(),
            result
        )
    }
}
