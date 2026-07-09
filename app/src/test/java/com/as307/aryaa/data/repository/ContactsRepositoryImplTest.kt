package com.as307.aryaa.data.repository

import com.as307.aryaa.data.local.db.ContactDao
import com.as307.aryaa.data.local.db.ContactEntity
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.AddContactRequest
import com.as307.aryaa.data.remote.dto.ApiError
import com.as307.aryaa.data.remote.dto.ContactDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ContactsRepositoryImplTest {

    private lateinit var fakeApi: FakeApi
    private lateinit var fakeDao: FakeDao
    private lateinit var repository: ContactsRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        fakeApi = FakeApi()
        fakeDao = FakeDao()
        repository = ContactsRepositoryImpl(fakeApi, fakeDao, json)
    }

    @Test
    fun getContacts_networkSuccess_firstEmissionIsNetworkResult() = runTest {
        // Pre-populate cache with one contact
        val cached = ContactEntity("1", "Cached Alice", "9876543210", "FAMILY", "user1")
        fakeDao.insertAll(listOf(cached))

        // Network returns a fresh list
        fakeApi.contactsToReturn = listOf(
            ContactDto("1", "Network Alice", "9876543210", "FAMILY", "user1", "", "")
        )

        // With a synchronous fake, onStart runs the network call before the first emission,
        // so the DAO is already updated with network data when the flow emits.
        val result = repository.getContacts().first()
        assertEquals("Network Alice", result.first().name)
    }

    @Test
    fun getContacts_networkFailure_firstEmissionIsCache() = runTest {
        // Pre-populate cache with one contact
        val cached = ContactEntity("1", "Cached Alice", "9876543210", "FAMILY", "user1")
        fakeDao.insertAll(listOf(cached))

        // Network throws IOException — cache should be served unchanged
        fakeApi.shouldThrowIOException = true

        val result = repository.getContacts().first()
        assertEquals("Cached Alice", result.first().name)
    }

    @Test
    fun addContact_success_insertsIntoCache() = runTest {
        fakeApi.addContactResponse = Response.success(
            ContactDto("2", "Bob", "9123456780", "FRIEND", "user1", "", "")
        )

        val result = repository.addContact("Bob", "9123456780", "FRIEND")
        assertTrue(result.isSuccess)
        assertEquals(1, fakeDao.contacts.value.size)
        assertEquals("Bob", fakeDao.contacts.value.first().name)
    }

    @Test
    fun addContact_networkFailure_returnsNetworkError() = runTest {
        fakeApi.shouldThrowIOException = true

        val result = repository.addContact("Charlie", "9000000000", "COLLEAGUE")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.NetworkError)
    }

    @Test
    fun addContact_400response_returnsValidationError() = runTest {
        val errorJson = """{"error":"Bad Request","message":"phone: Invalid phone format"}"""
        fakeApi.addContactResponse = Response.error(
            400, errorJson.toResponseBody(null)
        )

        val result = repository.addContact("Dave", "123", "OTHER")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.ValidationError)
        assertEquals("phone: Invalid phone format",
            (result.exceptionOrNull() as AuthError.ValidationError).message)
    }

    @Test
    fun removeContact_success_deletesFromCache() = runTest {
        fakeDao.insertAll(listOf(
            ContactEntity("3", "Eve", "9111111111", "NEIGHBOUR", "user1")
        ))
        fakeApi.deleteContactResponse = Response.success(Unit)

        val result = repository.removeContact("3")
        assertTrue(result.isSuccess)
        assertTrue(fakeDao.contacts.value.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Fakes
    // -------------------------------------------------------------------------

    class FakeApi : AryaaApi {
        var contactsToReturn: List<ContactDto> = emptyList()
        var addContactResponse: Response<ContactDto> =
            Response.success(ContactDto("x", "Test", "9000000001", "FAMILY", "u", "", ""))
        var deleteContactResponse: Response<Unit> = Response.success(Unit)
        var shouldThrowIOException = false

        override suspend fun signup(request: com.as307.aryaa.data.remote.dto.SignupRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.AuthResponse("tok",
                com.as307.aryaa.data.remote.dto.UserDto("u","T","t@t.com","9000000000","")))

        override suspend fun login(request: com.as307.aryaa.data.remote.dto.LoginRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.AuthResponse("tok",
                com.as307.aryaa.data.remote.dto.UserDto("u","T","t@t.com","9000000000","")))

        override suspend fun getContacts(): Response<List<ContactDto>> {
            if (shouldThrowIOException) throw java.io.IOException("No network")
            return Response.success(contactsToReturn)
        }

        override suspend fun addContact(request: AddContactRequest): Response<ContactDto> {
            if (shouldThrowIOException) throw java.io.IOException("No network")
            return addContactResponse
        }

        override suspend fun deleteContact(id: String): Response<Unit> {
            if (shouldThrowIOException) throw java.io.IOException("No network")
            return deleteContactResponse
        }

        override suspend fun triggerSos(request: com.as307.aryaa.data.remote.dto.SosTriggerRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.SosResponse("","","", emptyList()))
        override suspend fun cancelSos(request: com.as307.aryaa.data.remote.dto.SosCancelRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.SosCancelResponse("","",""))
        override suspend fun duressCancel(request: com.as307.aryaa.data.remote.dto.SosCancelRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.SosCancelResponse("","",""))
        override suspend fun getSosHistory() =
            Response.success(emptyList<com.as307.aryaa.data.remote.dto.SosHistoryItem>())
        override suspend fun updateLocation(request: com.as307.aryaa.data.remote.dto.SosLocationUpdateRequest) =
            Response.success(Unit)
        override suspend fun registerFcmToken(request: com.as307.aryaa.data.remote.dto.FcmTokenRequest) =
            Response.success(Unit)
        override suspend fun startDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneStartRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.DeadZoneResponse("", ""))
        override suspend fun checkInDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.DeadZoneResponse("", ""))
        override suspend fun cancelDeadZone(request: com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest) =
            Response.success(com.as307.aryaa.data.remote.dto.DeadZoneResponse("", ""))
        override suspend fun getDeadZoneStatus() =
            Response.success(com.as307.aryaa.data.remote.dto.DeadZoneStatusContainer(null))
    }

    class FakeDao : ContactDao {
        val contacts = MutableStateFlow<List<ContactEntity>>(emptyList())

        override suspend fun insertAll(c: List<ContactEntity>) {
            contacts.update { existing ->
                val map = (existing + c).associateBy { it.id }
                map.values.toList()
            }
        }

        override suspend fun insert(contact: ContactEntity) {
            contacts.update { it + contact }
        }

        override fun getAllContacts(): Flow<List<ContactEntity>> = contacts

        override suspend fun getAllContactsOnce(): List<ContactEntity> = contacts.value

        override suspend fun deleteContactById(id: String): Int {
            val before = contacts.value.size
            contacts.update { it.filter { c -> c.id != id } }
            return before - contacts.value.size
        }

        override suspend fun clearAll(): Int {
            val count = contacts.value.size
            contacts.value = emptyList()
            return count
        }
    }
}
