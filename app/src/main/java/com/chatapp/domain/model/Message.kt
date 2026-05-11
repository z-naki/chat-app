package com.chatapp.domain.model

data class Message(
    val id: Long = 0,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val thinking: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.COMPLETE
)

enum class MessageStatus {
    SENDING,
    STREAMING,
    COMPLETE,
    ERROR
}
