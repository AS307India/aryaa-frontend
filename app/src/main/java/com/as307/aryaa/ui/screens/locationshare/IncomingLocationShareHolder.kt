package com.as307.aryaa.ui.screens.locationshare

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class IncomingLocationShare(
    val sessionId: String,
    val sharerName: String,
    val shareUrl: String,
    val durationMinutes: String
)

@Singleton
class IncomingLocationShareHolder @Inject constructor() {
    private val _incoming = MutableStateFlow<IncomingLocationShare?>(null)
    val incoming: StateFlow<IncomingLocationShare?> = _incoming.asStateFlow()

    fun set(data: IncomingLocationShare) {
        _incoming.value = data
    }

    fun clear() {
        _incoming.value = null
    }
}
