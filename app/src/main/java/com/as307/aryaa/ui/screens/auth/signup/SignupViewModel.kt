package com.as307.aryaa.ui.screens.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    sealed interface UiState {
        object Idle : UiState
        object Loading : UiState
        object Success : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
    private val phoneRegex = "^(?:\\+91)?[6-9]\\d{9}$".toRegex()

    fun signup(name: String, email: String, phone: String, password: String) {
        if (name.isBlank()) {
            _uiState.value = UiState.Error("Name is required.")
            return
        }
        if (email.isBlank()) {
            _uiState.value = UiState.Error("Email is required.")
            return
        }
        if (!email.matches(emailRegex)) {
            _uiState.value = UiState.Error("Please enter a valid email address.")
            return
        }
        if (phone.isBlank()) {
            _uiState.value = UiState.Error("Phone number is required.")
            return
        }
        if (!phone.matches(phoneRegex)) {
            _uiState.value = UiState.Error("Please enter a valid 10-digit Indian phone number.")
            return
        }
        if (password.length < 8) {
            _uiState.value = UiState.Error("Password must be at least 8 characters long.")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.signup(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                password = password
            )
                .onSuccess {
                    _uiState.value = UiState.Success
                }
                .onFailure { exception ->
                    val userMessage = exception.message ?: "Sign up failed. Please try again."
                    _uiState.value = UiState.Error(userMessage)
                }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
