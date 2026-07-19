package com.as307.aryaa.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class DeadZonePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_ACTIVE_CHECKIN_ID  = stringPreferencesKey("deadzone_active_checkin_id")
        private val KEY_EXPECTED_BACK_AT   = stringPreferencesKey("deadzone_expected_back_at")
        private val KEY_GRACE_PERIOD_END   = stringPreferencesKey("deadzone_grace_period_end")
        private val KEY_MODE               = stringPreferencesKey("deadzone_mode")
        private val KEY_DESTINATION        = stringPreferencesKey("deadzone_destination")
        private val KEY_INTERVAL_MINUTES   = stringPreferencesKey("deadzone_interval_minutes")
        private val KEY_LOCATION_SHARE_SESSION_ID = stringPreferencesKey("deadzone_location_share_session_id")
    }

    open suspend fun getActiveCheckInId(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_ACTIVE_CHECKIN_ID]
        }.first()
    }

    open suspend fun setActiveCheckInId(id: String?) {
        dataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(KEY_ACTIVE_CHECKIN_ID)
            } else {
                preferences[KEY_ACTIVE_CHECKIN_ID] = id
            }
        }
    }

    open suspend fun getExpectedBackAt(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_EXPECTED_BACK_AT]
        }.first()
    }

    open suspend fun setExpectedBackAt(timestamp: String?) {
        dataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(KEY_EXPECTED_BACK_AT)
            } else {
                preferences[KEY_EXPECTED_BACK_AT] = timestamp
            }
        }
    }

    open suspend fun getGracePeriodEnd(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_GRACE_PERIOD_END]
        }.first()
    }

    open suspend fun setGracePeriodEnd(timestamp: String?) {
        dataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(KEY_GRACE_PERIOD_END)
            } else {
                preferences[KEY_GRACE_PERIOD_END] = timestamp
            }
        }
    }

    open suspend fun getMode(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_MODE]
        }.first()
    }

    open suspend fun setMode(mode: String?) {
        dataStore.edit { preferences ->
            if (mode == null) {
                preferences.remove(KEY_MODE)
            } else {
                preferences[KEY_MODE] = mode
            }
        }
    }

    open suspend fun getDestination(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_DESTINATION]
        }.first()
    }

    open suspend fun setDestination(destination: String?) {
        dataStore.edit { preferences ->
            if (destination == null) {
                preferences.remove(KEY_DESTINATION)
            } else {
                preferences[KEY_DESTINATION] = destination
            }
        }
    }

    open suspend fun getIntervalMinutes(): Int? {
        return dataStore.data.map { preferences ->
            preferences[KEY_INTERVAL_MINUTES]?.toIntOrNull()
        }.first()
    }

    open suspend fun setIntervalMinutes(minutes: Int?) {
        dataStore.edit { preferences ->
            if (minutes == null) {
                preferences.remove(KEY_INTERVAL_MINUTES)
            } else {
                preferences[KEY_INTERVAL_MINUTES] = minutes.toString()
            }
        }
    }

    open suspend fun getLocationShareSessionId(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_LOCATION_SHARE_SESSION_ID]
        }.first()
    }

    open suspend fun setLocationShareSessionId(id: String?) {
        dataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(KEY_LOCATION_SHARE_SESSION_ID)
            } else {
                preferences[KEY_LOCATION_SHARE_SESSION_ID] = id
            }
        }
    }

    open suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACTIVE_CHECKIN_ID)
            preferences.remove(KEY_EXPECTED_BACK_AT)
            preferences.remove(KEY_GRACE_PERIOD_END)
            preferences.remove(KEY_MODE)
            preferences.remove(KEY_DESTINATION)
            preferences.remove(KEY_INTERVAL_MINUTES)
            preferences.remove(KEY_LOCATION_SHARE_SESSION_ID)
        }
    }
}
