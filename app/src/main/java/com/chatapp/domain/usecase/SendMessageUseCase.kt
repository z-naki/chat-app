package com.chatapp.domain.usecase

import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.MessageStatus
import com.chatapp.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(conversationId: Long, content: String): Message {
        val message = Message(
            conversationId = conversationId,
            role = MessageRole.USER,
            content = content,
            status = MessageStatus.COMPLETE
        )
        val id = chatRepository.saveMessage(message)
        return message.copy(id = id)
    }
}
