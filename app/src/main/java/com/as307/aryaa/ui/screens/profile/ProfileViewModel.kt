package com.as307.aryaa.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.local.ProfilePreferences
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.repository.AuthRepository
import com.as307.aryaa.data.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileNavigationEvent {
    object NavigateToLogin : ProfileNavigationEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val contactsRepository: ContactsRepository,
    private val authRepository: AuthRepository,
    private val profilePreferences: ProfilePreferences
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userPhone = MutableStateFlow("")
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _contactCount = MutableStateFlow(0)
    val contactCount: StateFlow<Int> = _contactCount.asStateFlow()

    private val _sosHoldDuration = MutableStateFlow(3)
    val sosHoldDuration: StateFlow<Int> = _sosHoldDuration.asStateFlow()

    private val _volumeButtonTrigger = MutableStateFlow(true)
    val volumeButtonTrigger: StateFlow<Boolean> = _volumeButtonTrigger.asStateFlow()

    private val _offlineSmsAlerts = MutableStateFlow(true)
    val offlineSmsAlerts: StateFlow<Boolean> = _offlineSmsAlerts.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ProfileNavigationEvent>()
    val navigationEvents: SharedFlow<ProfileNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        loadUserProfile()
        loadPreferences()
        observeContacts()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _userName.value = tokenStorage.getUserName() ?: ""
            _userEmail.value = tokenStorage.getUserEmail() ?: ""
            _userPhone.value = tokenStorage.getUserPhone() ?: ""
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            profilePreferences.getSosHoldDurationFlow().collect { duration ->
                _sosHoldDuration.value = duration
            }
        }
        viewModelScope.launch {
            profilePreferences.getVolumeButtonTriggerFlow().collect { enabled ->
                _volumeButtonTrigger.value = enabled
            }
        }
        viewModelScope.launch {
            profilePreferences.getOfflineSmsAlertFlow().collect { enabled ->
                _offlineSmsAlerts.value = enabled
            }
        }
    }

    private fun observeContacts() {
        viewModelScope.launch {
            contactsRepository.getContacts().collect { contactsList ->
                _contactCount.value = contactsList.size
            }
        }
    }

    fun updateSosHoldDuration(duration: Int) {
        viewModelScope.launch {
            profilePreferences.setSosHoldDuration(duration)
        }
    }

    fun updateVolumeButtonTrigger(enabled: Boolean) {
        viewModelScope.launch {
            profilePreferences.setVolumeButtonTrigger(enabled)
        }
    }

    fun updateOfflineSmsAlerts(enabled: Boolean) {
        viewModelScope.launch {
            profilePreferences.setOfflineSmsAlert(enabled)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
            _navigationEvents.emit(ProfileNavigationEvent.NavigateToLogin)
        }
    }
}
