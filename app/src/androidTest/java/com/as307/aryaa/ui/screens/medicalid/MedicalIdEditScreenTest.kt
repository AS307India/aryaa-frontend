package com.as307.aryaa.ui.screens.medicalid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.as307.aryaa.data.local.MedicalIdPreferences
import com.as307.aryaa.ui.theme.AryaaTheme
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class MedicalIdEditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class FakeMedicalIdPreferences : MedicalIdPreferences(mockk(relaxed = true)) {
        var blood: String? = "A+"
        var allerg: String? = "Peanuts"
        var meds: String? = "Advil"
        var conds: String? = "Asthma"
        var emergencyName: String? = "Jane Doe"
        var emergencyPhone: String? = "9876543210"
        var donor: Boolean = true
        var noteTexts: String? = "Test note"

        override suspend fun getBloodType(): String? = blood
        override suspend fun getAllergies(): String? = allerg
        override suspend fun getMedications(): String? = meds
        override suspend fun getConditions(): String? = conds
        override suspend fun getEmergencyContactName(): String? = emergencyName
        override suspend fun getEmergencyContactPhone(): String? = emergencyPhone
        override suspend fun getOrganDonor(): Boolean = donor
        override suspend fun getNotes(): String? = noteTexts
    }

    private val preferences = FakeMedicalIdPreferences()
    private val notifier = mockk<MedicalIdNotifier>(relaxed = true)

    @Test
    fun medicalIdEditScreen_rendersAllFormFieldsAndSaveButton() {
        preferences.blood = "A+"
        preferences.allerg = "Peanuts"
        preferences.meds = "Advil"
        preferences.conds = "Asthma"
        preferences.emergencyName = "Jane Doe"
        preferences.emergencyPhone = "9876543210"
        preferences.donor = true
        preferences.noteTexts = "Test note"

        val viewModel = MedicalIdViewModel(preferences, notifier)

        composeTestRule.setContent {
            AryaaTheme {
                MedicalIdEditScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        // Verify sections
        composeTestRule.onNodeWithText("Medical Info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Emergency Contact").assertIsDisplayed()
        composeTestRule.onNodeWithText("Other").assertExists()

        // Verify pre-populated values
        composeTestRule.onAllNodesWithText("A+")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Peanuts")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Advil")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Asthma")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Jane Doe")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("9876543210")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Test note")[0].assertExists()

        // Verify Save button
        composeTestRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun medicalIdEditScreen_bloodTypeDropdownOpensOptions() {
        preferences.blood = "A+"
        val viewModel = MedicalIdViewModel(preferences, notifier)

        composeTestRule.setContent {
            AryaaTheme {
                MedicalIdEditScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        // Open Dropdown
        composeTestRule.onAllNodesWithText("A+")[0].performClick()

        // Verify one of the options in dropdown is shown
        composeTestRule.onNodeWithText("O+").assertIsDisplayed()
        composeTestRule.onNodeWithText("AB-").assertIsDisplayed()
    }
}
