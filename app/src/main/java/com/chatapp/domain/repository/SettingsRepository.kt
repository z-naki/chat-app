package com.chatapp.domain.repository

import com.chatapp.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun saveApiKey(providerType: ProviderType, key: String)
    suspend fun getApiKey(providerType: ProviderType): String?
    suspend fun deleteApiKey(providerType: ProviderType)
    suspend fun saveProviderBaseUrl(providerType: ProviderType, url: String)
    suspend fun getProviderBaseUrl(providerType: ProviderType): String
    suspend fun saveProviderModel(providerType: ProviderType, model: String)
    suspend fun getProviderModel(providerType: ProviderType): String
    suspend fun getActiveProvider(): ProviderType
    suspend fun setActiveProvider(type: ProviderType)
    fun isProxyEnabled(): Flow<Boolean>
    suspend fun setProxyEnabled(enabled: Boolean)
    fun getProxyAddress(): Flow<String>
    suspend fun setProxyAddress(address: String)
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)
}
