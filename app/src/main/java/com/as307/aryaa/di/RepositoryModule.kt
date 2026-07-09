package com.as307.aryaa.di

import com.as307.aryaa.data.repository.AuthRepository
import com.as307.aryaa.data.repository.AuthRepositoryImpl
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.data.repository.ContactsRepositoryImpl
import com.as307.aryaa.data.repository.SosRepository
import com.as307.aryaa.data.repository.SosRepositoryImpl
import com.as307.aryaa.data.repository.FcmTokenRepository
import com.as307.aryaa.data.repository.FcmTokenRepositoryImpl
import com.as307.aryaa.data.repository.DeadZoneRepository
import com.as307.aryaa.data.repository.DeadZoneRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindContactsRepository(
        contactsRepositoryImpl: ContactsRepositoryImpl
    ): ContactsRepository

    @Binds
    @Singleton
    abstract fun bindSosRepository(
        sosRepositoryImpl: SosRepositoryImpl
    ): SosRepository

    @Binds
    @Singleton
    abstract fun bindFcmTokenRepository(
        fcmTokenRepositoryImpl: FcmTokenRepositoryImpl
    ): FcmTokenRepository

    @Binds
    @Singleton
    abstract fun bindDeadZoneRepository(
        deadZoneRepositoryImpl: DeadZoneRepositoryImpl
    ): DeadZoneRepository
}

