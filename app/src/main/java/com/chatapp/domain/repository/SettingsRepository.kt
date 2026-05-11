package com.chatapp.domain.repository

import com.chatapp.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun saveApiKey(providerType: ProviderType, key: String)
    suspend fun getApiKey(providerType: ProviderType): String?
    suspend fun deleteApiKey(providerType: ProviderType)
    suspend fun getActiveProvider(): ProviderType
    suspend fun setActiveProvider(type: ProviderType)
    fun isProxyEnabled(): Flow<Boolean>
    suspend fun setProxyEnabled(enabled: Boolean)
    fun getProxyAddress(): Flow<String>
    suspend fun setProxyAddress(address: String)
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)
    fun getTemperature(): Flow<Float>
    suspend fun setTemperature(temp: Float)
    fun getMaxTokens(): Flow<Int>
    suspend fun setMaxTokens(tokens: Int)
    fun getContextRounds(): Flow<Int>
    suspend fun setContextRounds(rounds: Int)
}
