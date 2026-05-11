package com.chatapp.domain.usecase

import com.chatapp.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(conversationId: Long) {
        chatRepository.deleteConversation(conversationId)
    }
}
