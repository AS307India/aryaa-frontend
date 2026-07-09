package com.as307.aryaa.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FakeCallPreferencesTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var preferences: FakeCallPreferences

    @Before
    fun setUp() {
        val testScope = TestScope(UnconfinedTestDispatcher())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tmpFolder.root, "test_prefs.preferences_pb") }
        )
        preferences = FakeCallPreferences(dataStore)
    }

    @Test
    fun defaultCallerName_isMaa() = runTest {
        assertEquals("Maa", preferences.getCallerName())
    }

    @Test
    fun setCallerName_getCallerName_roundTrip() = runTest {
        preferences.setCallerName("Papa")
        assertEquals("Papa", preferences.getCallerName())
    }

    @Test
    fun defaultDelay_is5() = runTest {
        assertEquals(5, preferences.getCallerDelay())
    }

    @Test
    fun setCallerDelay_getCallerDelay_roundTrip() = runTest {
        preferences.setCallerDelay(15)
        assertEquals(15, preferences.getCallerDelay())
    }
}
