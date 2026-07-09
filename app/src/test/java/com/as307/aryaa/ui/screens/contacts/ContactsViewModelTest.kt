package com.as307.aryaa.ui.screens.contacts

import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.repository.AuthError
import com.as307.aryaa.data.repository.ContactsRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_emptyContacts_showsEmptyState() = runTest {
        val repo = FakeContactsRepository(emptyList())
        val vm = ContactsViewModel(repo, mockk(relaxed = true))
        advanceUntilIdle()
        assertEquals(ContactsViewModel.UiState.Empty, vm.uiState.value)
    }

    @Test
    fun init_withContacts_showsSuccessState() = runTest {
        val contacts = listOf(
            ContactDto("1", "Alice", "9876543210", "FAMILY", "u", "", "")
        )
        val repo = FakeContactsRepository(contacts)
        val vm = ContactsViewModel(repo, mockk(relaxed = true))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is ContactsViewModel.UiState.Success)
        assertEquals(1, (state as ContactsViewModel.UiState.Success).contacts.size)
    }

    @Test
    fun addContact_failure_setsActionMessage() = runTest {
        val repo = FakeContactsRepository(emptyList(), addShouldFail = true)
        val vm = ContactsViewModel(repo, mockk(relaxed = true))
        advanceUntilIdle()

        var callbackResult: Boolean? = null
        vm.addContact("X", "123", "OTHER") { callbackResult = it }
        advanceUntilIdle()

        assertEquals(false, callbackResult)
    }

    @Test
    fun removeContact_networkError_setsActionMessage() = runTest {
        val contacts = listOf(ContactDto("1", "Alice", "9876543210", "FAMILY", "u", "", ""))
        val repo = FakeContactsRepository(contacts, deleteShouldFail = true)
        val vm = ContactsViewModel(repo, mockk(relaxed = true))
        advanceUntilIdle()

        vm.removeContact("1")
        advanceUntilIdle()

        assertTrue(vm.actionMessage.value != null)
    }

    // -------------------------------------------------------------------------
    // Fake
    // -------------------------------------------------------------------------

    class FakeContactsRepository(
        private val contacts: List<ContactDto>,
        private val addShouldFail: Boolean = false,
        private val deleteShouldFail: Boolean = false
    ) : ContactsRepository {

        private val flow = MutableStateFlow(contacts)

        override fun getContacts(): Flow<List<ContactDto>> = flow

        override suspend fun addContact(name: String, phone: String, relationship: String): Result<ContactDto> {
            return if (addShouldFail) {
                Result.failure(AuthError.ValidationError("Invalid phone format"))
            } else {
                val dto = ContactDto("new", name, phone, relationship, "u", "", "")
                flow.value = flow.value + dto
                Result.success(dto)
            }
        }

        override suspend fun removeContact(id: String): Result<Unit> {
            return if (deleteShouldFail) {
                Result.failure(AuthError.NetworkError)
            } else {
                flow.value = flow.value.filter { it.id != id }
                Result.success(Unit)
            }
        }
    }
}
