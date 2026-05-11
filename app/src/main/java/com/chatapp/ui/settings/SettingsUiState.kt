package com.chatapp.ui.settings

import com.chatapp.domain.model.ProviderType

data class SettingsUiState(
    val themeMode: String = "system",
    val activeProvider: ProviderType = ProviderType.DEEPSEEK,
    val proxyEnabled: Boolean = false,
    val proxyAddress: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: String = "384000",
    val contextRounds: String = "20",
    val apiKeys: Map<ProviderType, Boolean> = mapOf(ProviderType.DEEPSEEK to true),
    val editingProvider: ProviderType? = null,
    val editingKeyValue: String = "",
    val showProxyWarning: Boolean = false,
    val showKey: Map<ProviderType, Boolean> = emptyMap()
)
