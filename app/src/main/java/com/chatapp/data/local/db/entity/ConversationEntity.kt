package com.chatapp.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.ProviderType

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "provider") val provider: String,
    @ColumnInfo(name = "temperature") val temperature: Float = 0.7f,
    @ColumnInfo(name = "max_tokens") val maxTokens: Int = 384_000,
    @ColumnInfo(name = "context_rounds") val contextRounds: Int = 20,
    @ColumnInfo(name = "top_p") val topP: Float = 0.9f,
    @ColumnInfo(name = "multimodal_enabled") val multimodalEnabled: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        provider = ProviderType.fromStringOrDefault(provider),
        temperature = temperature,
        maxTokens = maxTokens,
        contextRounds = contextRounds,
        topP = topP,
        multimodalEnabled = multimodalEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(conv: Conversation): ConversationEntity = ConversationEntity(
            id = conv.id,
            title = conv.title,
            provider = conv.provider.name,
            temperature = conv.temperature,
            maxTokens = conv.maxTokens,
            contextRounds = conv.contextRounds,
            topP = conv.topP,
            multimodalEnabled = conv.multimodalEnabled,
            createdAt = conv.createdAt,
            updatedAt = conv.updatedAt
        )
    }
}
