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

class FakeCallRingingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class FakePreferences : FakeCallPreferences(mockk(relaxed = true)) {
        var name = "Papa"
        var delay = 0
        override suspend fun getCallerName(): String = name
        override suspend fun getCallerDelay(): Int = delay
    }

    private val preferences = FakePreferences()
    private val viewModel = FakeCallViewModel(preferences)

    @Test
    fun ringingScreen_displaysCallerNameAndButtons() {
        var answered = false
        var declined = false

        preferences.name = "Papa"
        preferences.delay = 0
        viewModel.scheduleFakeCall() // transitions state to Ringing("Papa")

        composeTestRule.setContent {
            AryaaTheme {
                FakeCallRingingScreen(
                    viewModel = viewModel,
                    onAnswer = { answered = true },
                    onDecline = { declined = true }
                )
            }
        }

        // Verify caller name is displayed
        composeTestRule.onNodeWithText("Papa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mobile").assertIsDisplayed()

        // Verify answer and decline buttons are present
        composeTestRule.onNodeWithContentDescription("Answer call").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Decline call").assertIsDisplayed()

        // Click answer and verify action
        composeTestRule.onNodeWithContentDescription("Answer call").performClick()
        assert(viewModel.uiState.value is FakeCallUiState.InCall)
        assert(answered)
    }

    @Test
    fun ringingScreen_clickDecline_triggersDeclineAction() {
        var declined = false

        preferences.name = "Papa"
        preferences.delay = 0
        viewModel.scheduleFakeCall() // transitions state to Ringing("Papa")

        composeTestRule.setContent {
            AryaaTheme {
                FakeCallRingingScreen(
                    viewModel = viewModel,
                    onAnswer = {},
                    onDecline = { declined = true }
                )
            }
        }

        // Click decline and verify action
        composeTestRule.onNodeWithContentDescription("Decline call").performClick()
        assert(viewModel.uiState.value is FakeCallUiState.Ended)
        assert(declined)
    }
}
