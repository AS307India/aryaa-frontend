package com.as307.aryaa.ui.screens.splash

import com.as307.aryaa.data.remote.dto.AuthResponse
import com.as307.aryaa.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSplashTimerTriggersNavigationToHomeWhenLoggedIn() = runTest {
        fakeRepository.isLoggedInFlow = flowOf(true)
        val viewModel = SplashViewModel(fakeRepository)

        val events = mutableListOf<SplashNavigationEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEvent.collect { events.add(it) }
        }

        testDispatcher.scheduler.advanceTimeBy(1550)

        assertEquals(1, events.size)
        assertEquals(SplashNavigationEvent.NavigateToHome, events[0])
    }

    @Test
    fun testSplashTimerTriggersNavigationToLoginWhenLoggedOut() = runTest {
        fakeRepository.isLoggedInFlow = flowOf(false)
        val viewModel = SplashViewModel(fakeRepository)

        val events = mutableListOf<SplashNavigationEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEvent.collect { events.add(it) }
        }

        testDispatcher.scheduler.advanceTimeBy(1550)

        assertEquals(1, events.size)
        assertEquals(SplashNavigationEvent.NavigateToLogin, events[0])
    }
}

class FakeAuthRepository : AuthRepository {
    var isLoggedInFlow: Flow<Boolean> = flowOf(false)

    override suspend fun signup(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<AuthResponse> = Result.failure(Exception())

    override suspend fun login(email: String, password: String): Result<AuthResponse> = Result.failure(Exception())

    override suspend fun logout() {}

    override fun isLoggedIn(): Flow<Boolean> = isLoggedInFlow
}
