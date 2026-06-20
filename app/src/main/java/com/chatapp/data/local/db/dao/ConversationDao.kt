package com.chatapp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatapp.data.local.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updated_at DESC")
    fun getAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("UPDATE conversations SET title = :title, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET updated_at = :updatedAt WHERE id = :id")
    suspend fun updateTimestamp(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET temperature = :temperature, max_tokens = :maxTokens, context_rounds = :contextRounds WHERE id = :id")
    suspend fun updateParameters(id: Long, temperature: Float, maxTokens: Int, contextRounds: Int)

    @Query("UPDATE conversations SET top_p = :topP WHERE id = :id")
    suspend fun updateTopP(id: Long, topP: Float)

    @Query("UPDATE conversations SET multimodal_enabled = :enabled WHERE id = :id")
    suspend fun updateMultimodal(id: Long, enabled: Boolean)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: Long)
}
