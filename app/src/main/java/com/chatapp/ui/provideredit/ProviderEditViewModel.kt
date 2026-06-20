package com.chatapp.ui.provideredit

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.repository.SettingsRepository
import com.chatapp.util.BiometricAuthHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderEditViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val biometricAuthHelper: BiometricAuthHelper,
    private val securePrefs: SecurePrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderEditUiState())
    val uiState: StateFlow<ProviderEditUiState> = _uiState.asStateFlow()

    private var activity: FragmentActivity? = null

    fun setActivity(activity: FragmentActivity) {
        this.activity = activity
    }

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
                    isLoaded = true,
                    showKey = false,
                    isAuthenticated = false,
                    isAuthenticating = false,
                    authErrorMessage = null,
                    customProviderName = securePrefs.getCustomProviderName(providerType.name),
                    systemPrompt = securePrefs.getSystemPrompt(providerType.name),
                    customParams = securePrefs.getCustomParams(providerType.name)
                )
            }
        }
    }

    fun onCustomNameChange(name: String) {
        _uiState.update { it.copy(customProviderName = name) }
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
        val current = _uiState.value

        // If currently showing, hide immediately (no auth needed to hide)
        if (current.showKey) {
            _uiState.update { it.copy(showKey = false, isAuthenticated = false) }
            return
        }

        // If already authenticated this session, show immediately
        if (current.isAuthenticated) {
            _uiState.update { it.copy(showKey = true) }
            return
        }

        // Require biometric authentication
        val act = activity
        if (act == null) {
            _uiState.update { it.copy(authErrorMessage = "Activity not available") }
            return
        }

        _uiState.update { it.copy(isAuthenticating = true) }
        biometricAuthHelper.authenticate(act) { result ->
            if (result.success) {
                _uiState.update {
                    it.copy(showKey = true, isAuthenticated = true, isAuthenticating = false, authErrorMessage = null)
                }
            } else {
                _uiState.update {
                    it.copy(isAuthenticating = false, authErrorMessage = result.errorMessage)
                }
            }
        }
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
            if (provider.name.startsWith("CUSTOM")) {
                securePrefs.putCustomProviderName(provider.name, state.customProviderName)
            }
            securePrefs.putSystemPrompt(provider.name, state.systemPrompt)
            securePrefs.putCustomParams(provider.name, state.customParams)
        }
    }

    fun onSystemPromptChange(v: String) { _uiState.update { it.copy(systemPrompt = v) } }
    fun onCustomParamsChange(v: String) { _uiState.update { it.copy(customParams = v) } }

    private fun defaultBaseUrl(type: ProviderType): String = when (type) {
        ProviderType.DEEPSEEK -> "https://api.deepseek.com"
        ProviderType.OPENAI -> "https://api.openai.com"
        ProviderType.ANTHROPIC -> "https://api.anthropic.com"
        ProviderType.GEMINI -> "https://generativelanguage.googleapis.com"
        ProviderType.MOONSHOT -> "https://api.moonshot.cn"
        ProviderType.QWEN -> "https://dashscope.aliyuncs.com/compatible-mode"
        ProviderType.CUSTOM_1, ProviderType.CUSTOM_2, ProviderType.CUSTOM_3 -> ""
    }

    private fun defaultModel(type: ProviderType): String = when (type) {
        ProviderType.DEEPSEEK -> "deepseek-v4-pro"
        ProviderType.OPENAI -> "gpt-4o"
        ProviderType.ANTHROPIC -> "claude-sonnet-4-6"
        ProviderType.GEMINI -> "gemini-2.5-pro"
        ProviderType.MOONSHOT -> "moonshot-v1-128k"
        ProviderType.QWEN -> "qwen-max"
        ProviderType.CUSTOM_1, ProviderType.CUSTOM_2, ProviderType.CUSTOM_3 -> ""
    }
}
