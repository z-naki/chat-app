package com.chatapp.data.remote.provider

import com.chatapp.domain.model.ChatRequest
import com.chatapp.domain.model.ChatResponse
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.StreamChunk
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val type: ProviderType
    val supportsThinking: Boolean get() = false
    val supportsStreaming: Boolean get() = true
    suspend fun chat(request: ChatRequest): ChatResponse
    fun stream(request: ChatRequest): Flow<StreamChunk>
    suspend fun fetchAvailableModels(): List<String>
    fun supportsFileType(mimeType: String): Boolean = false
}
