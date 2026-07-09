package com.as307.aryaa.ui.screens.medicalid

import com.as307.aryaa.data.local.MedicalIdPreferences
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeMedicalIdPreferences : MedicalIdPreferences(mockk(relaxed = true)) {
    var bloodType: String? = "Unknown"
    var allergies: String? = ""
    var medications: String? = ""
    var conditions: String? = ""
    var emergencyName: String? = ""
    var emergencyPhone: String? = ""
    var organDonor: Boolean = false
    var notes: String? = ""

    override suspend fun getBloodType(): String? = bloodType
    override suspend fun setBloodType(value: String?) { bloodType = value }

    override suspend fun getAllergies(): String? = allergies
    override suspend fun setAllergies(value: String?) { allergies = value }

    override suspend fun getMedications(): String? = medications
    override suspend fun setMedications(value: String?) { medications = value }

    override suspend fun getConditions(): String? = conditions
    override suspend fun setConditions(value: String?) { conditions = value }

    override suspend fun getEmergencyContactName(): String? = emergencyName
    override suspend fun setEmergencyContactName(value: String?) { emergencyName = value }

    override suspend fun getEmergencyContactPhone(): String? = emergencyPhone
    override suspend fun setEmergencyContactPhone(value: String?) { emergencyPhone = value }

    override suspend fun getOrganDonor(): Boolean = organDonor
    override suspend fun setOrganDonor(value: Boolean) { organDonor = value }

    override suspend fun getNotes(): String? = notes
    override suspend fun setNotes(value: String?) { notes = value }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MedicalIdViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val notifier: MedicalIdNotifier = mockk(relaxed = true)
    private val preferences = FakeMedicalIdPreferences()

    private lateinit var viewModel: MedicalIdViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadMedicalId_loadsDataCorrectly() = runTest(testDispatcher) {
        preferences.bloodType = "O+"
        preferences.allergies = "Peanuts"
        preferences.organDonor = true

        viewModel = MedicalIdViewModel(preferences, notifier)
        testScheduler.advanceUntilIdle()

        assertEquals("O+", viewModel.bloodType.value)
        assertEquals("Peanuts", viewModel.allergies.value)
        assertTrue(viewModel.organDonor.value)
    }

    @Test
    fun saveMedicalId_withValidFields_savesAndTriggersNotification() = runTest(testDispatcher) {
        viewModel = MedicalIdViewModel(preferences, notifier)
        testScheduler.advanceUntilIdle()

        viewModel.saveMedicalId(
            blood = "B-",
            allerg = "Dust",
            meds = "None",
            conds = "None",
            contactName = "Mom",
            contactPhone = "9876543210",
            donor = false,
            noteTexts = "Type 1 Diabetes"
        )
        testScheduler.advanceUntilIdle()

        // Verify saved to preferences
        assertEquals("B-", preferences.bloodType)
        assertEquals("Dust", preferences.allergies)
        assertEquals("Type 1 Diabetes", preferences.notes)

        // Verify notification is posted
        verify { notifier.showNotification() }
    }

    @Test
    fun saveMedicalId_withAllEmptyFields_cancelsNotification() = runTest(testDispatcher) {
        viewModel = MedicalIdViewModel(preferences, notifier)
        testScheduler.advanceUntilIdle()

        // Save completely empty info
        viewModel.saveMedicalId(
            blood = "Unknown",
            allerg = "",
            meds = "",
            conds = "",
            contactName = "",
            contactPhone = "",
            donor = false,
            noteTexts = ""
        )
        testScheduler.advanceUntilIdle()

        // Verify notification is cancelled
        verify { notifier.cancelNotification() }
    }
}
