package com.chatapp.ui.conversationlist

import com.chatapp.domain.model.Conversation

data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true
)
