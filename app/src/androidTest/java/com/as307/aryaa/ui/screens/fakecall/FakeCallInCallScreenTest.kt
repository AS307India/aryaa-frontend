package com.as307.aryaa.ui.screens.fakecall

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.as307.aryaa.data.local.FakeCallPreferences
import com.as307.aryaa.ui.theme.AryaaTheme
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class FakeCallInCallScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class FakePreferences : FakeCallPreferences(mockk(relaxed = true)) {
        var name = "Maa"
        var delay = 5
        override suspend fun getCallerName(): String = name
        override suspend fun getCallerDelay(): Int = delay
    }

    private val preferences = FakePreferences()
    private val viewModel = FakeCallViewModel(preferences)

    @Test
    fun inCallScreen_displaysCallerAndDurationAndActionGrid() {
        var callEnded = false

        preferences.name = "Maa"
        viewModel.answerCall() // transitions state to InCall("Maa", "0:00")

        composeTestRule.setContent {
            AryaaTheme {
                FakeCallInCallScreen(
                    viewModel = viewModel,
                    onCallEnded = { callEnded = true }
                )
            }
        }

        // Verify caller info and elapsed duration counter
        composeTestRule.onNodeWithText("Maa").assertIsDisplayed()
        composeTestRule.onNodeWithText("0:00").assertIsDisplayed()

        // Verify key action button elements are visible
        composeTestRule.onNodeWithText("Mute").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keypad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Speaker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hold").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("End Call").assertIsDisplayed()

        // Click End Call floating button and verify navigation trigger
        composeTestRule.onNodeWithContentDescription("End Call").performClick()
        assert(viewModel.uiState.value is FakeCallUiState.Ended)
        assert(callEnded)
    }
}
