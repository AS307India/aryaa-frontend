package com.as307.aryaa.util

/**
 * Pure, framework-free state machine for the volume-button SOS trigger.
 *
 * Tracks a sliding window of press timestamps and fires [onTriggered] when
 * [requiredPresses] presses are recorded within [windowMs] milliseconds.
 * The timestamp list is pruned on every press so the window truly slides.
 *
 * This class is intentionally kept free of Android imports so it can be
 * tested with plain JVM unit tests (no Robolectric, no Activity, no Context).
 *
 * @param windowMs      Rolling time window in milliseconds (default 2000).
 * @param requiredPresses Number of presses required to fire the trigger (default 3).
 * @param onTriggered   Callback invoked on the thread that calls [onPress].
 */
class VolumeTriggerHandler(
    private val windowMs: Long = 2000L,
    private val requiredPresses: Int = 3,
    private val onTriggered: () -> Unit
) {
    private val timestamps = mutableListOf<Long>()

    /**
     * Record a press at [nowMs] (epoch millis). If [requiredPresses] recent presses
     * have occurred within [windowMs], [onTriggered] fires and the list is cleared.
     */
    fun onPress(nowMs: Long = System.currentTimeMillis()) {
        timestamps.add(nowMs)
        // Drop entries older than the window
        timestamps.removeAll { nowMs - it > windowMs }
        if (timestamps.size >= requiredPresses) {
            timestamps.clear()
            onTriggered()
        }
    }
}
