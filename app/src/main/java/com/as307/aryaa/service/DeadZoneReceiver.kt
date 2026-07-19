package com.as307.aryaa.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.as307.aryaa.data.local.DeadZonePreferences
import com.as307.aryaa.data.repository.DeadZoneRepository
import com.as307.aryaa.service.work.DeadZoneCheckInWorker
import com.as307.aryaa.service.work.DeadZoneReminderWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.TimeUnit

class DeadZoneReceiver : BroadcastReceiver() {

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface DeadZoneReceiverEntryPoint {
        fun deadZoneRepository(): DeadZoneRepository
        fun deadZonePreferences(): DeadZonePreferences
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CONFIRM_SAFE) {
            val checkInId = intent.getStringExtra(EXTRA_CHECKIN_ID) ?: return
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1004)

            // Clear the notification instantly
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationId)

            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DeadZoneReceiverEntryPoint::class.java
            )
            val repo = entryPoint.deadZoneRepository()
            val prefs = entryPoint.deadZonePreferences()

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repo.checkIn(checkInId).onSuccess { response ->
                        android.util.Log.d("DEADZONE_RECEIVER", "Background check-in succeeded: status=${response.status}")
                        if (response.status == "PENDING") {
                            // HEARTBEAT loop is continuing: schedule the next reminder & server-side trigger scan
                            val expectedBackAtIso = response.expectedBackAt ?: ""
                            val gracePeriodEndIso = response.gracePeriodEnd ?: ""
                            
                            scheduleHeartbeatJobs(context, expectedBackAtIso, gracePeriodEndIso, checkInId)
                        } else {
                            // Session completed: cancel any pending local notifications/work and stop LocationShareService
                            cancelWorkManagerJobs(context, checkInId)
                            stopLocationShareService(context)
                        }
                    }.onFailure { err ->
                        android.util.Log.e("DEADZONE_RECEIVER", "Background check-in API call failed", err)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DEADZONE_RECEIVER", "Exception inside DeadZoneReceiver coroutine", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun scheduleHeartbeatJobs(context: Context, expectedBackAtIso: String, gracePeriodEndIso: String, checkInId: String) {
        try {
            val wm = WorkManager.getInstance(context)

            // 1. Next notification reminder (at expectedBackAt)
            val expectedMillis = Instant.parse(expectedBackAtIso).toEpochMilli()
            val reminderDelay = maxOf(0L, expectedMillis - System.currentTimeMillis())
            val reminderRequest = OneTimeWorkRequestBuilder<DeadZoneReminderWorker>()
                .setInitialDelay(reminderDelay, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(
                "deadzone_reminder_$checkInId",
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )

            // 2. Next server-side SOS trigger validation scan (at gracePeriodEnd)
            val graceMillis = Instant.parse(gracePeriodEndIso).toEpochMilli()
            val graceDelay = maxOf(0L, graceMillis - System.currentTimeMillis())
            val graceRequest = OneTimeWorkRequestBuilder<DeadZoneCheckInWorker>()
                .setInitialDelay(graceDelay, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(
                "deadzone_escalation_$checkInId",
                ExistingWorkPolicy.REPLACE,
                graceRequest
            )

            android.util.Log.d("DEADZONE_RECEIVER", "Heartbeat rescheduled reminder in $reminderDelay ms, escalation in $graceDelay ms")
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_RECEIVER", "Failed to reschedule heartbeat jobs", e)
        }
    }

    private fun cancelWorkManagerJobs(context: Context, checkInId: String) {
        try {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork("deadzone_reminder_$checkInId")
            wm.cancelUniqueWork("deadzone_escalation_$checkInId")
        } catch (e: Exception) {
            android.util.Log.e("DEADZONE_RECEIVER", "Failed to cancel WorkManager jobs", e)
        }
    }

    private fun stopLocationShareService(context: Context) {
        try {
            val intent = Intent(context, LocationShareService::class.java).apply {
                action = LocationShareService.ACTION_STOP_SHARE
            }
            context.startService(intent)
        } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_CONFIRM_SAFE = "com.as307.aryaa.action.CONFIRM_SAFE"
        const val EXTRA_CHECKIN_ID = "com.as307.aryaa.extra.CHECKIN_ID"
        const val EXTRA_NOTIFICATION_ID = "com.as307.aryaa.extra.NOTIFICATION_ID"
    }
}
