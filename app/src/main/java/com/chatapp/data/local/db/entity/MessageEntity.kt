package com.chatapp.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chatapp.domain.model.Message
import com.chatapp.domain.model.MessageRole
import com.chatapp.domain.model.MessageStatus

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversation_id")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conversation_id") val conversationId: Long,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "thinking") val thinking: String?,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "status") val status: String
) {
    fun toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        role = MessageRole.fromStringOrDefault(role),
        content = content,
        thinking = thinking,
        timestamp = timestamp,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.COMPLETE)
    )

    companion object {
        fun fromDomain(msg: Message): MessageEntity = MessageEntity(
            id = msg.id,
            conversationId = msg.conversationId,
            role = msg.role.name,
            content = msg.content,
            thinking = msg.thinking,
            timestamp = msg.timestamp,
            status = msg.status.name
        )
    }
}
