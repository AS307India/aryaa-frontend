package com.as307.aryaa.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the SOS SMS message formatting logic.
 *
 * Tests [formatSosTriggeredAt] and [buildSosMessage] directly — both are
 * internal top-level functions extracted specifically so they can be tested
 * without Android framework classes (Context, SmsManager, etc.).
 */
class SmsDispatcherTest {

    // ── formatSosTriggeredAt ──────────────────────────────────────────────────

    @Test
    fun formatTriggeredAt_utcNoon_producesCorrectIstTime() {
        // 12:00 UTC = 17:30 IST (UTC+5:30)
        val result = formatSosTriggeredAt("2026-07-01T12:00:00Z")
        assertEquals("01 Jul 2026, 05:30 PM IST", result)
    }

    @Test
    fun formatTriggeredAt_utcMidnight_producesCorrectIstTime() {
        // 00:00 UTC = 05:30 IST
        val result = formatSosTriggeredAt("2026-07-01T00:00:00Z")
        assertEquals("01 Jul 2026, 05:30 AM IST", result)
    }

    @Test
    fun formatTriggeredAt_invalidString_returnsOriginal() {
        val result = formatSosTriggeredAt("not-a-date")
        assertEquals("not-a-date", result)
    }

    // ── buildSosMessage ───────────────────────────────────────────────────────

    @Test
    fun buildSosMessage_withW3wAddress_containsW3wAndInstructions() {
        val msg = buildSosMessage(
            userName = "Priya Singh",
            triggeredAt = "2026-07-01T12:00:00Z",
            latitude = 12.9716,
            longitude = 77.5946,
            w3wAddress = "///filled.count.soap"
        )
        assertTrue(msg.contains("Priya Singh needs help! SOS via ARYAA."))
        assertTrue(msg.contains("Location: ///filled.count.soap (what3words)"))
        assertTrue(msg.contains("Coords: 12.9716, 77.5946"))
        assertTrue(msg.contains("Time: 01 Jul 2026, 05:30 PM IST"))
        assertTrue(msg.contains("Open ///filled.count.soap in the what3words app to navigate."))
        assertTrue("Multipart check; length=${msg.length}", msg.length > 160)
    }

    @Test
    fun buildSosMessage_withW3wAddressAlternativePrefix_cleansPrefix() {
        val msg = buildSosMessage(
            userName = "Priya Singh",
            triggeredAt = "2026-07-01T12:00:00Z",
            latitude = 12.9716,
            longitude = 77.5946,
            w3wAddress = "filled.count.soap" // no prefix
        )
        assertTrue(msg.contains("Location: ///filled.count.soap (what3words)"))
    }

    @Test
    fun buildSosMessage_withNullW3wButLocationAvailable_usesCoordsTemplate() {
        val msg = buildSosMessage(
            userName = "Priya Singh",
            triggeredAt = "2026-07-01T12:00:00Z",
            latitude = 12.9716,
            longitude = 77.5946,
            w3wAddress = null
        )
        assertTrue(msg.contains("Priya Singh needs help! SOS via ARYAA."))
        assertTrue(msg.contains("Location: 12.9716, 77.5946"))
        assertTrue(msg.contains("Time: 01 Jul 2026, 05:30 PM IST"))
    }

    @Test
    fun buildSosMessage_withBothNull_usesLocationUnavailableTemplate() {
        val msg = buildSosMessage(
            userName = "Test User",
            triggeredAt = "2026-07-01T12:00:00Z",
            latitude = null,
            longitude = null,
            w3wAddress = null
        )
        assertTrue(msg.contains("Priya Singh needs help! SOS via ARYAA.") == false)
        assertTrue(msg.contains("Location unavailable."))
    }

}
