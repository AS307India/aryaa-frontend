package com.as307.aryaa.ui.screens.medicalid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.local.MedicalIdPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicalIdViewModel @Inject constructor(
    private val medicalIdPreferences: MedicalIdPreferences,
    private val notifier: MedicalIdNotifier
) : ViewModel() {

    private val _bloodType = MutableStateFlow<String?>("Unknown")
    val bloodType: StateFlow<String?> = _bloodType.asStateFlow()

    private val _allergies = MutableStateFlow<String?>("")
    val allergies: StateFlow<String?> = _allergies.asStateFlow()

    private val _medications = MutableStateFlow<String?>("")
    val medications: StateFlow<String?> = _medications.asStateFlow()

    private val _conditions = MutableStateFlow<String?>("")
    val conditions: StateFlow<String?> = _conditions.asStateFlow()

    private val _emergencyContactName = MutableStateFlow<String?>("")
    val emergencyContactName: StateFlow<String?> = _emergencyContactName.asStateFlow()

    private val _emergencyContactPhone = MutableStateFlow<String?>("")
    val emergencyContactPhone: StateFlow<String?> = _emergencyContactPhone.asStateFlow()

    private val _organDonor = MutableStateFlow(false)
    val organDonor: StateFlow<Boolean> = _organDonor.asStateFlow()

    private val _notes = MutableStateFlow<String?>("")
    val notes: StateFlow<String?> = _notes.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    companion object {
        private const val CHANNEL_ID = "aryaa_medical_id"
        private const val NOTIFICATION_ID = 2002
    }

    init {
        loadMedicalId()
    }

    private fun loadMedicalId() {
        viewModelScope.launch {
            _bloodType.value = medicalIdPreferences.getBloodType() ?: "Unknown"
            _allergies.value = medicalIdPreferences.getAllergies() ?: ""
            _medications.value = medicalIdPreferences.getMedications() ?: ""
            _conditions.value = medicalIdPreferences.getConditions() ?: ""
            _emergencyContactName.value = medicalIdPreferences.getEmergencyContactName() ?: ""
            _emergencyContactPhone.value = medicalIdPreferences.getEmergencyContactPhone() ?: ""
            _organDonor.value = medicalIdPreferences.getOrganDonor()
            _notes.value = medicalIdPreferences.getNotes() ?: ""
        }
    }

    fun setBloodType(value: String?) { _bloodType.value = value }
    fun setAllergies(value: String?) { _allergies.value = value }
    fun setMedications(value: String?) { _medications.value = value }
    fun setConditions(value: String?) { _conditions.value = value }
    fun setEmergencyContactName(value: String?) { _emergencyContactName.value = value }
    fun setEmergencyContactPhone(value: String?) { _emergencyContactPhone.value = value }
    fun setOrganDonor(value: Boolean) { _organDonor.value = value }
    fun setNotes(value: String?) { _notes.value = value }

    fun saveMedicalId(
        blood: String?,
        allerg: String?,
        meds: String?,
        conds: String?,
        contactName: String?,
        contactPhone: String?,
        donor: Boolean,
        noteTexts: String?
    ) {
        viewModelScope.launch {
            medicalIdPreferences.setBloodType(blood)
            medicalIdPreferences.setAllergies(allerg)
            medicalIdPreferences.setMedications(meds)
            medicalIdPreferences.setConditions(conds)
            medicalIdPreferences.setEmergencyContactName(contactName)
            medicalIdPreferences.setEmergencyContactPhone(contactPhone)
            medicalIdPreferences.setOrganDonor(donor)
            medicalIdPreferences.setNotes(noteTexts)

            _isSaved.value = true

            // Trigger notification update based on non-null fields
            val hasData = !blood.isNullOrBlank() && blood != "Unknown" ||
                    !allerg.isNullOrBlank() ||
                    !meds.isNullOrBlank() ||
                    !conds.isNullOrBlank() ||
                    !contactName.isNullOrBlank() ||
                    !contactPhone.isNullOrBlank() ||
                    donor ||
                    !noteTexts.isNullOrBlank()

            if (hasData) {
                showMedicalIdNotification()
            } else {
                cancelMedicalIdNotification()
            }
        }
    }

    fun resetSavedState() {
        _isSaved.value = false
    }

    private fun showMedicalIdNotification() {
        notifier.showNotification()
    }

    private fun cancelMedicalIdNotification() {
        notifier.cancelNotification()
    }
}
