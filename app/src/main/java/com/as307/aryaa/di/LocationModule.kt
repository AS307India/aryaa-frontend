package com.as307.aryaa.di

import com.as307.aryaa.data.location.LocationProvider
import com.as307.aryaa.data.location.LocationProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationProvider(
        locationProviderImpl: LocationProviderImpl
    ): LocationProvider
}
