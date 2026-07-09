package com.as307.aryaa.service

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for [SosServiceManager].
 *
 * [SosServiceManager.startSos] and [cancelSos] construct [android.content.Intent]
 * objects internally. The Intent constructor throws "Method not mocked" in plain
 * JVM tests — Intent wiring is verified in the instrumented SosServiceTest instead.
 *
 * What IS testable here without Android framework:
 * - [SosServiceManager.emitTriggerEvent] fires on [SosServiceManager.sosTriggerEvents].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SosServiceManagerTest {

    @Test
    fun emitTriggerEvent_deliversToSharedFlow() = runTest {
        // Pass a relaxed mock context — emitTriggerEvent() never touches context
        val manager = SosServiceManager(mockk(relaxed = true))

        val received = mutableListOf<Unit>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.sosTriggerEvents.toList(received)
        }

        manager.emitTriggerEvent()
        manager.emitTriggerEvent()

        collectJob.cancel()

        assertEquals(
            "sosTriggerEvents must emit once per emitTriggerEvent() call",
            2,
            received.size
        )
    }
}
