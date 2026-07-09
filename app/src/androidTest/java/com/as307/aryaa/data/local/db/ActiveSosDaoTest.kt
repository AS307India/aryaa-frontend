package com.as307.aryaa.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ActiveSosDaoTest {

    private lateinit var database: AryaaDatabase
    private lateinit var activeSosDao: ActiveSosDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AryaaDatabase::class.java
        ).allowMainThreadQueries().build()
        activeSosDao = database.activeSosDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertGetAndClearActiveSosRoundTrip() = runTest {
        // 1. Initially database should return null
        var activeSos = activeSosDao.getActiveSos()
        assertNull(activeSos)

        // 2. Insert active SOS state
        val entity = ActiveSosEntity(
            sosEventId = "event-test-123",
            triggeredAt = "2026-07-01T12:00:00Z",
            w3wAddress = "///filled.count.soap",
            contactsJson = "[]",
            latitude = 12.97,
            longitude = 77.59
        )
        activeSosDao.insertActiveSos(entity)

        // 3. Query active SOS state and verify matches
        activeSos = activeSosDao.getActiveSos()
        assertNotNull(activeSos)
        assertEquals("event-test-123", activeSos?.sosEventId)
        assertEquals("///filled.count.soap", activeSos?.w3wAddress)
        assertEquals(12.97, activeSos?.latitude)
        assertEquals(77.59, activeSos?.longitude)

        // 4. Clear active SOS state and verify it returns null again
        activeSosDao.clearActiveSos()
        activeSos = activeSosDao.getActiveSos()
        assertNull(activeSos)
    }
}
