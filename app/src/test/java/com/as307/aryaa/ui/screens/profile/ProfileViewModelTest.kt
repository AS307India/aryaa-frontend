package com.as307.aryaa.ui.screens.profile

import com.as307.aryaa.data.local.ProfilePreferences
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.repository.AuthRepository
import com.as307.aryaa.data.repository.ContactsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeProfilePreferences : ProfilePreferences(mockk(relaxed = true)) {
    var holdDuration = 3
    var volumeTrigger = true
    var offlineSmsAlerts = true

    override suspend fun getSosHoldDuration(): Int = holdDuration
    override fun getSosHoldDurationFlow(): Flow<Int> = flowOf(holdDuration)
    override suspend fun setSosHoldDuration(duration: Int) { holdDuration = duration }

    override suspend fun getVolumeButtonTrigger(): Boolean = volumeTrigger
    override fun getVolumeButtonTriggerFlow(): Flow<Boolean> = flowOf(volumeTrigger)
    override suspend fun setVolumeButtonTrigger(enabled: Boolean) { volumeTrigger = enabled }

    override suspend fun getOfflineSmsAlert(): Boolean = offlineSmsAlerts
    override fun getOfflineSmsAlertFlow(): Flow<Boolean> = flowOf(offlineSmsAlerts)
    override suspend fun getOfflineSmsAlerts(): Boolean = offlineSmsAlerts
    override suspend fun setOfflineSmsAlert(enabled: Boolean) { offlineSmsAlerts = enabled }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val contactsRepository: ContactsRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val preferences = FakeProfilePreferences()

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { tokenStorage.getUserName() } returns "Test User"
        coEvery { tokenStorage.getUserEmail() } returns "test@aryaa.app"
        coEvery { tokenStorage.getUserPhone() } returns "+919876543210"
        every { contactsRepository.getContacts() } returns flowOf(
            listOf(
                ContactDto(
                    id = "1",
                    name = "Emergency",
                    phone = "+919999999999",
                    relationship = "FAMILY",
                    userId = "user_123",
                    createdAt = "2026-07-02T11:00:00Z",
                    updatedAt = "2026-07-02T11:00:00Z"
                )
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadUserProfile_loadsDetailsCorrectly() = runTest(testDispatcher) {
        viewModel = ProfileViewModel(tokenStorage, contactsRepository, authRepository, preferences)
        testScheduler.advanceUntilIdle()

        assertEquals("Test User", viewModel.userName.value)
        assertEquals("test@aryaa.app", viewModel.userEmail.value)
        assertEquals("+919876543210", viewModel.userPhone.value)
        assertEquals(1, viewModel.contactCount.value)
    }

    @Test
    fun updatePreferences_savesAndUpdatesState() = runTest(testDispatcher) {
        viewModel = ProfileViewModel(tokenStorage, contactsRepository, authRepository, preferences)
        testScheduler.advanceUntilIdle()

        viewModel.updateSosHoldDuration(2)
        viewModel.updateVolumeButtonTrigger(false)
        viewModel.updateOfflineSmsAlerts(false)
        testScheduler.advanceUntilIdle()

        // Verify state is updated through simulated preferences flows
        assertEquals(2, preferences.holdDuration)
        assertEquals(false, preferences.volumeTrigger)
        assertEquals(false, preferences.offlineSmsAlerts)
    }

    @Test
    fun signOut_performsLogoutAndEmitsNavigationEvent() = runTest(testDispatcher) {
        viewModel = ProfileViewModel(tokenStorage, contactsRepository, authRepository, preferences)
        testScheduler.advanceUntilIdle()

        val navEventsList = mutableListOf<ProfileNavigationEvent>()
        val navJob = launch {
            viewModel.navigationEvents.collect {
                navEventsList.add(it)
            }
        }

        viewModel.signOut()
        testScheduler.advanceUntilIdle()

        coVerify { authRepository.logout() }
        assertEquals(1, navEventsList.size)
        assertTrue(navEventsList[0] is ProfileNavigationEvent.NavigateToLogin)

        navJob.cancel()
    }
}
