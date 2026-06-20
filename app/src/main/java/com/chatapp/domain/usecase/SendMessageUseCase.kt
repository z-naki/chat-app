package com.chatapp.domain.usecase

import com.chatapp.domain.model.Attachment
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.MessageStatus
import com.chatapp.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        conversationId: Long,
        content: String,
        attachments: List<Attachment> = emptyList()
    ): Message {
        val message = Message(
            conversationId = conversationId,
            role = MessageRole.USER,
            content = content,
            attachments = attachments,
            status = MessageStatus.COMPLETE
        )
        val id = chatRepository.saveMessage(message)
        return message.copy(id = id)
    }
}
