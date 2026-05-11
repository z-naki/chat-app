package com.chatapp.ui.chat

import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.Message

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val enableSearch: Boolean = false,
    val errorMessage: String? = null
)
