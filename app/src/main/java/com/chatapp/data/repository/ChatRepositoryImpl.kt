package com.chatapp.data.repository

import com.chatapp.data.local.db.dao.ConversationDao
import com.chatapp.data.local.db.dao.MessageDao
import com.chatapp.data.local.db.entity.ConversationEntity
import com.chatapp.data.local.db.entity.MessageEntity
import com.chatapp.data.remote.provider.ProviderRouter
import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.ChatRequest
import com.chatapp.domain.model.ProviderMessage
import com.chatapp.domain.model.StreamChunk
import com.chatapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val providerRouter: ProviderRouter
) : ChatRepository {

    override fun getConversations(): Flow<List<Conversation>> {
        return conversationDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConversation(conversationId: Long): Conversation? {
        return conversationDao.getById(conversationId)?.toDomain()
    }

    override suspend fun createConversation(
        title: String,
        provider: ProviderType,
        temperature: Float,
        maxTokens: Int,
        contextRounds: Int
    ): Conversation {
        val entity = ConversationEntity(
            title = title,
            provider = provider.name,
            temperature = temperature,
            maxTokens = maxTokens,
            contextRounds = contextRounds,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = conversationDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun updateConversationParameters(
        conversationId: Long,
        temperature: Float,
        maxTokens: Int,
        contextRounds: Int
    ) {
        conversationDao.updateParameters(conversationId, temperature, maxTokens, contextRounds)
    }

    override suspend fun deleteConversation(conversationId: Long) {
        conversationDao.delete(conversationId)
    }

    override fun getMessages(conversationId: Long): Flow<List<Message>> {
        return messageDao.getByConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveMessage(message: Message): Long {
        return messageDao.insert(MessageEntity.fromDomain(message))
    }

    override suspend fun updateMessageContent(messageId: Long, content: String, thinking: String?) {
        messageDao.updateContent(messageId, content, thinking)
    }

    override fun streamReply(
        conversation: Conversation,
        messages: List<Message>,
        enableSearch: Boolean
    ): Flow<StreamChunk> {
        val provider = providerRouter.resolve(conversation.provider)
        val trimmed = trimContext(messages, conversation.contextRounds)
        val providerMessages = trimmed.map { msg ->
            ProviderMessage(
                role = when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                },
                content = msg.content
            )
        }
        val request = ChatRequest(
            model = "deepseek-v4-pro",
            messages = providerMessages,
            temperature = conversation.temperature,
            maxTokens = conversation.maxTokens,
            enableSearch = enableSearch
        )
        return provider.stream(request)
    }

    private fun trimContext(messages: List<Message>, contextRounds: Int): List<Message> {
        val effective = maxOf(contextRounds, 1)
        val maxMessages = effective * 2
        return if (messages.size > maxMessages) messages.takeLast(maxMessages) else messages
    }
}
