package com.chatapp.data.repository

import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val securePrefs: SecurePrefs
) : SettingsRepository {

    override suspend fun saveApiKey(providerType: ProviderType, key: String) {
        securePrefs.putApiKey(providerType.name, key)
    }

    override suspend fun getApiKey(providerType: ProviderType): String? {
        return securePrefs.getApiKey(providerType.name)
    }

    override suspend fun deleteApiKey(providerType: ProviderType) {
        securePrefs.deleteApiKey(providerType.name)
    }

    override suspend fun getActiveProvider(): ProviderType {
        return ProviderType.fromStringOrDefault(securePrefs.getActiveProvider())
    }

    override suspend fun setActiveProvider(type: ProviderType) {
        securePrefs.setActiveProvider(type.name)
    }

    override suspend fun saveProviderBaseUrl(providerType: ProviderType, url: String) {
        securePrefs.putProviderBaseUrl(providerType.name, url)
    }

    override suspend fun getProviderBaseUrl(providerType: ProviderType): String {
        return securePrefs.getProviderBaseUrl(providerType.name)
    }

    override suspend fun saveProviderModel(providerType: ProviderType, model: String) {
        securePrefs.putProviderModel(providerType.name, model)
    }

    override suspend fun getProviderModel(providerType: ProviderType): String {
        return securePrefs.getProviderModel(providerType.name)
    }

    override fun isProxyEnabled(): Flow<Boolean> = securePrefs.isProxyEnabled()

    override suspend fun setProxyEnabled(enabled: Boolean) = securePrefs.setProxyEnabled(enabled)

    override fun getProxyAddress(): Flow<String> = securePrefs.getProxyAddress()

    override suspend fun setProxyAddress(address: String) = securePrefs.setProxyAddress(address)

    override fun getThemeMode(): Flow<String> = securePrefs.getThemeMode()

    override suspend fun setThemeMode(mode: String) = securePrefs.setThemeMode(mode)
}
