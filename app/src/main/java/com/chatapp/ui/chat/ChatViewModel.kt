package com.chatapp.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatapp.data.remote.provider.ProviderRouter
import com.chatapp.domain.model.Attachment
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.MessageStatus
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.StreamChunk
import com.chatapp.domain.repository.ChatRepository
import com.chatapp.domain.repository.SettingsRepository
import com.chatapp.util.DebugLog
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
    private val settingsRepository: SettingsRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val streamMessageUseCase: StreamMessageUseCase,
    private val providerRouter: ProviderRouter
) : ViewModel() {

    private val conversationId: Long = savedStateHandle["conversationId"] ?: 0L

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var messagesJob: Job? = null
    private var onConversationCreated: ((Long) -> Unit)? = null

    fun setOnConversationCreated(callback: ((Long) -> Unit)?) {
        onConversationCreated = callback
    }

    fun loadConversation(id: Long) {
        if (id <= 0) {
            // New conversation: clear state
            messagesJob?.cancel()
            _uiState.update { it.copy(conversation = null, messages = emptyList(), isStreaming = false) }
            return
        }
        if (id == _uiState.value.conversation?.id) return
        // Cancel previous messages collection to avoid interleaving
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            val conversation = chatRepository.getConversation(id)
            _uiState.update {
                it.copy(
                    conversation = conversation,
                    temperature = conversation?.temperature ?: 0.7f,
                    contextRounds = conversation?.contextRounds ?: 20,
                    maxTokensUi = conversation?.maxTokens ?: 384_000,
                    topP = conversation?.topP ?: 0.9f,
                    multimodalEnabled = conversation?.multimodalEnabled ?: false,
                    messages = emptyList() // Clear old messages immediately
                )
            }
            chatRepository.getMessages(id).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    init {
        viewModelScope.launch {
            val activeProvider = settingsRepository.getActiveProvider()
            val model = settingsRepository.getProviderModel(activeProvider)
            val defaultModel = getProviderDefaultModel(activeProvider)
            _uiState.update {
                it.copy(
                    activeProvider = activeProvider,
                    currentModel = model.ifEmpty { defaultModel }
                )
            }
            // Pre-fetch available models
            val models = providerRouterFetchModels(activeProvider)
            _uiState.update { it.copy(availableModels = models.ifEmpty { getProviderFallbackModels(activeProvider) }) }
        }
        if (conversationId > 0) {
            viewModelScope.launch {
                val conversation = chatRepository.getConversation(conversationId)
                _uiState.update {
                    it.copy(
                        conversation = conversation,
                        temperature = conversation?.temperature ?: 0.7f,
                        contextRounds = conversation?.contextRounds ?: 20,
                        maxTokensUi = conversation?.maxTokens ?: 384_000,
                        topP = conversation?.topP ?: 0.9f,
                        multimodalEnabled = conversation?.multimodalEnabled ?: false
                    )
                }
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

    fun toggleModelPicker() {
        val show = !_uiState.value.showModelPicker
        _uiState.update { it.copy(showModelPicker = show) }
        if (show) {
            viewModelScope.launch {
                val provider = _uiState.value.activeProvider
                val models = providerRouterFetchModels(provider)
                _uiState.update { it.copy(availableModels = models) }
            }
        }
    }

    fun selectModel(model: String) {
        val provider = _uiState.value.activeProvider
        _uiState.update { it.copy(currentModel = model, showModelPicker = false) }
        viewModelScope.launch {
            settingsRepository.saveProviderModel(provider, model)
            // Apply provider-recommended defaults when switching models
            val defaults = getProviderDefaults(provider)
            val convId = _uiState.value.conversation?.id ?: return@launch
            _uiState.update { it.copy(conversation = it.conversation?.copy(temperature = defaults.temp, maxTokens = defaults.maxTokens, contextRounds = defaults.contextRounds)) }
            chatRepository.updateConversationParameters(convId, defaults.temp, defaults.maxTokens, defaults.contextRounds)
        }
    }

    private data class ProviderDefaults(val temp: Float, val maxTokens: Int, val contextRounds: Int)
    private fun getProviderDefaults(provider: ProviderType): ProviderDefaults = when (provider) {
        ProviderType.DEEPSEEK -> ProviderDefaults(0.7f, 384_000, 20)
        ProviderType.OPENAI -> ProviderDefaults(0.7f, 16_384, 20)
        ProviderType.ANTHROPIC -> ProviderDefaults(0.7f, 16_384, 20)
        ProviderType.GEMINI -> ProviderDefaults(0.9f, 65_536, 20)
        ProviderType.MOONSHOT -> ProviderDefaults(0.3f, 4_096, 20)
        ProviderType.QWEN -> ProviderDefaults(0.7f, 8_192, 20)
        ProviderType.CUSTOM_1, ProviderType.CUSTOM_2, ProviderType.CUSTOM_3 -> ProviderDefaults(0.7f, 16_384, 20)
    }

    fun selectProvider(provider: ProviderType) {
        _uiState.update { it.copy(activeProvider = provider, showModelPicker = false) }
        viewModelScope.launch {
            settingsRepository.setActiveProvider(provider)
            val model = settingsRepository.getProviderModel(provider)
            _uiState.update {
                it.copy(currentModel = model.ifEmpty { getProviderDefaultModel(provider) })
            }
            val models = providerRouterFetchModels(provider)
            _uiState.update {
                it.copy(availableModels = models.ifEmpty { getProviderFallbackModels(provider) })
            }
        }
    }

    private fun getProviderDefaultModel(provider: ProviderType): String = when (provider) {
        ProviderType.DEEPSEEK -> "deepseek-v4-pro"
        ProviderType.OPENAI -> "gpt-4o"
        ProviderType.ANTHROPIC -> "claude-sonnet-4-6"
        ProviderType.GEMINI -> "gemini-2.5-flash"
        ProviderType.MOONSHOT -> "moonshot-v1-128k"
        ProviderType.QWEN -> "qwen-max"
        ProviderType.CUSTOM_1, ProviderType.CUSTOM_2, ProviderType.CUSTOM_3 -> ""
    }

    private fun getProviderFallbackModels(provider: ProviderType): List<String> = when (provider) {
        ProviderType.DEEPSEEK -> listOf("deepseek-v4-pro", "deepseek-chat")
        ProviderType.OPENAI -> listOf("gpt-4o", "gpt-4o-mini", "o4-mini", "o3-mini")
        ProviderType.ANTHROPIC -> listOf("claude-sonnet-4-6", "claude-opus-4-8", "claude-haiku-4-5-20251001", "claude-fable-5")
        ProviderType.GEMINI -> listOf("gemini-2.5-flash", "gemini-2.5-pro")
        ProviderType.MOONSHOT -> listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
        ProviderType.QWEN -> listOf("qwen-turbo", "qwen-plus", "qwen-max")
        ProviderType.CUSTOM_1, ProviderType.CUSTOM_2, ProviderType.CUSTOM_3 -> emptyList()
    }

    fun toggleMultimodal() {
        _uiState.update { it.copy(multimodalEnabled = !it.multimodalEnabled) }
        val convId = _uiState.value.conversation?.id ?: return
        viewModelScope.launch {
            val conv = _uiState.value.conversation ?: return@launch
            chatRepository.updateConversationMultimodal(convId, !conv.multimodalEnabled)
            _uiState.update { it.copy(conversation = conv.copy(multimodalEnabled = !conv.multimodalEnabled)) }
        }
    }

    private suspend fun providerRouterFetchModels(provider: ProviderType): List<String> {
        return try {
            providerRouter.resolve(provider).fetchAvailableModels()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp, conversation = it.conversation?.copy(temperature = temp)) }
        val convId = _uiState.value.conversation?.id ?: return
        viewModelScope.launch { chatRepository.updateConversationParameters(convId, temp, _uiState.value.maxTokensUi, _uiState.value.contextRounds) }
    }

    fun updateContextRounds(rounds: Int) {
        _uiState.update { it.copy(contextRounds = rounds, conversation = it.conversation?.copy(contextRounds = rounds)) }
        val convId = _uiState.value.conversation?.id ?: return
        viewModelScope.launch { chatRepository.updateConversationParameters(convId, _uiState.value.temperature, _uiState.value.maxTokensUi, rounds) }
    }

    fun updateTopP(p: Float) {
        _uiState.update { it.copy(topP = p, conversation = it.conversation?.copy(topP = p)) }
        val convId = _uiState.value.conversation?.id ?: return
        viewModelScope.launch { chatRepository.updateConversationTopP(convId, p) }
    }

    fun supportsTopP(): Boolean {
        // DeepSeek thinking models: temperature/top_p are ignored by API
        if (_uiState.value.activeProvider == ProviderType.DEEPSEEK && !_uiState.value.currentModel.contains("chat")) return false
        return true
    }

    fun supportsTemperature(): Boolean {
        val p = _uiState.value.activeProvider
        // DeepSeek thinking models: temperature is ignored by API
        if (p == ProviderType.DEEPSEEK && !_uiState.value.currentModel.contains("chat")) return false
        return true
    }

    fun supportsSearch(): Boolean = _uiState.value.activeProvider == ProviderType.DEEPSEEK

    fun updateMaxTokens(tokens: Int) {
        _uiState.update { it.copy(maxTokensUi = tokens, conversation = it.conversation?.copy(maxTokens = tokens)) }
        val convId = _uiState.value.conversation?.id ?: return
        viewModelScope.launch { chatRepository.updateConversationParameters(convId, _uiState.value.temperature, tokens, _uiState.value.contextRounds) }
    }

    fun addAttachment(attachment: Attachment) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + attachment) }
    }

    fun removeAttachment(attachmentId: String) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments.filter { a -> a.id != attachmentId }) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isStreaming) return

        DebugLog.log("ChatVM", "sendMessage: text='${text.take(30)}'")
        DebugLog.log("VM", "=== sendMessage START ===")
        val attachments = _uiState.value.pendingAttachments
        _uiState.update { it.copy(inputText = "", isStreaming = true, streamingOutput = "", streamingThinking = "", isThinkingCollapsed = false, thinkingTokenCount = 0, outputTokenCount = 0, errorMessage = null, pendingAttachments = emptyList()) }

        viewModelScope.launch {
            val existingConvId = _uiState.value.conversation?.id ?: 0L
            var activeConversationId = existingConvId
            val isNew = existingConvId <= 0
            if (isNew) {
                DebugLog.log("ChatVM", "Auto-creating conversation...")
                val s = _uiState.value
                val newConv = chatRepository.createConversation(
                    title = text.take(50),
                    provider = s.activeProvider,
                    temperature = s.temperature,
                    maxTokens = s.maxTokensUi,
                    contextRounds = s.contextRounds,
                    multimodalEnabled = s.multimodalEnabled,
                    topP = s.topP
                )
                activeConversationId = newConv.id
                _uiState.update { it.copy(conversation = newConv) }
                DebugLog.log("ChatVM", "Auto-created convId=$activeConversationId")
            }

            DebugLog.log("ChatVM", "Saving user message for convId=$activeConversationId")
            val userMessage = sendMessageUseCase(activeConversationId, text, attachments)

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
            // Add streaming placeholder to messages so stopGeneration can find it
            _uiState.update { it.copy(messages = it.messages + streamingMessage.copy(id = streamingId)) }

            streamJob?.cancel()
            streamJob = viewModelScope.launch {
                val conversation = _uiState.value.conversation ?: run {
                    _uiState.update {
                        it.copy(isStreaming = false, errorMessage = "Conversation not loaded")
                    }
                    return@launch
                }
                // Filter out streaming placeholder — never send empty STREAMING messages to API
                val messages = if (isNew) listOf(userMessage)
                    else _uiState.value.messages.filter { it.status != MessageStatus.STREAMING }
                DebugLog.log("ChatVM", "Sending ${messages.size} messages to stream")
                streamMessageUseCase(
                    conversation = conversation,
                    messages = messages,
                    enableSearch = _uiState.value.enableSearch
                ).collect { chunk ->
                    when (chunk) {
                        is StreamChunk.Content -> {
                            if (chunk.text.isNotEmpty()) {
                                _uiState.update {
                                    val newOutput = it.streamingOutput + chunk.text
                                    it.copy(
                                        streamingOutput = newOutput,
                                        outputTokenCount = (newOutput.length / 2.5).toLong()
                                    )
                                }
                            }
                        }
                        is StreamChunk.Thinking -> {
                            if (chunk.text.isNotEmpty()) {
                                _uiState.update {
                                    val newThinking = it.streamingThinking + chunk.text
                                    it.copy(
                                        streamingThinking = newThinking,
                                        thinkingTokenCount = (newThinking.length / 2.5).toLong()
                                    )
                                }
                            }
                        }
                        is StreamChunk.SearchStatus -> {
                            _uiState.update { it.copy(errorMessage = "Searching: ${chunk.query.take(50)}...") }
                        }
                        is StreamChunk.Done -> {
                            val fullContent = _uiState.value.streamingOutput
                            val cleanThinking = _uiState.value.streamingThinking.trimEnd()
                            DebugLog.log("VM", "=== Stream DONE, think=${cleanThinking.length} output=${fullContent.length} ===")
                            DebugLog.log("ChatVM", "StreamChunk.Done received")
                            chatRepository.updateMessageContent(streamingId, fullContent, cleanThinking.ifEmpty { null })
                            val completedMsg = Message(
                                id = streamingId,
                                conversationId = activeConversationId,
                                role = MessageRole.ASSISTANT,
                                content = fullContent,
                                thinking = cleanThinking.ifEmpty { null },
                                status = MessageStatus.COMPLETE
                            )
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    streamingOutput = "",
                                    streamingThinking = "",
                                    isThinkingCollapsed = false,
                                    thinkingTokenCount = 0,
                                    messages = it.messages.filterNot { m -> m.id == streamingId } + completedMsg
                                )
                            }
                            if (isNew) onConversationCreated?.invoke(activeConversationId)
                        }
                        is StreamChunk.Error -> {
                            DebugLog.log("VM", "Stream error: ${chunk.throwable.message}")
                            DebugLog.log("ChatVM", "StreamChunk.Error: ${chunk.throwable.message}")
                            chatRepository.updateMessageContent(
                                streamingId,
                                _uiState.value.streamingOutput,
                                null
                            )
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    streamingOutput = "",
                                    streamingThinking = "",
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
        val partial = _uiState.value.streamingOutput
        val streamingMsg = _uiState.value.messages.lastOrNull { it.status == MessageStatus.STREAMING }
        _uiState.update {
            it.copy(isStreaming = false, streamingOutput = "", streamingThinking = "",
                messages = if (partial.isEmpty()) it.messages.filterNot { m -> m.id == streamingMsg?.id } else it.messages)
        }
        if (streamingMsg != null && partial.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.updateMessageContent(streamingMsg.id, partial, null)
            }
        } else if (streamingMsg != null) {
            viewModelScope.launch { chatRepository.deleteMessage(streamingMsg.id) }
        }
    }

    fun toggleThinkingCollapse() {
        _uiState.update { it.copy(isThinkingCollapsed = !it.isThinkingCollapsed) }
    }

    fun regenerateMessage(assistantMessageId: Long) {
        if (_uiState.value.isStreaming) return
        val messages = _uiState.value.messages
        val idx = messages.indexOfFirst { it.id == assistantMessageId }
        if (idx <= 0) return
        val userMsg = messages.getOrNull(idx - 1) ?: return
        if (userMsg.role != MessageRole.USER) return

        // Remove old assistant message, then stream directly
        val newMessages = messages.filterNot { it.id == assistantMessageId }
        _uiState.update { it.copy(messages = newMessages, isStreaming = true, streamingOutput = "", streamingThinking = "", errorMessage = null) }
        viewModelScope.launch { chatRepository.deleteMessage(assistantMessageId) }

        val conv = _uiState.value.conversation ?: return
        val streamingMsg = Message(conversationId = conv.id, role = MessageRole.ASSISTANT, content = "", status = MessageStatus.STREAMING)
        viewModelScope.launch {
            val streamingId = chatRepository.saveMessage(streamingMsg)
            _uiState.update { it.copy(messages = it.messages + streamingMsg.copy(id = streamingId)) }
            streamMessageUseCase(conv, newMessages.filter { it.status != MessageStatus.STREAMING }, _uiState.value.enableSearch).collect { chunk ->
                when (chunk) {
                    is StreamChunk.Content -> { if (chunk.text.isNotEmpty()) _uiState.update { val o = it.streamingOutput + chunk.text; it.copy(streamingOutput = o, outputTokenCount = (o.length / 2.5).toLong()) } }
                    is StreamChunk.Thinking -> { if (chunk.text.isNotEmpty()) _uiState.update { val t = it.streamingThinking + chunk.text; it.copy(streamingThinking = t, thinkingTokenCount = (t.length / 2.5).toLong()) } }
                    is StreamChunk.SearchStatus -> { _uiState.update { it.copy(errorMessage = "Searching: ${chunk.query.take(50)}...") } }
                    is StreamChunk.Done -> {
                        val full = _uiState.value.streamingOutput; val thinking = _uiState.value.streamingThinking
                        chatRepository.updateMessageContent(streamingId, full, thinking.ifEmpty { null })
                        val completed = Message(id = streamingId, conversationId = conv.id, role = MessageRole.ASSISTANT, content = full, thinking = thinking.ifEmpty { null }, status = MessageStatus.COMPLETE)
                        _uiState.update { it.copy(isStreaming = false, streamingOutput = "", streamingThinking = "", messages = it.messages.filterNot { m -> m.id == streamingId } + completed) }
                    }
                    is StreamChunk.Error -> { chatRepository.updateMessageContent(streamingId, _uiState.value.streamingOutput, null); _uiState.update { it.copy(isStreaming = false) } }
                }
            }
        }
    }
}
