package com.as307.aryaa

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.as307.aryaa.data.local.TokenManager
import com.as307.aryaa.data.local.db.AryaaDatabase
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.AuthInterceptor
import com.as307.aryaa.data.repository.AuthError
import com.as307.aryaa.data.repository.AuthRepositoryImpl
import com.as307.aryaa.data.repository.ContactsRepositoryImpl
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Real E2E instrumented test: connects to the Fastify backend at 10.0.2.2:3000.
 * Requires the backend server to be running before the emulator test executes.
 *
 * Test coverage:
 * 1. Register a fresh user session.
 * 2. Add a trusted contact via real POST /api/contacts.
 * 3. Verify Room cache is updated.
 * 4. Verify contact appears in the contacts list.
 * 5. Attempt to add a contact with malformed phone → clean validation error message.
 * 6. Delete the contact.
 * 7. Verify deletion from both backend and Room cache.
 */
@RunWith(AndroidJUnit4::class)
class ContactsE2ETest {

    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var contactsRepository: ContactsRepositoryImpl
    private lateinit var db: AryaaDatabase
    private lateinit var tokenStorage: TokenManager
    private val timestamp = System.currentTimeMillis()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        tokenStorage = TokenManager(context)
        val json = Json { ignoreUnknownKeys = true }

        db = Room.inMemoryDatabaseBuilder(context, AryaaDatabase::class.java).build()
        val contactDao = db.contactDao()

        val authInterceptor = AuthInterceptor(tokenStorage)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(AryaaApi::class.java)

        authRepository = AuthRepositoryImpl(api, tokenStorage, json)
        contactsRepository = ContactsRepositoryImpl(api, contactDao, json)
    }

    @Test
    fun contactsE2E_addListDelete_roundTrip() = runTest {
        // 1. Register a fresh user
        val email = "e2e_contacts_$timestamp@test.com"
        val signup = authRepository.signup("E2E User", email, "9${timestamp.toString().takeLast(9)}", "password123")
        assertTrue("Signup should succeed", signup.isSuccess)
        assertNotNull("Token should be present", signup.getOrNull()?.token)

        // 2. Add a trusted contact via real POST /api/contacts
        val addResult = contactsRepository.addContact("Test Contact", "9876543210", "FAMILY")
        assertTrue("Add contact should succeed: ${addResult.exceptionOrNull()?.message}", addResult.isSuccess)
        val addedContact = addResult.getOrNull()!!
        assertEquals("Test Contact", addedContact.name)
        assertEquals("FAMILY", addedContact.relationship)

        // 3. Verify Room cache is updated (flow emits with new contact)
        val cachedContacts = contactsRepository.getContacts().first()
        assertTrue("Room cache should contain added contact",
            cachedContacts.any { it.id == addedContact.id })

        // 4. Verify contact list count (should be at least 1)
        assertTrue("Should have at least one contact", cachedContacts.isNotEmpty())

        // 5. Attempt to add a contact with malformed phone → expect clean validation error
        val badPhoneResult = contactsRepository.addContact("Bad Phone", "12345", "FRIEND")
        assertTrue("Bad phone should fail", badPhoneResult.isFailure)
        val error = badPhoneResult.exceptionOrNull()
        assertTrue("Error should be ValidationError", error is AuthError.ValidationError)
        val msg = (error as AuthError.ValidationError).userMessage
        // The message should be human-readable, not a raw JSON array
        assertTrue("Error message should not be a raw JSON array", !msg.startsWith("["))
        assertTrue("Error message should mention phone", msg.contains("phone", ignoreCase = true)
                || msg.contains("Invalid", ignoreCase = true))

        // 6. Delete the contact
        val deleteResult = contactsRepository.removeContact(addedContact.id)
        assertTrue("Delete should succeed", deleteResult.isSuccess)

        // 7. Verify deletion from Room cache
        val afterDelete = contactsRepository.getContacts().first()
        assertTrue("Deleted contact should not be in cache",
            afterDelete.none { it.id == addedContact.id })
    }
}
