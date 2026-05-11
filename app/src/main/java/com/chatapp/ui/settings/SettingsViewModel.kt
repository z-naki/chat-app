package com.chatapp.ui.settings

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

    fun startEditApiKey(providerType: ProviderType) {
        viewModelScope.launch {
            val existingKey = settingsRepository.getApiKey(providerType)
            _uiState.update {
                it.copy(
                    editingProvider = providerType,
                    editingKeyValue = existingKey ?: "",
                    showProxyWarning = providerType.requiresProxy && !it.proxyEnabled
                )
            }
        }
    }

    fun onEditKeyValueChange(value: String) {
        _uiState.update { it.copy(editingKeyValue = value) }
    }

    fun saveApiKey() {
        val provider = _uiState.value.editingProvider ?: return
        val key = _uiState.value.editingKeyValue.trim()
        viewModelScope.launch {
            if (key.isEmpty()) {
                settingsRepository.deleteApiKey(provider)
            } else {
                settingsRepository.saveApiKey(provider, key)
            }
            _uiState.update {
                it.copy(
                    editingProvider = null,
                    editingKeyValue = "",
                    showProxyWarning = false,
                    apiKeys = it.apiKeys + (provider to key.isNotEmpty())
                )
            }
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingProvider = null, editingKeyValue = "", showProxyWarning = false) }
    }

    fun toggleKeyVisibility(providerType: ProviderType) {
        _uiState.update {
            it.copy(showKey = it.showKey + (providerType to !(it.showKey[providerType] ?: false)))
        }
    }
}
