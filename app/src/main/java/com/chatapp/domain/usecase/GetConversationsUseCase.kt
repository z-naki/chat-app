package com.chatapp.domain.usecase

import com.chatapp.domain.model.Conversation
import com.chatapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return chatRepository.getConversations()
    }
}
