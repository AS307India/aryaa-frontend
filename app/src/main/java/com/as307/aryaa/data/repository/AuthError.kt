package com.as307.aryaa.data.repository

sealed class AuthError(val userMessage: String) : Throwable(userMessage) {
    object NetworkError : AuthError("Connection failed. Please check your internet connection and try again.")
    object InvalidCredentials : AuthError("Invalid email or password. Please try again.")
    class ServerError(message: String = "Server error. Please try again later.") : AuthError(message)
    class ValidationError(message: String) : AuthError(message)
    class UnknownError(message: String = "An unexpected error occurred. Please try again.") : AuthError(message)
}
