package com.as307.aryaa.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface SessionEvent {
    object SessionExpired : SessionEvent
}

object SessionManager {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun triggerSessionExpired() {
        _events.tryEmit(SessionEvent.SessionExpired)
    }
}
