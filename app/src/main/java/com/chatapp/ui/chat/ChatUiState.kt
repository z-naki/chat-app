package com.chatapp.ui.chat

import com.chatapp.domain.model.Attachment
import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.ProviderType

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val streamingOutput: String = "",
    val streamingThinking: String = "",
    val isThinkingCollapsed: Boolean = false,
    val thinkingTokenCount: Long = 0,
    val outputTokenCount: Long = 0,
    val enableSearch: Boolean = false,
    val multimodalEnabled: Boolean = false,
    val errorMessage: String? = null,
    val pendingAttachments: List<Attachment> = emptyList(),
    val availableModels: List<String> = emptyList(),
    val showModelPicker: Boolean = false,
    val currentModel: String = "",
    val activeProvider: ProviderType = ProviderType.DEEPSEEK,
    val topP: Float = 0.9f,
    val temperature: Float = 0.7f,
    val contextRounds: Int = 20,
    val maxTokensUi: Int = 384_000
)
