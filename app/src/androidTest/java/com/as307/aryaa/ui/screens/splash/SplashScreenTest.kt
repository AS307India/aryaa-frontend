package com.as307.aryaa.ui.screens.splash

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import com.as307.aryaa.data.remote.dto.AuthResponse
import com.as307.aryaa.data.repository.AuthRepository
import com.as307.aryaa.ui.navigation.Destination
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.as307.aryaa.ui.theme.AryaaTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSplashScreenDisplaysAndNavigates() {
        lateinit var navController: TestNavHostController
        val fakeRepository = FakeAuthRepository(isLoggedIn = true)
        val testViewModel = SplashViewModel(fakeRepository)

        composeTestRule.setContent {
            AryaaTheme {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(ComposeNavigator())

                NavHost(
                    navController = navController,
                    startDestination = Destination.Splash.route
                ) {
                    composable(Destination.Splash.route) {
                        SplashScreen(
                            onNavigate = { targetRoute ->
                                navController.navigate(targetRoute) {
                                    popUpTo(Destination.Splash.route) { inclusive = true }
                                }
                            },
                            viewModel = testViewModel
                        )
                    }
                    composable(Destination.Home.route) {
                        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize())
                    }
                }
            }
        }

        // Verify "ARYAA" text is displayed initially
        composeTestRule.onNodeWithText("ARYAA").assertIsDisplayed()
        composeTestRule.onNodeWithText("आर्या").assertIsDisplayed()

        // Wait for the splash delay (1500ms) to complete and trigger navigation
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            navController.currentDestination?.route == Destination.Home.route
        }

        // Verify that the current destination is now Home
        assertEquals(Destination.Home.route, navController.currentDestination?.route)
    }

    private class FakeAuthRepository(val isLoggedIn: Boolean) : AuthRepository {
        override suspend fun signup(name: String, email: String, phone: String, password: String): Result<AuthResponse> = Result.failure(Exception())
        override suspend fun login(email: String, password: String): Result<AuthResponse> = Result.failure(Exception())
        override suspend fun logout() {}
        override fun isLoggedIn(): Flow<Boolean> = flowOf(isLoggedIn)
    }
}
