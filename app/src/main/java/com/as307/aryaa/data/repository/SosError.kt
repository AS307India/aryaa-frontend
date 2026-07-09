package com.as307.aryaa.data.repository

sealed class SosError(val userMessage: String) : Throwable(userMessage) {
    object NetworkError : SosError("Connection failed. Please check your internet connection and try again.")
    object Unauthorized : SosError("Session expired. Please log in again.")
    object Forbidden : SosError("Permission denied for this SOS event.")
    object AlreadyActive : SosError("Your SOS is already active. Tap status to view details.")
    object NotFound : SosError("SOS event not found.")
    class ServerError(message: String = "Server error. Please try again later.") : SosError(message)
    class UnknownError(message: String = "An unexpected error occurred. Please try again.") : SosError(message)
}
