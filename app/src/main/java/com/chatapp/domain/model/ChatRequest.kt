package com.chatapp.domain.model

data class ChatRequest(
    val model: String,
    val messages: List<ProviderMessage>,
    val temperature: Float = 0.7f,
    val topP: Float? = null,
    val maxTokens: Int = 384_000,
    val enableSearch: Boolean = false,
    val extraParams: Map<String, String> = emptyMap()
)

data class ProviderMessage(
    val role: String,
    val content: String,
    val attachments: List<com.chatapp.domain.model.Attachment> = emptyList()
)

data class ChatResponse(
    val content: String,
    val thinking: String? = null
)
