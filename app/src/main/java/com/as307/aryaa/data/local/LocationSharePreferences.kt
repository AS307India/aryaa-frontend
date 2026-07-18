package com.as307.aryaa.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSharePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_SESSION_ID = stringPreferencesKey("location_share_session_id")
        private val KEY_SHARE_TOKEN = stringPreferencesKey("location_share_token")
        private val KEY_SHARE_URL = stringPreferencesKey("location_share_url")
        private val KEY_EXPIRES_AT = stringPreferencesKey("location_share_expires_at")
    }

    suspend fun getSessionId(): String? =
        dataStore.data.map { it[KEY_SESSION_ID] }.first()

    suspend fun getShareToken(): String? =
        dataStore.data.map { it[KEY_SHARE_TOKEN] }.first()

    suspend fun getShareUrl(): String? =
        dataStore.data.map { it[KEY_SHARE_URL] }.first()

    suspend fun getExpiresAt(): String? =
        dataStore.data.map { it[KEY_EXPIRES_AT] }.first()

    fun getSessionIdFlow(): Flow<String?> =
        dataStore.data.map { it[KEY_SESSION_ID] }

    suspend fun saveSession(sessionId: String, shareToken: String, shareUrl: String, expiresAt: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = sessionId
            prefs[KEY_SHARE_TOKEN] = shareToken
            prefs[KEY_SHARE_URL] = shareUrl
            prefs[KEY_EXPIRES_AT] = expiresAt
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_SESSION_ID)
            prefs.remove(KEY_SHARE_TOKEN)
            prefs.remove(KEY_SHARE_URL)
            prefs.remove(KEY_EXPIRES_AT)
        }
    }

    suspend fun hasActiveSession(): Boolean {
        val sessionId = getSessionId() ?: return false
        val expiresAt = getExpiresAt() ?: return false
        return try {
            val expiry = java.time.Instant.parse(expiresAt)
            sessionId.isNotBlank() && java.time.Instant.now().isBefore(expiry)
        } catch (e: Exception) {
            false
        }
    }
}
