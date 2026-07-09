package com.as307.aryaa.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.repository.AuthError
import com.as307.aryaa.data.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val contacts: List<ContactDto>) : UiState()
        object Empty : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Tracks which contact id is currently being deleted (for loading indicator)
    private val _deletingId = MutableStateFlow<String?>(null)
    val deletingId: StateFlow<String?> = _deletingId.asStateFlow()

    // Error/success events for one-shot snackbar
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        observeContacts()
    }

    private fun observeContacts() {
        contactsRepository.getContacts()
            .onEach { contacts ->
                _uiState.value = if (contacts.isEmpty()) UiState.Empty else UiState.Success(contacts)
            }
            .catch { _uiState.value = UiState.Error("Failed to load contacts") }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        _uiState.value = UiState.Loading
        observeContacts()
    }

    fun addContact(name: String, phone: String, relationship: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = contactsRepository.addContact(name, phone, relationship)
            result.fold(
                onSuccess = { onDone(true) },
                onFailure = { error ->
                    val msg = when (error) {
                        is AuthError.ValidationError -> error.message
                        is AuthError.NetworkError -> "No internet connection"
                        is AuthError.ServerError -> error.message
                        else -> "Could not add contact"
                    }
                    _actionMessage.value = msg
                    onDone(false)
                }
            )
        }
    }

    /**
     * Pessimistic delete: we wait for backend confirmation before removing from UI.
     * Reason: contacts are safety-critical data. A premature removal from the list
     * that then fails silently could leave the user believing a contact is active
     * when it was never actually deleted (or worse, an optimistic add rolled back).
     */
    fun removeContact(id: String) {
        viewModelScope.launch {
            _deletingId.value = id
            val result = contactsRepository.removeContact(id)
            _deletingId.value = null
            result.onFailure { error ->
                val msg = when (error) {
                    is AuthError.NetworkError -> "No internet — contact not deleted"
                    else -> "Could not delete contact"
                }
                _actionMessage.value = msg
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun onSmsPermissionResult(isGranted: Boolean) {
        val prefs = context.getSharedPreferences("aryaa_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("sms_permission_denied", !isGranted).apply()
    }
}
