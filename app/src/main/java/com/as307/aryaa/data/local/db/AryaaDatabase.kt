package com.as307.aryaa.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class, ActiveSosEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AryaaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun activeSosDao(): ActiveSosDao
}
