package com.chatapp.ui.conversationlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.repository.SettingsRepository
import com.chatapp.domain.usecase.CreateConversationUseCase
import com.chatapp.domain.usecase.DeleteConversationUseCase
import com.chatapp.domain.usecase.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val createConversationUseCase: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getConversationsUseCase().collect { conversations ->
                _uiState.update { it.copy(conversations = conversations, isLoading = false) }
            }
        }
    }

    fun createConversation(title: String, provider: ProviderType = ProviderType.DEEPSEEK) {
        viewModelScope.launch {
            createConversationUseCase(title, provider)
        }
    }

    fun getProviderDisplayName(provider: ProviderType): String = settingsRepository.getProviderDisplayName(provider)

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            deleteConversationUseCase(conversationId)
        }
    }
}
