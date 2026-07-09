package com.as307.aryaa.di

import com.as307.aryaa.service.SmsDispatcher
import com.as307.aryaa.service.SmsDispatcherImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindSmsDispatcher(
        smsDispatcherImpl: SmsDispatcherImpl
    ): SmsDispatcher

    @Binds
    @Singleton
    abstract fun bindMedicalIdNotifier(
        medicalIdNotifierImpl: com.as307.aryaa.ui.screens.medicalid.MedicalIdNotifierImpl
    ): com.as307.aryaa.ui.screens.medicalid.MedicalIdNotifier
}
