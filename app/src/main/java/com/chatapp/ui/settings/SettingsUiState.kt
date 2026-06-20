package com.chatapp.ui.settings

import com.chatapp.domain.model.ProviderType

data class SettingsUiState(
    val themeMode: String = "system",
    val proxyEnabled: Boolean = false,
    val proxyAddress: String = "",
    val configuredProviders: Set<ProviderType> = emptySet(),
    val showOtherProviders: Boolean = false,
    val multimodalProvider: String = "Default",
    val multimodalApiUrl: String = "",
    val multimodalApiKey: String = "",
    val language: String = "en",
    val customSlotCount: Int = 1
)
