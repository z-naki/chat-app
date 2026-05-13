package com.chatapp.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.MessageStatus
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.StreamChunk
import com.chatapp.domain.repository.ChatRepository
import com.chatapp.util.DebugLog
import android.util.Log
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
        if (text.isBlank() || _uiState.value.isStreaming) return

        DebugLog.log("ChatVM", "sendMessage: text='${text.take(30)}'")
        Log.e("ChatApp", "=== sendMessage START === ")
        _uiState.update { it.copy(inputText = "", isStreaming = true, streamingContent = "", errorMessage = null) }

        viewModelScope.launch {
            var activeConversationId = conversationId
            val isNew = activeConversationId <= 0
            if (isNew) {
                DebugLog.log("ChatVM", "Auto-creating conversation...")
                val newConv = chatRepository.createConversation(
                    title = text.take(50),
                    provider = ProviderType.DEEPSEEK
                )
                activeConversationId = newConv.id
                _uiState.update { it.copy(conversation = newConv) }
                DebugLog.log("ChatVM", "Auto-created convId=$activeConversationId")
            }

            DebugLog.log("ChatVM", "Saving user message for convId=$activeConversationId")
            val userMessage = sendMessageUseCase(activeConversationId, text)

            if (isNew) {
                _uiState.update { it.copy(messages = listOf(userMessage)) }
            } else {
                _uiState.update { it.copy(messages = it.messages + userMessage) }
            }

            val streamingMessage = Message(
                conversationId = activeConversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                status = MessageStatus.STREAMING
            )
            val streamingId = chatRepository.saveMessage(streamingMessage)

            streamJob?.cancel()
            streamJob = viewModelScope.launch {
                val conversation = _uiState.value.conversation ?: run {
                    _uiState.update {
                        it.copy(isStreaming = false, errorMessage = "Conversation not loaded")
                    }
                    return@launch
                }
                val messages = if (isNew) listOf(userMessage) else _uiState.value.messages
                DebugLog.log("ChatVM", "Sending ${messages.size} messages to stream")
                streamMessageUseCase(
                    conversation = conversation,
                    messages = messages,
                    enableSearch = _uiState.value.enableSearch
                ).collect { chunk ->
                    when (chunk) {
                        is StreamChunk.Content -> {
                            val safe = chunk.text.replace("null", "")
                            if (safe.isNotEmpty()) {
                                DebugLog.log("NULL", "S5_CVM+ txt='${safe.take(80)}'")
                                Log.e("ChatApp", "AI: ${safe.take(80)}")
                            } else if (chunk.text.isNotEmpty()) {
                                DebugLog.log("NULL", "S5_SKIP nullContent len=${chunk.text.length}")
                            }
                            _uiState.update {
                                val newContent = it.streamingContent + safe
                                if (safe.isEmpty()) {
                                    DebugLog.log("NULL", "S6_ACC len=${newContent.length} (no change)")
                                }
                                it.copy(streamingContent = newContent)
                            }
                        }
                        is StreamChunk.Thinking -> {
                            val safe = chunk.text.replace("null", "")
                            if (safe.isNotEmpty()) {
                                DebugLog.log("NULL", "S5_THK+ txt='${safe.take(80)}'")
                                _uiState.update {
                                    val newContent = it.streamingContent + safe
                                    it.copy(streamingContent = newContent)
                                }
                            } else if (chunk.text.isNotEmpty()) {
                                DebugLog.log("NULL", "S5_SKIP nullThink len=${chunk.text.length}")
                            }
                        }
                        is StreamChunk.SearchStatus -> { }
                        is StreamChunk.Done -> {
                            val raw = _uiState.value.streamingContent
                            val fullContent = raw.replace("null", "")
                            Log.e("ChatApp", "=== Stream DONE, raw=${raw.length} clean=${fullContent.length} ===")
                            DebugLog.log("ChatVM", "StreamChunk.Done received")
                            chatRepository.updateMessageContent(streamingId, fullContent, null)
                            val completedMsg = Message(
                                id = streamingId,
                                conversationId = activeConversationId,
                                role = MessageRole.ASSISTANT,
                                content = fullContent,
                                status = MessageStatus.COMPLETE
                            )
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    streamingContent = "",
                                    messages = it.messages.filterNot { m -> m.id == streamingId } + completedMsg
                                )
                            }
                            if (isNew) onConversationCreated?.invoke(activeConversationId)
                        }
                        is StreamChunk.Error -> {
                            Log.e("ChatApp", "Stream error: ${chunk.throwable.message}")
                            DebugLog.log("ChatVM", "StreamChunk.Error: ${chunk.throwable.message}")
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
                            if (isNew) onConversationCreated?.invoke(activeConversationId)
                        }
                    }
                }
            }
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        val partial = _uiState.value.streamingContent
        _uiState.update {
            it.copy(isStreaming = false, streamingContent = "")
        }
        // Try to find and save the streaming message
        val streamingMsg = _uiState.value.messages.lastOrNull { it.status == MessageStatus.STREAMING }
        if (streamingMsg != null && partial.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.updateMessageContent(streamingMsg.id, partial, null)
            }
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
