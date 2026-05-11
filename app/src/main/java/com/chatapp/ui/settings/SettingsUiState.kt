package com.chatapp.ui.settings

data class SettingsUiState(
    val themeMode: String = "system",
    val proxyEnabled: Boolean = false,
    val proxyAddress: String = ""
)
