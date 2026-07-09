package com.as307.aryaa.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenManagerTest {

    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        tokenManager = TokenManager(context)
    }

    @Test
    fun saveToken_and_retrieveToken_persistsCorrectly() = runTest {
        tokenManager.saveToken("test_token_123")

        val retrieved = tokenManager.getToken()
        assertEquals("test_token_123", retrieved)
    }

    @Test
    fun recreateTokenManager_retainsSavedToken() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        tokenManager.saveToken("persistent_token_456")

        val newManager = TokenManager(context)
        val retrieved = newManager.getToken()

        assertEquals("persistent_token_456", retrieved)
    }

    @Test
    fun clearToken_removesTokenFromStorage() = runTest {
        tokenManager.saveToken("token_to_delete")
        tokenManager.clearToken()

        val retrieved = tokenManager.getToken()
        assertNull(retrieved)
    }
}
