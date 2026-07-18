package com.as307.aryaa.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SafetyLimitsPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_SAFETY_LIMITS_ACKNOWLEDGED = booleanPreferencesKey("safety_limits_acknowledged")
    }

    open suspend fun isSafetyLimitsAcknowledged(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[KEY_SAFETY_LIMITS_ACKNOWLEDGED] ?: false
        }.first()
    }

    open fun isSafetyLimitsAcknowledgedFlow(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_SAFETY_LIMITS_ACKNOWLEDGED] ?: false
        }
    }

    open suspend fun setSafetyLimitsAcknowledged(acknowledged: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SAFETY_LIMITS_ACKNOWLEDGED] = acknowledged
        }
    }
}
