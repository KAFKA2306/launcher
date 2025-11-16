package com.kafka.launcher.data.store

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kafka.launcher.config.GeminiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GeminiApiKeyStore(context: Context) {
    private val appContext = context.applicationContext
    private val masterKey = MasterKey.Builder(appContext, GeminiConfig.apiMasterKeyAlias)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val preferences = EncryptedSharedPreferences.create(
        appContext,
        GeminiConfig.apiKeyStoreFileName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val store = MutableStateFlow(read())
    val data: Flow<String> = store.asStateFlow()

    fun current(): String = store.value

    suspend fun save(value: String) {
        write(value)
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            preferences.edit().remove(GeminiConfig.apiKeyPreferenceKey).apply()
        }
        store.value = ""
    }

    private suspend fun write(value: String) {
        withContext(Dispatchers.IO) {
            preferences.edit().putString(GeminiConfig.apiKeyPreferenceKey, value).apply()
        }
        store.value = value
    }

    private fun read(): String {
        return preferences.getString(GeminiConfig.apiKeyPreferenceKey, "").orEmpty()
    }
}
