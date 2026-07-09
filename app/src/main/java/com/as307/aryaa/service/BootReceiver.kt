package com.as307.aryaa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.as307.aryaa.data.local.db.ActiveSosDao
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Listens for system boot completion broadcasts.
 * 
 * If the device reboots while an SOS event is active in the local database,
 * this receiver automatically restarts the foreground service to maintain
 * tracking and user status notifications.
 * 
 * This does NOT send duplicate SMS alerts or call the backend trigger API.
 */
class BootReceiver : BroadcastReceiver() {

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface BootReceiverEntryPoint {
        fun activeSosDao(): ActiveSosDao
        fun sosServiceManager(): SosServiceManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("SOS_DEBUG", "boot receiver fired")
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BootReceiverEntryPoint::class.java
                )
                val dao = entryPoint.activeSosDao()
                val manager = entryPoint.sosServiceManager()

                val activeSos = dao.getActiveSos()
                if (activeSos != null) {
                    val contacts = try {
                        Json.decodeFromString<List<SosContactSnapshot>>(activeSos.contactsJson)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    // Start the service without calling the repository trigger/SMS path
                    manager.startSos(
                        sosEventId = activeSos.sosEventId,
                        contacts = contacts,
                        w3wAddress = activeSos.w3wAddress
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Error restoring SOS service on boot", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
