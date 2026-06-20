package com.chatapp.domain.usecase

import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.repository.ChatRepository
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        title: String,
        provider: ProviderType,
        temperature: Float = 0.7f,
        maxTokens: Int = 384_000,
        contextRounds: Int = 20,
        multimodalEnabled: Boolean = false,
        topP: Float = 0.9f
    ): Conversation {
        return chatRepository.createConversation(title, provider, temperature, maxTokens, contextRounds, multimodalEnabled, topP)
    }
}
