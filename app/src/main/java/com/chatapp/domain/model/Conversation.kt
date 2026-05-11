package com.chatapp.domain.model

data class Conversation(
    val id: Long = 0,
    val title: String,
    val provider: ProviderType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
