package com.chatapp.ui.settings

import com.chatapp.domain.model.ProviderType

data class SettingsUiState(
    val themeMode: String = "system",
    val proxyEnabled: Boolean = false,
    val proxyAddress: String = "",
    val configuredProviders: Set<ProviderType> = emptySet(),
    val showOtherProviders: Boolean = false
)
