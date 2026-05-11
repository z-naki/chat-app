package com.chatapp.domain.usecase

import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.StreamChunk
import com.chatapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StreamMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(
        conversation: Conversation,
        messages: List<Message>,
        enableSearch: Boolean
    ): Flow<StreamChunk> {
        return chatRepository.streamReply(
            conversation = conversation,
            messages = messages,
            enableSearch = enableSearch
        )
    }
}
