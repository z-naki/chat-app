package com.chatapp.domain.usecase

import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.repository.ChatRepository
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(title: String, provider: ProviderType): Conversation {
        return chatRepository.createConversation(title, provider)
    }
}
