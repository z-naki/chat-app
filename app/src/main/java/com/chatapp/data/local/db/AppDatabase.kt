package com.chatapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chatapp.data.local.db.dao.ConversationDao
import com.chatapp.data.local.db.dao.MessageDao
import com.chatapp.data.local.db.entity.ConversationEntity
import com.chatapp.data.local.db.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
