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
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        provider = ProviderType.valueOf(provider),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(conv: Conversation): ConversationEntity = ConversationEntity(
            id = conv.id,
            title = conv.title,
            provider = conv.provider.name,
            createdAt = conv.createdAt,
            updatedAt = conv.updatedAt
        )
    }
}
