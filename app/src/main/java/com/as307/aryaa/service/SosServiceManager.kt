package com.as307.aryaa.service

import android.content.Context
import android.content.Intent
import com.as307.aryaa.data.remote.dto.SosContactSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SosServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Fired when the user successfully completes the hold-countdown in SosViewModel
    // and the foreground service needs a navigation signal to AryaaNavGraph.
    private val _sosTriggerEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sosTriggerEvents: SharedFlow<Unit> = _sosTriggerEvents.asSharedFlow()

    // Fired by MainActivity when the volume-button triple-press is detected.
    // SosViewModel observes this and calls onCountdownComplete() on itself,
    // keeping all API-call and state-machine logic inside the ViewModel.
    private val _volumeTriggerRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val volumeTriggerRequests: SharedFlow<Unit> = _volumeTriggerRequests.asSharedFlow()

    // Fired by SosService when the "I'm Safe" notification action cancels the SOS.
    // SosViewModel collects this to reset the UI state to Idle and clear the persisted
    // event — without this, the UI would stay in Active state after notification dismissal.
    private val _sosCancelledEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sosCancelledEvents: SharedFlow<Unit> = _sosCancelledEvents.asSharedFlow()

    open fun startSos(sosEventId: String, contacts: List<SosContactSnapshot>, w3wAddress: String? = null) {
        val intent = Intent(context, SosService::class.java).apply {
            action = SosService.ACTION_START_SOS
            putExtra(SosService.EXTRA_SOS_EVENT_ID, sosEventId)
            putExtra(SosService.EXTRA_CONTACTS, ArrayList(contacts))
            putExtra(SosService.EXTRA_W3W_ADDRESS, w3wAddress)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }


    open fun cancelSos() {
        val intent = Intent(context, SosService::class.java).apply {
            action = SosService.ACTION_CANCEL_SOS
        }
        context.startService(intent)
    }

    open fun duressCancel() {
        val intent = Intent(context, SosService::class.java).apply {
            action = SosService.ACTION_DURESS_CANCEL
        }
        context.startService(intent)
    }

    fun emitTriggerEvent() {
        _sosTriggerEvents.tryEmit(Unit)
    }

    /** Called by MainActivity when triple-press volume trigger fires. */
    fun emitVolumeRequest() {
        _volumeTriggerRequests.tryEmit(Unit)
    }

    /** Called by SosService after a successful cancel API call so the ViewModel
     *  can reset its UI state back to Idle. */
    fun emitCancelledEvent() {
        _sosCancelledEvents.tryEmit(Unit)
    }
}
