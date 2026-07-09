package com.as307.aryaa.service.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.as307.aryaa.data.repository.DeadZoneRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class DeadZoneCheckInWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface DeadZoneWorkerEntryPoint {
        fun deadZoneRepository(): DeadZoneRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DeadZoneWorkerEntryPoint::class.java
        )
        val repo = entryPoint.deadZoneRepository()
        return executeWork(repo)
    }

    companion object {
        /**
         * Core work logic extracted to a companion object so it can be tested
         * in pure JVM unit tests without a real Context or Application.
         *
         * Always returns Result.success() — network failures must NOT cause
         * WorkManager to retry indefinitely. The backend piggyback hook on the
         * next authenticated request is the safety net for missed check-ins.
         */
        suspend fun executeWork(repo: DeadZoneRepository): Result {
            try {
                // Trigger status check — backend validates and alerts contacts
                // if the session is PENDING and the grace period has expired.
                repo.getStatus()
            } catch (_: Exception) {
                // Swallow all exceptions. Network failures are non-fatal here.
            }
            return Result.success()
        }
    }
}
