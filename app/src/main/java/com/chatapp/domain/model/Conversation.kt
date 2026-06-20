package com.chatapp.domain.model

data class Conversation(
    val id: Long = 0,
    val title: String,
    val provider: ProviderType,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 384_000,
    val contextRounds: Int = 20,
    val multimodalEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
