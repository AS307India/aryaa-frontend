package com.as307.aryaa.data.repository

import com.as307.aryaa.data.remote.dto.ContactDto
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {

    /**
     * Returns a Flow that emits cached contacts immediately, then refreshes from
     * the network and emits the updated list. Uses Room as the source of truth.
     */
    fun getContacts(): Flow<List<ContactDto>>

    suspend fun addContact(name: String, phone: String, relationship: String, isNearby: String = "SOMETIMES"): Result<ContactDto>

    suspend fun removeContact(id: String): Result<Unit>
}
