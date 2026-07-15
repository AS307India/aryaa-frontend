package com.as307.aryaa.data.repository

import com.as307.aryaa.data.local.db.ContactDao
import com.as307.aryaa.data.local.db.ContactEntity
import com.as307.aryaa.data.remote.AryaaApi
import com.as307.aryaa.data.remote.dto.AddContactRequest
import com.as307.aryaa.data.remote.dto.ApiError
import com.as307.aryaa.data.remote.dto.ContactDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val api: AryaaApi,
    private val contactDao: ContactDao,
    private val json: Json
) : ContactsRepository {

    /**
     * Single-source-of-truth pattern: Room is always the data source for the UI.
     * On first collection the Flow emits whatever is cached (may be empty on first install),
     * then a network refresh replaces the cache. The Room Flow automatically re-emits when
     * cache changes, so the UI updates without manual intervention.
     */
    override fun getContacts(): Flow<List<ContactDto>> {
        return contactDao.getAllContacts()
            .map { entities -> entities.map { it.toDto() } }
            .onStart {
                // Trigger a background refresh every time a new collector attaches
                // (e.g. screen enters composition). Errors here are swallowed intentionally;
                // stale cache is better than a crash for a safety-critical contact list.
                try {
                    val response = api.getContacts()
                    if (response.isSuccessful) {
                        val dtos = response.body() ?: emptyList()
                        contactDao.clearAll()
                        contactDao.insertAll(dtos.map { it.toEntity() })
                    }
                } catch (_: IOException) { /* no-op: show cached */ }
            }
    }

    override suspend fun addContact(
        name: String,
        phone: String,
        relationship: String,
        isNearby: String
    ): Result<ContactDto> {
        val result = safeCall { api.addContact(AddContactRequest(name, phone, relationship, isNearby)) }
        result.getOrNull()?.let { dto ->
            // Insert into cache immediately after confirmed backend response
            contactDao.insert(dto.toEntity())
        }
        return result
    }

    override suspend fun removeContact(id: String): Result<Unit> {
        return try {
            val response = api.deleteContact(id)
            if (response.isSuccessful) {
                // Remove from local cache only after confirmed server deletion
                contactDao.deleteContactById(id)
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val msg = errorBody?.let {
                    try { json.decodeFromString<ApiError>(it).message } catch (_: Exception) { null }
                } ?: "Could not delete contact"
                Result.failure(AuthError.ServerError(msg))
            }
        } catch (e: IOException) {
            Result.failure(AuthError.NetworkError)
        } catch (e: Exception) {
            Result.failure(AuthError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private suspend fun <T> safeCall(
        call: suspend () -> Response<T>
    ): Result<T> = try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.success(body)
            } else {
                Result.failure(AuthError.ServerError("Empty response from server"))
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val parsed = errorBody?.let {
                try { json.decodeFromString<ApiError>(it) } catch (_: Exception) { null }
            }
            val error = when (response.code()) {
                401 -> AuthError.InvalidCredentials
                400, 409 -> AuthError.ValidationError(parsed?.message ?: "Validation failed")
                else -> AuthError.ServerError(parsed?.message ?: "Server error: ${response.code()}")
            }
            Result.failure(error)
        }
    } catch (e: IOException) {
        Result.failure(AuthError.NetworkError)
    } catch (e: Exception) {
        Result.failure(AuthError.UnknownError(e.message ?: "Unknown error"))
    }
}

// -------------------------------------------------------------------------
// Mapping extensions
// -------------------------------------------------------------------------

fun ContactEntity.toDto() = ContactDto(
    id = id, name = name, phone = phone,
    relationship = relationship, isNearby = isNearby, userId = userId,
    createdAt = "", updatedAt = ""
)

fun ContactDto.toEntity() = ContactEntity(
    id = id, name = name, phone = phone,
    relationship = relationship, isNearby = isNearby, userId = userId
)
