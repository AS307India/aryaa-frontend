package com.as307.aryaa.ui.screens.auth.login

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

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_invalidEmail_showsValidationError() {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        composeTestRule.setContent {
            LoginScreen(
                initialEmail = null,
                onNavigateToSignup = {},
                onLoginSuccess = {},
                viewModel = viewModel
            )
        }

        // Enter invalid email format
        composeTestRule.onNodeWithText("Email").performTextInput("invalidemail")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")

        // Click Login
        composeTestRule.onNodeWithText("Login").performClick()

        // Verify the Crimson validation banner displays the warning
        composeTestRule.onNodeWithText("Please enter a valid email address.").assertExists()
        assertTrue(viewModel.uiState.value is LoginViewModel.UiState.Error)
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun signup(name: String, email: String, phone: String, password: String): Result<AuthResponse> = Result.failure(Exception())
        override suspend fun login(email: String, password: String): Result<AuthResponse> = Result.failure(Exception())
        override suspend fun logout() {}
        override fun isLoggedIn(): Flow<Boolean> = flowOf(false)
    }
}
