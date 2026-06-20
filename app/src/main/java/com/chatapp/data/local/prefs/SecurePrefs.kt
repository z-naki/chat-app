package com.chatapp.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.chatapp.data.local.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SecurePrefs @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "chat_app_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // --- API Key (double-encrypted: EncryptedPrefs + CryptoManager envelope) ---

    fun putApiKey(provider: String, key: String) {
        val envelope = cryptoManager.encrypt(key)
        encryptedPrefs.edit().putString("api_key_$provider", envelope).apply()
    }

    fun getApiKey(provider: String): String? {
        val envelope = encryptedPrefs.getString("api_key_$provider", null) ?: return null
        return try {
            cryptoManager.decrypt(envelope)
        } catch (e: Exception) {
            android.util.Log.w("SecurePrefs", "Failed to decrypt API key for $provider")
            null
        }
    }

    fun deleteApiKey(provider: String) {
        encryptedPrefs.edit().remove("api_key_$provider").apply()
    }

    // --- Provider Base URL (EncryptedSharedPreferences only, no envelope) ---

    fun putProviderBaseUrl(provider: String, url: String) {
        encryptedPrefs.edit().putString("base_url_$provider", url).apply()
    }

    fun getProviderBaseUrl(provider: String): String {
        return encryptedPrefs.getString("base_url_$provider", null) ?: ""
    }

    // --- Provider Model ---

    fun putProviderModel(provider: String, model: String) {
        encryptedPrefs.edit().putString("model_$provider", model).apply()
    }

    fun getProviderModel(provider: String): String {
        return encryptedPrefs.getString("model_$provider", null) ?: ""
    }

    // --- Active Provider ---

    fun getActiveProvider(): String {
        return encryptedPrefs.getString("active_provider", "DEEPSEEK") ?: "DEEPSEEK"
    }

    fun setActiveProvider(provider: String) {
        encryptedPrefs.edit().putString("active_provider", provider).apply()
    }

    // --- Proxy (in DataStore, not encrypted) ---

    fun isProxyEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[PROXY_ENABLED] ?: false
        }
    }

    suspend fun setProxyEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs -> prefs[PROXY_ENABLED] = enabled }
        } catch (e: Exception) {
            android.util.Log.e("SecurePrefs", "Failed to save proxy_enabled", e)
        }
    }

    fun getProxyAddress(): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[PROXY_ADDRESS] ?: ""
        }
    }

    suspend fun setProxyAddress(address: String) {
        try {
            context.dataStore.edit { prefs -> prefs[PROXY_ADDRESS] = address }
        } catch (e: Exception) {
            android.util.Log.e("SecurePrefs", "Failed to save proxy_address", e)
        }
    }

    // --- Multimodal Config ---

    fun putMultimodalConfig(url: String, key: String, providerName: String) {
        encryptedPrefs.edit().apply {
            putString("multimodal_api_url", url)
            putString("multimodal_provider", providerName)
        }.apply()
        if (key.isNotBlank()) {
            putMultimodalApiKey(key)
        }
    }

    fun getMultimodalApiUrl(): String {
        return encryptedPrefs.getString("multimodal_api_url", null) ?: ""
    }

    fun getMultimodalApiKey(): String? {
        val envelope = encryptedPrefs.getString("multimodal_api_key", null) ?: return null
        return try {
            cryptoManager.decrypt(envelope)
        } catch (e: Exception) {
            android.util.Log.w("SecurePrefs", "Failed to decrypt multimodal API key")
            null
        }
    }

    fun putMultimodalApiKey(key: String) {
        val envelope = cryptoManager.encrypt(key)
        encryptedPrefs.edit().putString("multimodal_api_key", envelope).apply()
    }

    fun getMultimodalProvider(): String {
        return encryptedPrefs.getString("multimodal_provider", null) ?: "Default"
    }

    fun putCustomProviderName(providerKey: String, name: String) {
        encryptedPrefs.edit().putString("custom_name_$providerKey", name).apply()
    }

    fun getCustomProviderName(providerKey: String): String {
        return encryptedPrefs.getString("custom_name_$providerKey", null) ?: "Custom"
    }

    fun putSystemPrompt(providerKey: String, prompt: String) {
        encryptedPrefs.edit().putString("system_prompt_$providerKey", prompt).apply()
    }

    fun getSystemPrompt(providerKey: String): String {
        return encryptedPrefs.getString("system_prompt_$providerKey", null) ?: ""
    }

    fun putCustomParams(providerKey: String, params: String) {
        encryptedPrefs.edit().putString("custom_params_$providerKey", params).apply()
    }

    fun getCustomParams(providerKey: String): String {
        return encryptedPrefs.getString("custom_params_$providerKey", null) ?: ""
    }

    // --- Theme ---

    fun getThemeMode(): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[THEME_MODE] ?: "system"
        }
    }

    suspend fun setThemeMode(mode: String) {
        try {
            context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
        } catch (e: Exception) {
            android.util.Log.e("SecurePrefs", "Failed to save theme_mode", e)
        }
    }

    fun getLanguage(): Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: "en"
    }

    suspend fun setLanguage(lang: String) {
        try { context.dataStore.edit { prefs -> prefs[LANGUAGE_KEY] = lang } }
        catch (e: Exception) { android.util.Log.e("SecurePrefs", "Failed to save language", e) }
    }

    companion object {
        private val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        private val PROXY_ADDRESS = stringPreferencesKey("proxy_address")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
    }
}
