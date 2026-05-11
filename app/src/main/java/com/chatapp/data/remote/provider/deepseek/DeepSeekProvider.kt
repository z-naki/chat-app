package com.chatapp.data.remote.provider.deepseek

import android.util.Log
import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.data.remote.provider.AiProvider
import com.chatapp.data.remote.sse.SseClient
import com.chatapp.data.remote.sse.SseEvent
import com.chatapp.domain.model.ChatRequest
import com.chatapp.domain.model.ChatResponse
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.StreamChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekProvider @Inject constructor(
    private val sseClient: SseClient,
    private val securePrefs: SecurePrefs,
    private val json: Json
) : AiProvider {

    override val type: ProviderType = ProviderType.DEEPSEEK

    companion object {
        private const val BASE_URL = "https://api.deepseek.com"
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        error("Use stream() for chat completions")
    }

    override fun stream(request: ChatRequest): Flow<StreamChunk> {
        val apiKey: String
        try {
            apiKey = securePrefs.getApiKey("DEEPSEEK")
                ?: return flow {
                    emit(StreamChunk.Error(IllegalStateException("DeepSeek API Key not configured")))
                }
        } catch (e: Exception) {
            Log.w("DeepSeekProvider", "Error retrieving API key")
            return flow {
                emit(StreamChunk.Error(e))
            }
        }

        return sseClient.connect(
            url = "$BASE_URL/v1/chat/completions",
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json"
            ),
            body = buildRequestBody(request)
        ).map { event ->
            when (event) {
                is SseEvent.Data -> parseChunk(event.text)
                is SseEvent.Done -> StreamChunk.Done
                is SseEvent.Error -> mapHttpError(event.throwable)
            }
        }
    }

    private fun buildRequestBody(request: ChatRequest): String {
        val obj = buildJsonObject {
            put("model", "deepseek-v4-pro")
            put("stream", true)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature.toDouble())
            putJsonArray("messages") {
                request.messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }
            if (request.enableSearch) {
                putJsonArray("tools") {
                    add(buildJsonObject {
                        put("type", "web_search")
                    })
                }
            }
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    private fun parseChunk(raw: String): StreamChunk {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val choices = obj["choices"]?.jsonArray ?: return StreamChunk.Content("")

            for (choice in choices) {
                val choiceObj = choice.jsonObject
                val delta = choiceObj["delta"]?.jsonObject ?: continue

                // Check for thinking content (deepseek-v4-pro may emit reasoning)
                val thinking = delta["reasoning_content"]?.jsonPrimitive?.content
                if (!thinking.isNullOrEmpty()) {
                    return StreamChunk.Thinking(thinking)
                }

                val content = delta["content"]?.jsonPrimitive?.content
                if (!content.isNullOrEmpty()) {
                    return StreamChunk.Content(content)
                }
            }

            StreamChunk.Content("")
        } catch (e: Exception) {
            Log.e("DeepSeekProvider", "Failed to parse SSE chunk", e)
            StreamChunk.Content("")
        }
    }

    private fun mapHttpError(throwable: Throwable): StreamChunk {
        return StreamChunk.Error(throwable)
    }
}
