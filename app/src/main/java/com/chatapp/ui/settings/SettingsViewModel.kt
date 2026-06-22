package com.chatapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatapp.domain.model.MultimodalConfig
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
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getThemeMode().collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isProxyEnabled().collect { enabled ->
                _uiState.update { it.copy(proxyEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getProxyAddress().collect { addr ->
                _uiState.update { it.copy(proxyAddress = addr) }
            }
        }
        loadConfiguredProviders()
        loadMultimodalConfig()
        viewModelScope.launch {
            settingsRepository.getLanguage().collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
    }

    private fun loadMultimodalConfig() {
        viewModelScope.launch {
            val config = settingsRepository.getMultimodalConfig()
            _uiState.update {
                it.copy(
                    multimodalProvider = config.providerName.ifEmpty { "Default" },
                    multimodalApiUrl = config.apiUrl,
                    multimodalApiKey = config.apiKey
                )
            }
        }
    }

    fun loadConfiguredProviders() {
        viewModelScope.launch {
            val configured = ProviderType.entries.filter {
                !settingsRepository.getApiKey(it).isNullOrBlank()
            }.toSet()
            _uiState.update { it.copy(configuredProviders = configured) }
        }
    }

    fun toggleOtherProviders() {
        _uiState.update { it.copy(showOtherProviders = !it.showOtherProviders) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setProxyEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setProxyEnabled(enabled) }
    }

    fun setProxyAddress(address: String) {
        _uiState.update { it.copy(proxyAddress = address) }
        viewModelScope.launch { settingsRepository.setProxyAddress(address) }
    }

    fun setMultimodalProvider(name: String) {
        _uiState.update { it.copy(multimodalProvider = name) }
        saveMultimodalConfig()
    }

    fun setMultimodalApiUrl(url: String) {
        _uiState.update { it.copy(multimodalApiUrl = url) }
        saveMultimodalConfig()
    }

    fun setMultimodalApiKey(key: String) {
        _uiState.update { it.copy(multimodalApiKey = key) }
        saveMultimodalConfig()
    }

    fun setLanguage(lang: String) {
        _uiState.update { it.copy(language = lang) }
        viewModelScope.launch { settingsRepository.setLanguage(lang) }
    }

    fun getProviderDisplayName(provider: ProviderType): String = settingsRepository.getProviderDisplayName(provider)

    fun saveMultimodalConfig() {
        viewModelScope.launch {
            val s = _uiState.value
            settingsRepository.saveMultimodalConfig(
                MultimodalConfig(
                    apiUrl = s.multimodalApiUrl,
                    apiKey = s.multimodalApiKey,
                    providerName = s.multimodalProvider
                )
            )
        }
    }
}
