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

    open suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACTIVE_CHECKIN_ID)
            preferences.remove(KEY_EXPECTED_BACK_AT)
            preferences.remove(KEY_GRACE_PERIOD_END)
        }
    }
}
