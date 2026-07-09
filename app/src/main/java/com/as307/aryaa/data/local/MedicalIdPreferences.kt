package com.as307.aryaa.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MedicalIdPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_BLOOD_TYPE = stringPreferencesKey("medical_id_blood_type")
        private val KEY_ALLERGIES = stringPreferencesKey("medical_id_allergies")
        private val KEY_MEDICATIONS = stringPreferencesKey("medical_id_medications")
        private val KEY_CONDITIONS = stringPreferencesKey("medical_id_conditions")
        private val KEY_EMERGENCY_CONTACT_NAME = stringPreferencesKey("medical_id_emergency_contact_name")
        private val KEY_EMERGENCY_CONTACT_PHONE = stringPreferencesKey("medical_id_emergency_contact_phone")
        private val KEY_ORGAN_DONOR = booleanPreferencesKey("medical_id_organ_donor")
        private val KEY_NOTES = stringPreferencesKey("medical_id_notes")
    }

    open suspend fun getBloodType(): String? = dataStore.data.map { it[KEY_BLOOD_TYPE] }.first()
    open fun getBloodTypeFlow(): Flow<String?> = dataStore.data.map { it[KEY_BLOOD_TYPE] }
    open suspend fun setBloodType(value: String?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(KEY_BLOOD_TYPE) else prefs[KEY_BLOOD_TYPE] = value }
    }

    open suspend fun getAllergies(): String? = dataStore.data.map { it[KEY_ALLERGIES] }.first()
    open fun getAllergiesFlow(): Flow<String?> = dataStore.data.map { it[KEY_ALLERGIES] }
    open suspend fun setAllergies(value: String?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(KEY_ALLERGIES) else prefs[KEY_ALLERGIES] = value }
    }

    open suspend fun getMedications(): String? = dataStore.data.map { it[KEY_MEDICATIONS] }.first()
    open fun getMedicationsFlow(): Flow<String?> = dataStore.data.map { it[KEY_MEDICATIONS] }
    open suspend fun setMedications(value: String?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(KEY_MEDICATIONS) else prefs[KEY_MEDICATIONS] = value }
    }

    open suspend fun getConditions(): String? = dataStore.data.map { it[KEY_CONDITIONS] }.first()
    open fun getConditionsFlow(): Flow<String?> = dataStore.data.map { it[KEY_CONDITIONS] }
    open suspend fun setConditions(value: String?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(KEY_CONDITIONS) else prefs[KEY_CONDITIONS] = value }
    }

    open suspend fun getEmergencyContactName(): String? = dataStore.data.map { it[KEY_EMERGENCY_CONTACT_NAME] }.first()
    open fun getEmergencyContactNameFlow(): Flow<String?> = dataStore.data.map { it[KEY_EMERGENCY_CONTACT_NAME] }
    open suspend fun setEmergencyContactName(value: String?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(KEY_EMERGENCY_CONTACT_NAME) else prefs[KEY_EMERGENCY_CONTACT_NAME] = value }
    }

    open suspend fun getEmergencyContactPhone(): String? = dataStore.data.map { it[KEY_EMERGENCY_CONTACT_PHONE] }.first()
    open fun getEmergencyContactPhoneFlow(): Flow<String?> = dataStore.data.map { it[KEY_EMERGENCY_CONTACT_PHONE] }
    open suspend fun setEmergencyContactPhone(value: String?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(KEY_EMERGENCY_CONTACT_PHONE) else prefs[KEY_EMERGENCY_CONTACT_PHONE] = value }
    }

    open suspend fun getOrganDonor(): Boolean = dataStore.data.map { it[KEY_ORGAN_DONOR] ?: false }.first()
    open fun getOrganDonorFlow(): Flow<Boolean> = dataStore.data.map { it[KEY_ORGAN_DONOR] ?: false }
    open suspend fun setOrganDonor(value: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ORGAN_DONOR] = value }
    }

    open suspend fun getNotes(): String? = dataStore.data.map { it[KEY_NOTES] }.first()
    open fun getNotesFlow(): Flow<String?> = dataStore.data.map { it[KEY_NOTES] }
    open suspend fun setNotes(value: String?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(KEY_NOTES) else prefs[KEY_NOTES] = value }
    }
}
