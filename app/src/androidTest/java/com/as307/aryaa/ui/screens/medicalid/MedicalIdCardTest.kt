package com.as307.aryaa.ui.screens.medicalid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.as307.aryaa.ui.components.MedicalIdCard
import com.as307.aryaa.ui.theme.AryaaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicalIdCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun medicalIdCard_displaysProminentBloodTypeAndBadge() {
        composeTestRule.setContent {
            AryaaTheme {
                MedicalIdCard(
                    bloodType = "O+",
                    allergies = "Nuts",
                    medications = null, // empty/null field
                    conditions = "Diabetes",
                    emergencyContactName = "Dad",
                    emergencyContactPhone = "9876543210",
                    organDonor = true, // donor badge should show
                    notes = ""
                )
            }
        }

        // Verify blood type displays prominently
        composeTestRule.onNodeWithText("O+").assertIsDisplayed()

        // Verify organ donor badge shows
        composeTestRule.onNodeWithText("🫀 Organ Donor").assertIsDisplayed()

        // Verify non-empty fields
        composeTestRule.onNodeWithText("Nuts").assertIsDisplayed()

        // Verify empty/null field displays "None"
        composeTestRule.onAllNodesWithText("None")[0].assertIsDisplayed()
    }

    @Test
    fun medicalIdCard_hidesOrganDonorBadgeWhenFalse() {
        composeTestRule.setContent {
            AryaaTheme {
                MedicalIdCard(
                    bloodType = "AB-",
                    allergies = "",
                    medications = "",
                    conditions = "",
                    emergencyContactName = "",
                    emergencyContactPhone = "",
                    organDonor = false, // donor badge should NOT show
                    notes = ""
                )
            }
        }

        // Verify organ donor badge is NOT present
        composeTestRule.onNodeWithText("🫀 Organ Donor").assertDoesNotExist()
    }
}
