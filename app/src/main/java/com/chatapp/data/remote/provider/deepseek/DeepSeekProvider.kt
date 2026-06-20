package com.chatapp.data.remote.provider.deepseek

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
class DeepSeekProvider @Inject constructor(
    private val sseClient: SseClient,
    private val securePrefs: SecurePrefs,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) : AiProvider {

    override val type: ProviderType = ProviderType.DEEPSEEK
    override val supportsThinking: Boolean = true

    override fun supportsFileType(mimeType: String): Boolean = mimeType.startsWith("image/")

    companion object {
        private const val BASE_URL = "https://api.deepseek.com"
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        error("Use stream() for chat completions")
    }

    override suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = securePrefs.getApiKey("DEEPSEEK") ?: return@withContext emptyList()
            val baseUrl = securePrefs.getProviderBaseUrl("DEEPSEEK").ifEmpty { BASE_URL }
            val client = okHttpClient.newBuilder()
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("$baseUrl/models")
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            if (!response.isSuccessful) return@withContext emptyList()
            val obj = json.parseToJsonElement(body).jsonObject
            val data = obj["data"]?.jsonArray ?: return@withContext emptyList()
            data.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            DebugLog.log("DeepSeek", "fetchModels failed: ${e.message}")
            emptyList()
        }
    }

    override fun stream(request: ChatRequest): Flow<StreamChunk> {
        DebugLog.log("DeepSeek", "stream() called with ${request.messages.size} messages")
        val apiKey: String
        try {
            apiKey = securePrefs.getApiKey("DEEPSEEK")
                ?: run {
                    DebugLog.log("DeepSeek", "API key NOT configured")
                    DebugLog.log("DS", "API key NOT configured")
                    return flow {
                        emit(StreamChunk.Error(IllegalStateException("DeepSeek API Key not configured")))
                    }
                }
        } catch (e: Exception) {
            DebugLog.log("DeepSeek", "Error retrieving API key: ${e.message}")
            return flow {
                emit(StreamChunk.Error(e))
            }
        }

        val baseUrl = securePrefs.getProviderBaseUrl("DEEPSEEK").ifEmpty { BASE_URL }
        val body = buildRequestBody(request)
        DebugLog.log("DeepSeek", "Connecting to $baseUrl, body=${body.take(300)}")
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
                is SseEvent.Error -> mapHttpError(event.throwable)
            }
        }
    }

    private fun buildRequestBody(request: ChatRequest): String {
        val model = securePrefs.getProviderModel("DEEPSEEK").ifEmpty { "deepseek-v4-pro" }
        val obj = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("max_tokens", request.maxTokens)
            // Only enable thinking for reasoning-capable models, not deepseek-chat
            val modelName = securePrefs.getProviderModel("DEEPSEEK").ifEmpty { "deepseek-v4-pro" }
            val isThinking = !modelName.contains("chat")
            // DeepSeek thinking mode ignores temperature/top_p — skip them to avoid confusion
            if (!isThinking) {
                put("temperature", request.temperature.toDouble().let { (it * 100).toInt() / 100.0 })
                request.topP?.let { put("top_p", it.toDouble().let { v -> (v * 100).toInt() / 100.0 }) }
            }
            if (isThinking) {
                put("thinking", buildJsonObject { put("type", "enabled") })
                put("reasoning_effort", "high")
            }
            putJsonArray("messages") {
                // System prompt
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
            if (request.enableSearch) {
                putJsonArray("tools") {
                    add(buildJsonObject {
                        put("type", "web_search")
                        put("web_search", buildJsonObject {
                            put("enable", true)
                        })
                    })
                }
                put("tool_choice", "auto")
            }
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    private fun parseChunk(raw: String): StreamChunk {
        return try {
            DebugLog.log("DeepSeek", "Raw SSE: ${raw.take(200)}")
            val obj = json.parseToJsonElement(raw).jsonObject
            val choices = obj["choices"]?.jsonArray ?: return StreamChunk.Content("")

            for (choice in choices) {
                val choiceObj = choice.jsonObject
                val delta = choiceObj["delta"]?.jsonObject
                val message = choiceObj["message"]?.jsonObject

                DebugLog.log("DS", "S0_choice keys=${choiceObj.keys}")
                if (delta != null) DebugLog.log("DS", "S0_delta keys=${delta.keys} vals=${delta.entries.joinToString { "${it.key}=${it.value}" }.take(200)}")
                if (message != null) DebugLog.log("DS", "S0_msg keys=${message.keys} vals=${message.entries.joinToString { "${it.key}=${it.value}" }.take(200)}")

                if (delta == null && message == null) continue
                val active = delta ?: message ?: continue

                // Check for search results
                val searchResults = active["search_results"]
                if (searchResults != null) {
                    DebugLog.log("DS", "S2_SEARCH found search_results")
                    val query = active["search_query"]?.jsonPrimitive?.content
                    if (!query.isNullOrEmpty()) {
                        DebugLog.log("DS", "S4_RET SearchStatus('${query.take(100)}')")
                        return StreamChunk.SearchStatus(query)
                    }
                }

                // Check for thinking content (deepseek-v4-pro may emit reasoning)
                val thinkingElem = active["reasoning_content"]
                val thinking = thinkingElem?.jsonPrimitive?.content
                if (!thinking.isNullOrEmpty() && thinking != "null") {
                    DebugLog.log("DS", "S4_RET Thinking('${thinking.take(100)}')")
                    return StreamChunk.Thinking(thinking)
                }

                val contentElem = active["content"]
                val content = contentElem?.jsonPrimitive?.content
                if (!content.isNullOrEmpty() && content != "null") {
                    DebugLog.log("DS", "S4_RET Content('${content.take(100)}')")
                    return StreamChunk.Content(content)
                }
            }

            DebugLog.log("DS", "S4_RET EMPTY")
            StreamChunk.Content("")
        } catch (e: Exception) {
            DebugLog.log("DS", "Failed to parse SSE chunk: ${e.message}")
            StreamChunk.Content("")
        }
    }

    private fun mapHttpError(throwable: Throwable): StreamChunk {
        return StreamChunk.Error(throwable)
    }
}
