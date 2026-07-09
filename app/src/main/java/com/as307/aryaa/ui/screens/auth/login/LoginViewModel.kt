package com.as307.aryaa.ui.screens.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.as307.aryaa.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
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

    fun login(email: String, password: String) {
        if (email.isBlank()) {
            _uiState.value = UiState.Error("Email is required.")
            return
        }
        if (!email.matches(emailRegex)) {
            _uiState.value = UiState.Error("Please enter a valid email address.")
            return
        }
        if (password.isBlank()) {
            _uiState.value = UiState.Error("Password is required.")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.login(email.trim(), password)
                .onSuccess {
                    _uiState.value = UiState.Success
                }
                .onFailure { exception ->
                    val userMessage = exception.message ?: "Login failed. Please try again."
                    _uiState.value = UiState.Error(userMessage)
                }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
