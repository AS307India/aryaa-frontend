package com.as307.aryaa.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ActiveLocationShare(
    val sessionId: String,
    val shareUrl: String,
    val expiresAt: String,
    val contactCount: Int
)

@Singleton
class LocationShareManager @Inject constructor() {
    private val _activeShare = MutableStateFlow<ActiveLocationShare?>(null)
    val activeShare: StateFlow<ActiveLocationShare?> = _activeShare.asStateFlow()

    fun setActiveShare(share: ActiveLocationShare) {
        _activeShare.value = share
    }

    fun clearActiveShare() {
        _activeShare.value = null
    }

    fun isActive(): Boolean = _activeShare.value != null
}
