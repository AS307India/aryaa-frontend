package com.as307.aryaa.data.local

interface TokenStorage {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
    suspend fun saveUserProfile(name: String, email: String, phone: String)
    suspend fun getUserName(): String?
    suspend fun getUserEmail(): String?
    suspend fun getUserPhone(): String?
    suspend fun clearUserProfile()
}
