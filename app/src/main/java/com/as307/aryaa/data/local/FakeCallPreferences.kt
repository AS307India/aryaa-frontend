package com.as307.aryaa.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class FakeCallPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_CALLER_NAME = stringPreferencesKey("caller_name")
        private val KEY_CALLER_DELAY = intPreferencesKey("caller_delay")
    }

    open suspend fun getCallerName(): String {
        return dataStore.data.map { preferences ->
            preferences[KEY_CALLER_NAME] ?: "Maa"
        }.first()
    }

    open suspend fun setCallerName(name: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CALLER_NAME] = name
        }
    }

    open suspend fun getCallerDelay(): Int {
        return dataStore.data.map { preferences ->
            preferences[KEY_CALLER_DELAY] ?: 5
        }.first()
    }

    open suspend fun setCallerDelay(delay: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_CALLER_DELAY] = delay
        }
    }
}
