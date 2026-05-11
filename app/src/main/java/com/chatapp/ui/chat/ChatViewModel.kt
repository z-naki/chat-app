package com.chatapp.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.MessageStatus
import com.chatapp.domain.model.StreamChunk
import com.chatapp.domain.repository.ChatRepository
import com.chatapp.domain.usecase.SendMessageUseCase
import com.chatapp.domain.usecase.StreamMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val streamMessageUseCase: StreamMessageUseCase
) : ViewModel() {

    private val conversationId: Long = savedStateHandle["conversationId"] ?: 0L

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var onConversationCreated: ((Long) -> Unit)? = null

    fun setOnConversationCreated(callback: ((Long) -> Unit)?) {
        onConversationCreated = callback
    }

    init {
        if (conversationId > 0) {
            viewModelScope.launch {
                val conversation = chatRepository.getConversation(conversationId)
                _uiState.update { it.copy(conversation = conversation) }
            }
            viewModelScope.launch {
                chatRepository.getMessages(conversationId).collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(enableSearch = !it.enableSearch) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        _uiState.update { it.copy(inputText = "", errorMessage = null) }

        viewModelScope.launch {
            // Auto-create conversation if this is a new conversation
            var activeConversationId = conversationId
            if (activeConversationId <= 0) {
                val newConv = chatRepository.createConversation(
                    title = text.take(50),
                    provider = com.chatapp.domain.model.ProviderType.DEEPSEEK
                )
                activeConversationId = newConv.id
                _uiState.update { it.copy(conversation = newConv) }
                onConversationCreated?.invoke(activeConversationId)
            }

            val userMessage = sendMessageUseCase(activeConversationId, text)

            val streamingMessage = Message(
                conversationId = activeConversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                status = MessageStatus.STREAMING
            )
            val streamingId = chatRepository.saveMessage(streamingMessage)

            _uiState.update { it.copy(isStreaming = true, streamingContent = "") }

            streamJob = viewModelScope.launch {
                val conversation = _uiState.value.conversation ?: run {
                    _uiState.update {
                        it.copy(isStreaming = false, errorMessage = "Conversation not loaded")
                    }
                    return@launch
                }
                val messages = _uiState.value.messages + userMessage
                streamMessageUseCase(
                    conversation = conversation,
                    messages = messages,
                    enableSearch = _uiState.value.enableSearch
                ).collect { chunk ->
                    when (chunk) {
                        is StreamChunk.Content -> {
                            _uiState.update {
                                it.copy(
                                    streamingContent = it.streamingContent + chunk.text
                                )
                            }
                        }
                        is StreamChunk.Thinking -> { }
                        is StreamChunk.SearchStatus -> { }
                        is StreamChunk.Done -> {
                            val fullContent = _uiState.value.streamingContent
                            chatRepository.updateMessageContent(streamingId, fullContent, null)
                            _uiState.update {
                                it.copy(isStreaming = false, streamingContent = "")
                            }
                        }
                        is StreamChunk.Error -> {
                            chatRepository.updateMessageContent(
                                streamingId,
                                _uiState.value.streamingContent,
                                null
                            )
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    streamingContent = "",
                                    errorMessage = chunk.throwable.message ?: "Unknown error"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        _uiState.update {
            it.copy(isStreaming = false, streamingContent = "")
        }
    }

    fun updateParameters(temperature: Float, maxTokens: String, contextRounds: String) {
        val conv = _uiState.value.conversation ?: return
        val tokens = maxTokens.toIntOrNull() ?: return
        val rounds = contextRounds.toIntOrNull() ?: return
        viewModelScope.launch {
            chatRepository.updateConversationParameters(conv.id, temperature, tokens, rounds)
            _uiState.update { it.copy(conversation = conv.copy(
                temperature = temperature,
                maxTokens = tokens,
                contextRounds = rounds
            ))}
        }
    }
}
