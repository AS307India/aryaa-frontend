package com.as307.aryaa.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ProfilePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_SOS_HOLD_DURATION = intPreferencesKey("sos_hold_duration")
        private val KEY_VOLUME_BUTTON_TRIGGER = booleanPreferencesKey("volume_button_trigger")
        private val KEY_OFFLINE_SMS_ALERTS = booleanPreferencesKey("offline_sms_alerts")
    }

    open suspend fun getSosHoldDuration(): Int {
        return dataStore.data.map { preferences ->
            preferences[KEY_SOS_HOLD_DURATION] ?: 3
        }.first()
    }

    open fun getSosHoldDurationFlow(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[KEY_SOS_HOLD_DURATION] ?: 3
        }
    }

    open suspend fun setSosHoldDuration(duration: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_SOS_HOLD_DURATION] = duration
        }
    }

    open suspend fun getVolumeButtonTrigger(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[KEY_VOLUME_BUTTON_TRIGGER] ?: true
        }.first()
    }

    open fun getVolumeButtonTriggerFlow(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_VOLUME_BUTTON_TRIGGER] ?: true
        }
    }

    open suspend fun setVolumeButtonTrigger(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_VOLUME_BUTTON_TRIGGER] = enabled
        }
    }

    open suspend fun getOfflineSmsAlert(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[KEY_OFFLINE_SMS_ALERTS] ?: true
        }.first()
    }

    open fun getOfflineSmsAlertFlow(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_OFFLINE_SMS_ALERTS] ?: true
        }
    }

    // Spec says: "Offline SMS Alerts" reads `profilePreferences.getOfflineSmsAlerts` or similar
    open suspend fun getOfflineSmsAlerts(): Boolean = getOfflineSmsAlert()

    open suspend fun setOfflineSmsAlert(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OFFLINE_SMS_ALERTS] = enabled
        }
    }
}
