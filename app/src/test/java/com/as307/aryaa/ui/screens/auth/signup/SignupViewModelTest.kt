package com.as307.aryaa.ui.screens.auth.signup

import com.as307.aryaa.data.remote.dto.AuthResponse
import com.as307.aryaa.data.remote.dto.UserDto
import com.as307.aryaa.ui.screens.auth.login.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: SignupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = SignupViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signup_emptyName_emitsError() = runTest {
        viewModel.signup("", "user@test.com", "9876543210", "password")

        val state = viewModel.uiState.value
        assertTrue(state is SignupViewModel.UiState.Error)
        assertEquals("Name is required.", (state as SignupViewModel.UiState.Error).message)
    }

    @Test
    fun signup_invalidEmail_emitsError() = runTest {
        viewModel.signup("User", "invalid-email", "9876543210", "password")

        val state = viewModel.uiState.value
        assertTrue(state is SignupViewModel.UiState.Error)
        assertEquals("Please enter a valid email address.", (state as SignupViewModel.UiState.Error).message)
    }

    @Test
    fun signup_invalidPhone_emitsError() = runTest {
        viewModel.signup("User", "user@test.com", "12345", "password")

        val state = viewModel.uiState.value
        assertTrue(state is SignupViewModel.UiState.Error)
        assertEquals("Please enter a valid 10-digit Indian phone number.", (state as SignupViewModel.UiState.Error).message)
    }

    @Test
    fun signup_shortPassword_emitsError() = runTest {
        viewModel.signup("User", "user@test.com", "9876543210", "short")

        val state = viewModel.uiState.value
        assertTrue(state is SignupViewModel.UiState.Error)
        assertEquals("Password must be at least 8 characters long.", (state as SignupViewModel.UiState.Error).message)
    }

    @Test
    fun signup_success_emitsLoading_then_Success() = runTest {
        val userDto = UserDto("1", "User", "user@test.com", "9876543210", "2026-06-30")
        fakeRepository.signupResult = Result.success(AuthResponse("token", userDto))

        viewModel.signup("User", "user@test.com", "9876543210", "password123")

        assertEquals(SignupViewModel.UiState.Loading, viewModel.uiState.value)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SignupViewModel.UiState.Success, viewModel.uiState.value)
    }
}
