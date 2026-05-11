package com.chatapp.ui.provideredit

import com.chatapp.domain.model.ProviderType

data class ProviderEditUiState(
    val selectedProvider: ProviderType = ProviderType.DEEPSEEK,
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val showKey: Boolean = false,
    val providerExpanded: Boolean = false,
    val isLoaded: Boolean = false
)
