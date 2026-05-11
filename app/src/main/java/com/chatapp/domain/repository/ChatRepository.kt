package com.chatapp.domain.repository

import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.StreamChunk
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<Conversation>>
    suspend fun createConversation(title: String, provider: ProviderType): Conversation
    suspend fun deleteConversation(conversationId: Long)
    fun getMessages(conversationId: Long): Flow<List<Message>>
    suspend fun saveMessage(message: Message): Long
    suspend fun updateMessageContent(messageId: Long, content: String, thinking: String?)
    fun streamReply(providerType: ProviderType, messages: List<Message>, enableSearch: Boolean): Flow<StreamChunk>
}
