package com.as307.aryaa.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.as307.aryaa.data.local.db.ActiveSosDao
import com.as307.aryaa.data.local.db.AryaaDatabase
import com.as307.aryaa.data.local.db.ContactDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `active_sos` (" +
                "`sosEventId` TEXT NOT NULL, " +
                "`triggeredAt` TEXT NOT NULL, " +
                "`w3wAddress` TEXT, " +
                "`contactsJson` TEXT NOT NULL, " +
                "`latitude` REAL, " +
                "`longitude` REAL, " +
                "PRIMARY KEY(`sosEventId`)" +
                ")"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `active_sos` ADD COLUMN `accuracy` REAL")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AryaaDatabase {
        return Room.databaseBuilder(
            context,
            AryaaDatabase::class.java,
            "aryaa_database"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: AryaaDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    @Singleton
    fun provideActiveSosDao(database: AryaaDatabase): ActiveSosDao {
        return database.activeSosDao()
    }
}

