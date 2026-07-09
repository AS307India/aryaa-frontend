package com.as307.aryaa.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenStorage {
    companion object {
        private const val PREF_FILE_NAME = "aryaa_secure_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    override suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(KEY_JWT_TOKEN, null)
    }

    override suspend fun clearToken() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().remove(KEY_JWT_TOKEN).apply()
    }

    override suspend fun saveUserProfile(name: String, email: String, phone: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PHONE, phone)
            .apply()
    }

    override suspend fun getUserName(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(KEY_USER_NAME, null)
    }

    override suspend fun getUserEmail(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    override suspend fun getUserPhone(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(KEY_USER_PHONE, null)
    }

    override suspend fun clearUserProfile() = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHONE)
            .apply()
    }
}
