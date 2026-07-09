package com.as307.aryaa.di

import com.as307.aryaa.data.local.TokenManager
import com.as307.aryaa.data.local.TokenStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindTokenStorage(
        tokenManager: TokenManager
    ): TokenStorage
}
