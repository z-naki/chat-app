package com.chatapp.data.remote.provider

import com.chatapp.domain.model.ProviderType
import com.chatapp.data.local.prefs.SecurePrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRouter @Inject constructor(
    private val securePrefs: SecurePrefs
) {
    private val providers = mutableMapOf<ProviderType, AiProvider>()

    fun register(provider: AiProvider) {
        providers[provider.type] = provider
    }

    fun resolve(type: ProviderType): AiProvider {
        return providers[type]
            ?: throw IllegalArgumentException("No provider registered for $type")
    }

    fun getActive(): AiProvider {
        val activeType = securePrefs.getActiveProvider()
        val type = try {
            ProviderType.valueOf(activeType)
        } catch (e: IllegalArgumentException) {
            ProviderType.DEEPSEEK
        }
        return resolve(type)
    }

    fun getAll(): Set<ProviderType> = providers.keys
}
