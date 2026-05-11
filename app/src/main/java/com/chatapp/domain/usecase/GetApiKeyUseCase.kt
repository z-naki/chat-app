package com.chatapp.domain.usecase

import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.repository.SettingsRepository
import javax.inject.Inject

class GetApiKeyUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(providerType: ProviderType): String? {
        return settingsRepository.getApiKey(providerType)
    }
}
