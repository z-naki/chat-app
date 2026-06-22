package com.chatapp.data.repository

import com.chatapp.data.local.db.dao.ConversationDao
import com.chatapp.data.local.db.dao.MessageDao
import com.chatapp.data.local.db.entity.ConversationEntity
import com.chatapp.data.local.db.entity.MessageEntity
import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.data.remote.provider.ProviderRouter
import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.ChatRequest
import com.chatapp.domain.model.ProviderMessage
import com.chatapp.domain.model.StreamChunk
import com.chatapp.domain.analyze.MultimodalAnalyzer
import com.chatapp.domain.repository.ChatRepository
import com.chatapp.util.DebugLog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val providerRouter: ProviderRouter,
    private val securePrefs: SecurePrefs,
    private val multimodalAnalyzer: MultimodalAnalyzer
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
        contextRounds: Int,
        multimodalEnabled: Boolean,
        topP: Float
    ): Conversation {
        val entity = ConversationEntity(
            title = title,
            provider = provider.name,
            temperature = temperature,
            maxTokens = maxTokens,
            contextRounds = contextRounds,
            topP = topP,
            multimodalEnabled = multimodalEnabled,
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

    override suspend fun fetchAvailableModels(): List<String> {
        return providerRouter.fetchAvailableModels()
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

    override suspend fun deleteMessage(messageId: Long) {
        messageDao.deleteById(messageId)
    }

    override suspend fun updateConversationMultimodal(conversationId: Long, enabled: Boolean) {
        conversationDao.updateMultimodal(conversationId, enabled)
    }

    override suspend fun updateConversationTopP(conversationId: Long, topP: Float) {
        conversationDao.updateTopP(conversationId, topP)
    }

    override suspend fun updateConversationProvider(conversationId: Long, provider: ProviderType) {
        conversationDao.updateProvider(conversationId, provider.name)
    }

    override suspend fun streamReply(
        conversation: Conversation,
        messages: List<Message>,
        enableSearch: Boolean
    ): Flow<StreamChunk> {
        val provider = providerRouter.resolve(conversation.provider)
        val trimmed = trimContext(messages, conversation.contextRounds)
        val multimodalActive = conversation.multimodalEnabled
        val providerMessages = coroutineScope {
            trimmed.map { msg ->
                // Identify attachments the provider doesn't support natively
                val unsupported = if (multimodalActive) {
                    msg.attachments.filter { !provider.supportsFileType(it.mimeType) }
                } else emptyList()

                // Route unsupported attachments through multimodal analyzer (parallel per message)
                val analyzedTexts = unsupported.map { att ->
                    async {
                        multimodalAnalyzer.analyze(att).fold(
                            onSuccess = { fc -> fc.text },
                            onFailure = { e ->
                                DebugLog.log("Multimodal", "Failed: ${att.name}: ${e.message}")
                                "[Analyze failed: ${att.name}]"
                            }
                        )
                    }
                }.map { it.await() }

                val effectiveContent = if (analyzedTexts.isNotEmpty()) {
                    val extra = analyzedTexts.joinToString("\n\n")
                    if (msg.content.isNotBlank()) msg.content + "\n\n" + extra else extra
                } else {
                    msg.content
                }

                ProviderMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = effectiveContent,
                    attachments = msg.attachments.filter { att ->
                        // Only send attachments the provider supports natively
                        provider.supportsFileType(att.mimeType)
                    }
                )
            }
        }
        val model = securePrefs.getProviderModel(conversation.provider.name)
            .ifEmpty { getDefaultModel(conversation.provider) }
        val systemPrompt = securePrefs.getSystemPrompt(conversation.provider.name).ifEmpty { null }
        val customParams = securePrefs.getCustomParams(conversation.provider.name).ifEmpty { null }
        val request = ChatRequest(
            model = model,
            messages = providerMessages,
            temperature = conversation.temperature,
            topP = conversation.topP,
            maxTokens = conversation.maxTokens,
            enableSearch = enableSearch,
            systemPrompt = systemPrompt,
            customParams = customParams
        )
        return provider.stream(request)
    }

    private fun getDefaultModel(provider: ProviderType): String = when (provider) {
        ProviderType.DEEPSEEK -> "deepseek-v4-pro"
        ProviderType.OPENAI -> "gpt-4o"
        ProviderType.ANTHROPIC -> "claude-sonnet-4-6"
        ProviderType.GEMINI -> "gemini-2.5-flash"
        ProviderType.MOONSHOT -> "moonshot-v1-128k"
        ProviderType.QWEN -> "qwen-max"
        ProviderType.CUSTOM_1, ProviderType.CUSTOM_2, ProviderType.CUSTOM_3 -> ""
    }

    private fun trimContext(messages: List<Message>, contextRounds: Int): List<Message> {
        val effective = maxOf(contextRounds, 1)
        val maxMessages = effective * 2
        return if (messages.size > maxMessages) messages.takeLast(maxMessages) else messages
    }
}
