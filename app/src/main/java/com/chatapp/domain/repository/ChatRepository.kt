package com.chatapp.domain.repository

import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.StreamChunk
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<Conversation>>
    suspend fun getConversation(conversationId: Long): Conversation?
    suspend fun createConversation(
        title: String,
        provider: ProviderType,
        temperature: Float = 0.7f,
        maxTokens: Int = 384_000,
        contextRounds: Int = 20
    ): Conversation
    suspend fun updateConversationParameters(
        conversationId: Long,
        temperature: Float,
        maxTokens: Int,
        contextRounds: Int
    )
    suspend fun deleteConversation(conversationId: Long)
    fun getMessages(conversationId: Long): Flow<List<Message>>
    suspend fun saveMessage(message: Message): Long
    suspend fun updateMessageContent(messageId: Long, content: String, thinking: String?)
    fun streamReply(
        conversation: Conversation,
        messages: List<Message>,
        enableSearch: Boolean
    ): Flow<StreamChunk>
}
