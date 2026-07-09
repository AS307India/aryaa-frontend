package com.as307.aryaa.ui.screens.auth.signup

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.as307.aryaa.data.remote.dto.AuthResponse
import com.as307.aryaa.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signupScreen_invalidInputs_showsValidationError() {
        val repository = FakeAuthRepository()
        val viewModel = SignupViewModel(repository)

        composeTestRule.setContent {
            SignupScreen(
                initialEmail = null,
                onNavigateToLogin = {},
                onSignupSuccess = {},
                viewModel = viewModel
            )
        }

        // Enter invalid inputs
        composeTestRule.onNodeWithText("Name").performTextInput("User")
        composeTestRule.onNodeWithText("Email").performTextInput("invalidemail")
        composeTestRule.onNodeWithText("Phone").performTextInput("123")
        composeTestRule.onNodeWithText("Password").performTextInput("short")

        // Click Create Account
        composeTestRule.onNodeWithText("Create Account").performClick()

        // Verify that the validation warning is displayed
        composeTestRule.onNodeWithText("Please enter a valid email address.").assertExists()
        assertTrue(viewModel.uiState.value is SignupViewModel.UiState.Error)
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun signup(name: String, email: String, phone: String, password: String): Result<AuthResponse> = Result.failure(Exception())
        override suspend fun login(email: String, password: String): Result<AuthResponse> = Result.failure(Exception())
        override suspend fun logout() {}
        override fun isLoggedIn(): Flow<Boolean> = flowOf(false)
    }
}
