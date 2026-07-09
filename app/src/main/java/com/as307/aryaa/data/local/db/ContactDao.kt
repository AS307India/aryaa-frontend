package com.as307.aryaa.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Query("SELECT * FROM contacts ORDER BY rowid ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsOnce(): List<ContactEntity>

    // Return Int (number of rows deleted) to avoid Room KSP "unexpected jvm signature V" bug
    // with Unit-returning @Query methods in Room 2.6.x
    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: String): Int

    @Query("DELETE FROM contacts")
    suspend fun clearAll(): Int
}
