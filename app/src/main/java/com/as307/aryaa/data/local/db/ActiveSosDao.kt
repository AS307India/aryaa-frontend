package com.as307.aryaa.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActiveSosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveSos(entity: ActiveSosEntity)

    @Query("SELECT * FROM active_sos LIMIT 1")
    suspend fun getActiveSos(): ActiveSosEntity?

    @Query("DELETE FROM active_sos")
    suspend fun clearActiveSos()
}
