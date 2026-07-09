package com.as307.aryaa.ui

import com.as307.aryaa.util.VolumeTriggerHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for the volume-down triple-press trigger logic.
 *
 * We test [VolumeTriggerHandler] directly — the extracted state machine for
 * the 3-presses-within-2-seconds mechanic — keeping these tests fast and
 * framework-free (no Activity, no Robolectric).
 */
class VolumeButtonTest {

    private var triggerFired = false
    private lateinit var handler: VolumeTriggerHandler

    @Before
    fun setUp() {
        triggerFired = false
        handler = VolumeTriggerHandler(windowMs = 2000L, requiredPresses = 3) {
            triggerFired = true
        }
    }

    @Test
    fun threeRapidPresses_withinWindow_triggersSos() {
        val now = System.currentTimeMillis()
        handler.onPress(now)
        handler.onPress(now + 100)
        handler.onPress(now + 200)
        assertTrue("3 rapid presses must trigger SOS", triggerFired)
    }

    @Test
    fun twoPresses_withinWindow_doesNotTrigger() {
        val now = System.currentTimeMillis()
        handler.onPress(now)
        handler.onPress(now + 100)
        assertFalse("2 presses must NOT trigger SOS", triggerFired)
    }

    @Test
    fun threePresses_firstTwoExpired_doesNotTrigger() {
        val now = System.currentTimeMillis()
        // First two presses are outside the 2-second window relative to the 3rd
        handler.onPress(now)
        handler.onPress(now + 100)
        // 3rd press arrives 2100ms after the 1st — the first press falls outside window
        handler.onPress(now + 2100)
        assertFalse("Presses spread beyond window must NOT trigger SOS", triggerFired)
    }

    @Test
    fun triggerResetsAfterFiring() {
        val now = System.currentTimeMillis()
        handler.onPress(now)
        handler.onPress(now + 100)
        handler.onPress(now + 200)
        assertTrue(triggerFired)

        // Reset and simulate again
        triggerFired = false
        handler.onPress(now + 300)
        handler.onPress(now + 400)
        assertFalse("Only 2 presses after reset must NOT re-trigger", triggerFired)

        handler.onPress(now + 500)
        assertTrue("3 more presses after reset must trigger again", triggerFired)
    }
}
