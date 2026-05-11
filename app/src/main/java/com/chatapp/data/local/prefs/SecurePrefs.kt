package com.chatapp.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
            null
        }
    }

    fun deleteApiKey(provider: String) {
        encryptedPrefs.edit().remove("api_key_$provider").apply()
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
        context.dataStore.edit { prefs ->
            prefs[PROXY_ENABLED] = enabled
        }
    }

    fun getProxyAddress(): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[PROXY_ADDRESS] ?: ""
        }
    }

    suspend fun setProxyAddress(address: String) {
        context.dataStore.edit { prefs ->
            prefs[PROXY_ADDRESS] = address
        }
    }

    // --- Theme ---

    fun getThemeMode(): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[THEME_MODE] ?: "system"
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    // --- Model Parameters ---

    fun getTemperature(): Flow<Float> {
        return context.dataStore.data.map { prefs ->
            prefs[TEMPERATURE] ?: 0.7f
        }
    }

    suspend fun setTemperature(temp: Float) {
        context.dataStore.edit { prefs ->
            prefs[TEMPERATURE] = temp
        }
    }

    fun getMaxTokens(): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[MAX_TOKENS] ?: 384_000
        }
    }

    suspend fun setMaxTokens(tokens: Int) {
        context.dataStore.edit { prefs ->
            prefs[MAX_TOKENS] = tokens
        }
    }

    fun getContextRounds(): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[CONTEXT_ROUNDS] ?: 20
        }
    }

    suspend fun setContextRounds(rounds: Int) {
        context.dataStore.edit { prefs ->
            prefs[CONTEXT_ROUNDS] = rounds
        }
    }

    companion object {
        private val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        private val PROXY_ADDRESS = stringPreferencesKey("proxy_address")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val TEMPERATURE = floatPreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val CONTEXT_ROUNDS = intPreferencesKey("context_rounds")
    }
}
