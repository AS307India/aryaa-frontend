package com.as307.aryaa.ui.screens.auth.login

import com.as307.aryaa.data.remote.dto.AuthResponse
import com.as307.aryaa.data.remote.dto.UserDto
import com.as307.aryaa.data.repository.AuthError
import com.as307.aryaa.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = LoginViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_emptyEmail_emitsError() = runTest {
        viewModel.login("", "password")

        val state = viewModel.uiState.value
        assertTrue(state is LoginViewModel.UiState.Error)
        assertEquals("Email is required.", (state as LoginViewModel.UiState.Error).message)
    }

    @Test
    fun login_invalidEmailFormat_emitsError() = runTest {
        viewModel.login("invalid-email", "password")

        val state = viewModel.uiState.value
        assertTrue(state is LoginViewModel.UiState.Error)
        assertEquals("Please enter a valid email address.", (state as LoginViewModel.UiState.Error).message)
    }

    @Test
    fun login_success_emitsLoading_then_Success() = runTest {
        val userDto = UserDto("1", "User", "user@test.com", "9876543210", "2026-06-30")
        fakeRepository.loginResult = Result.success(AuthResponse("token", userDto))

        viewModel.login("user@test.com", "password")

        assertEquals(LoginViewModel.UiState.Loading, viewModel.uiState.value)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LoginViewModel.UiState.Success, viewModel.uiState.value)
    }

    @Test
    fun login_failure_emitsLoading_then_Error() = runTest {
        fakeRepository.loginResult = Result.failure(AuthError.InvalidCredentials)

        viewModel.login("user@test.com", "password")

        assertEquals(LoginViewModel.UiState.Loading, viewModel.uiState.value)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LoginViewModel.UiState.Error)
        assertEquals(AuthError.InvalidCredentials.userMessage, (state as LoginViewModel.UiState.Error).message)
    }
}

class FakeAuthRepository : AuthRepository {
    var signupResult: Result<AuthResponse>? = null
    var loginResult: Result<AuthResponse>? = null
    var loggedInFlow: Flow<Boolean> = flowOf(false)

    override suspend fun signup(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<AuthResponse> {
        return signupResult ?: throw IllegalStateException("Signup result not set")
    }

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        return loginResult ?: throw IllegalStateException("Login result not set")
    }

    override suspend fun logout() {}

    override fun isLoggedIn(): Flow<Boolean> = loggedInFlow
}
