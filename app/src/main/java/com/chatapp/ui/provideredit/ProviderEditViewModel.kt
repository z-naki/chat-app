package com.chatapp.ui.provideredit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderEditViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderEditUiState())
    val uiState: StateFlow<ProviderEditUiState> = _uiState.asStateFlow()

    fun loadProvider(providerType: ProviderType) {
        viewModelScope.launch {
            val key = settingsRepository.getApiKey(providerType) ?: ""
            val baseUrl = settingsRepository.getProviderBaseUrl(providerType)
                .ifEmpty { defaultBaseUrl(providerType) }
            val model = settingsRepository.getProviderModel(providerType)
                .ifEmpty { defaultModel(providerType) }
            _uiState.update {
                it.copy(
                    selectedProvider = providerType,
                    apiKey = key,
                    baseUrl = baseUrl,
                    model = model,
                    isLoaded = true
                )
            }
        }
    }

    fun onProviderSelect(provider: ProviderType) {
        _uiState.update { it.copy(selectedProvider = provider, providerExpanded = false) }
        loadProvider(provider)
    }

    fun toggleProviderDropdown() {
        _uiState.update { it.copy(providerExpanded = !it.providerExpanded) }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value) }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun onModelChange(value: String) {
        _uiState.update { it.copy(model = value) }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(showKey = !it.showKey) }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val provider = state.selectedProvider
            if (state.apiKey.isBlank()) {
                settingsRepository.deleteApiKey(provider)
            } else {
                settingsRepository.saveApiKey(provider, state.apiKey)
            }
            settingsRepository.saveProviderBaseUrl(provider, state.baseUrl)
            settingsRepository.saveProviderModel(provider, state.model)
        }
    }

    private fun defaultBaseUrl(type: ProviderType): String = when (type) {
        ProviderType.DEEPSEEK -> "https://api.deepseek.com"
        ProviderType.OPENAI -> "https://api.openai.com"
        ProviderType.ANTHROPIC -> "https://api.anthropic.com"
        ProviderType.GEMINI -> "https://generativelanguage.googleapis.com"
    }

    private fun defaultModel(type: ProviderType): String = when (type) {
        ProviderType.DEEPSEEK -> "deepseek-v4-pro"
        ProviderType.OPENAI -> "gpt-4o"
        ProviderType.ANTHROPIC -> "claude-sonnet-4-6"
        ProviderType.GEMINI -> "gemini-2.5-pro"
    }
}
