package com.chatapp.data.remote.provider.openai

import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.util.DebugLog
import com.chatapp.data.remote.provider.AiProvider
import com.chatapp.data.remote.sse.SseClient
import com.chatapp.data.remote.sse.SseEvent
import com.chatapp.domain.model.ChatRequest
import com.chatapp.domain.model.ChatResponse
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.StreamChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiProvider @Inject constructor(
    private val sseClient: SseClient,
    private val securePrefs: SecurePrefs,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) : AiProvider {

    override val type: ProviderType = ProviderType.OPENAI
    override val supportsThinking: Boolean = true

    companion object {
        private const val BASE_URL = "https://api.openai.com"
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        error("Use stream() for chat completions")
    }

    override suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = securePrefs.getApiKey("OPENAI") ?: return@withContext emptyList()
            val baseUrl = securePrefs.getProviderBaseUrl("OPENAI").ifEmpty { BASE_URL }
            val client = okHttpClient.newBuilder()
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("$baseUrl/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            if (!response.isSuccessful) return@withContext emptyList()
            val obj = json.parseToJsonElement(body).jsonObject
            val data = obj["data"]?.jsonArray ?: return@withContext emptyList()
            data.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }
                .filter { id ->
                    id.isNotEmpty() &&
                    !id.contains("embed") && !id.contains("tts") && !id.contains("whisper") &&
                    !id.contains("dall-e") && !id.contains("moderation") && !id.contains("babbage") &&
                    !id.contains("davinci") && !id.contains("omni") && id != "gpt-3.5-turbo"
                }
                .sortedByDescending { it }
        } catch (e: Exception) {
            DebugLog.log("OpenAI", "fetchModels failed: ${e.message}")
            emptyList()
        }
    }

    override fun stream(request: ChatRequest): Flow<StreamChunk> {
        DebugLog.log("OpenAI", "stream() called")
        val apiKey: String
        try {
            apiKey = securePrefs.getApiKey("OPENAI")
                ?: return flow { emit(StreamChunk.Error(IllegalStateException("OpenAI API Key not configured"))) }
        } catch (e: Exception) {
            return flow { emit(StreamChunk.Error(e)) }
        }

        val baseUrl = securePrefs.getProviderBaseUrl("OPENAI").ifEmpty { BASE_URL }
        val body = buildRequestBody(request)
        DebugLog.log("OpenAI", "Connecting to $baseUrl")
        return sseClient.connect(
            url = "$baseUrl/v1/chat/completions",
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json"
            ),
            body = body
        ).map { event ->
            when (event) {
                is SseEvent.Data -> parseChunk(event.text)
                is SseEvent.Done -> StreamChunk.Done
                is SseEvent.Error -> StreamChunk.Error(event.throwable)
            }
        }
    }

    private fun buildRequestBody(request: ChatRequest): String {
        val model = securePrefs.getProviderModel("OPENAI").ifEmpty { "gpt-4o" }
        val isReasoningModel = model.contains("o1") || model.contains("o3") || model.contains("o4")
        // Cap max_tokens: reasoning models support up to 100K, others 16K
        val effectiveMaxTokens = if (isReasoningModel) {
            minOf(request.maxTokens, 100_000)
        } else {
            minOf(request.maxTokens, 16_384)
        }
        val obj = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("stream_options", buildJsonObject {
                put("include_usage", true)
            })
            if (isReasoningModel) {
                put("max_completion_tokens", effectiveMaxTokens)
                put("reasoning_effort", "high")
            } else {
                put("max_tokens", effectiveMaxTokens)
                put("temperature", request.temperature.toDouble().let { (it * 100).toInt() / 100.0 })
                request.topP?.let { put("top_p", it.toDouble().let { v -> (v * 100).toInt() / 100.0 }) }
            }
            putJsonArray("messages") {
                request.systemPrompt?.let { sp ->
                    add(buildJsonObject { put("role", "system"); put("content", sp) })
                }
                request.messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        val imageAttachments = msg.attachments.filter {
                            it.type == com.chatapp.domain.model.AttachmentType.IMAGE && it.dataBase64.isNotBlank()
                        }
                        if (imageAttachments.isNotEmpty()) {
                            putJsonArray("content") {
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", msg.content)
                                })
                                imageAttachments.forEach { att ->
                                    add(buildJsonObject {
                                        put("type", "image_url")
                                        put("image_url", buildJsonObject {
                                            put("url", "data:${att.mimeType};base64,${att.dataBase64}")
                                        })
                                    })
                                }
                            }
                        } else {
                            put("content", msg.content)
                        }
                    })
                }
            }
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    override fun supportsFileType(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    private fun parseChunk(raw: String): StreamChunk {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            // Check for API error in response
            val error = obj["error"]?.jsonObject
            if (error != null) {
                val msg = error["message"]?.jsonPrimitive?.content ?: "Unknown API error"
                return StreamChunk.Error(RuntimeException(msg))
            }
            val choices = obj["choices"]?.jsonArray ?: return StreamChunk.Content("")

            for (choice in choices) {
                val choiceObj = choice.jsonObject
                val delta = choiceObj["delta"]?.jsonObject ?: choiceObj["message"]?.jsonObject ?: continue

                val reasoningElem = delta["reasoning_content"]
                val thinking = reasoningElem?.jsonPrimitive?.content
                if (!thinking.isNullOrEmpty() && thinking != "null") {
                    DebugLog.log("OpenAI", "Thinking: ${thinking.take(100)}")
                    return StreamChunk.Thinking(thinking)
                }

                val contentElem = delta["content"]
                val content = contentElem?.jsonPrimitive?.content
                if (!content.isNullOrEmpty() && content != "null") {
                    return StreamChunk.Content(content)
                }
            }
            StreamChunk.Content("")
        } catch (e: Exception) {
            DebugLog.log("OpenAI", "Parse error: ${e.message}")
            StreamChunk.Content("")
        }
    }
}
