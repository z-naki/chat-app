package com.chatapp.data.remote.provider.anthropic

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
class AnthropicProvider @Inject constructor(
    private val sseClient: SseClient,
    private val securePrefs: SecurePrefs,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) : AiProvider {

    override val type: ProviderType = ProviderType.ANTHROPIC
    override val supportsThinking: Boolean = true

    companion object {
        private const val BASE_URL = "https://api.anthropic.com"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private val DEFAULT_MODELS = listOf(
            "claude-sonnet-4-6",
            "claude-opus-4-8",
            "claude-haiku-4-5-20251001",
            "claude-fable-5"
        )
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        error("Use stream() for chat completions")
    }

    override suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            // Anthropic has no official list-models endpoint; return well-known models
            val apiKey = securePrefs.getApiKey("ANTHROPIC") ?: return@withContext DEFAULT_MODELS
            // Verify API key works by checking a simple health endpoint or just return defaults
            DEFAULT_MODELS
        } catch (e: Exception) {
            DebugLog.log("Anthropic", "fetchModels failed: ${e.message}")
            DEFAULT_MODELS
        }
    }

    override fun stream(request: ChatRequest): Flow<StreamChunk> {
        DebugLog.log("Anthropic", "stream() called")
        val apiKey: String
        try {
            apiKey = securePrefs.getApiKey("ANTHROPIC")
                ?: return flow { emit(StreamChunk.Error(IllegalStateException("Anthropic API Key not configured"))) }
        } catch (e: Exception) {
            return flow { emit(StreamChunk.Error(e)) }
        }

        val baseUrl = securePrefs.getProviderBaseUrl("ANTHROPIC").ifEmpty { BASE_URL }
        val body = buildRequestBody(request)
        return sseClient.connect(
            url = "$baseUrl/v1/messages",
            headers = mapOf(
                "x-api-key" to apiKey,
                "anthropic-version" to ANTHROPIC_VERSION,
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
        val model = securePrefs.getProviderModel("ANTHROPIC").ifEmpty { "claude-sonnet-4-6" }
        val enableThinking = model.contains("opus") || model.contains("sonnet") || model.contains("haiku") || model.contains("fable")
        // Cap max_tokens: Claude models support up to 16384 output tokens
        val effectiveMaxTokens = minOf(request.maxTokens, 16384)
        val thinkingBudget = if (enableThinking) {
            minOf(maxOf(effectiveMaxTokens / 4, 1024), 16384)
        } else 0
        val obj = buildJsonObject {
            put("model", model)
            put("max_tokens", effectiveMaxTokens)
            put("stream", true)
            put("temperature", request.temperature.toDouble().let { (it * 100).toInt() / 100.0 })
            if (enableThinking && thinkingBudget >= 1024) {
                put("thinking", buildJsonObject {
                    put("type", "enabled")
                    put("budget_tokens", thinkingBudget)
                })
            }
            putJsonArray("messages") {
                request.messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        // Anthropic uses content array format
                        putJsonArray("content") {
                            val imageAttachments = msg.attachments.filter {
                                it.type == com.chatapp.domain.model.AttachmentType.IMAGE && it.dataBase64.isNotBlank()
                            }
                            imageAttachments.forEach { att ->
                                add(buildJsonObject {
                                    put("type", "image")
                                    put("source", buildJsonObject {
                                        put("type", "base64")
                                        put("media_type", att.mimeType.ifEmpty { "image/png" })
                                        put("data", att.dataBase64)
                                    })
                                })
                            }
                            if (msg.content.isNotBlank()) {
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", msg.content)
                                })
                            } else if (imageAttachments.isEmpty()) {
                                // Ensure at least one content block exists
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", "")
                                })
                            }
                        }
                    })
                }
            }
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    override fun supportsFileType(mimeType: String): Boolean {
        return mimeType.startsWith("image/") || mimeType == "application/pdf"
    }

    private fun parseChunk(raw: String): StreamChunk {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val eventType = obj["type"]?.jsonPrimitive?.content ?: ""

            when (eventType) {
                "message_start" -> {
                    // Message metadata — ignore but log for debugging
                    val message = obj["message"]?.jsonObject
                    val model = message?.get("model")?.jsonPrimitive?.content
                    DebugLog.log("Anthropic", "message_start model=$model")
                    StreamChunk.Content("")
                }
                "content_block_start" -> {
                    // Content block metadata — ignore but detect type
                    val block = obj["content_block"]?.jsonObject
                    val blockType = block?.get("type")?.jsonPrimitive?.content
                    DebugLog.log("Anthropic", "content_block_start type=$blockType")
                    StreamChunk.Content("")
                }
                "content_block_delta" -> {
                    val delta = obj["delta"]?.jsonObject ?: return StreamChunk.Content("")
                    val deltaType = delta["type"]?.jsonPrimitive?.content ?: ""
                    when (deltaType) {
                        "thinking_delta" -> {
                            val thinking = delta["thinking"]?.jsonPrimitive?.content ?: ""
                            if (thinking.isNotEmpty() && thinking != "null") {
                                StreamChunk.Thinking(thinking)
                            } else StreamChunk.Content("")
                        }
                        "text_delta" -> {
                            val text = delta["text"]?.jsonPrimitive?.content ?: ""
                            if (text.isNotEmpty() && text != "null") {
                                StreamChunk.Content(text)
                            } else StreamChunk.Content("")
                        }
                        "input_json_delta" -> {
                            // Tool use JSON delta — emit as content for visibility
                            val partialJson = delta["partial_json"]?.jsonPrimitive?.content ?: ""
                            if (partialJson.isNotEmpty()) {
                                StreamChunk.Content(partialJson)
                            } else StreamChunk.Content("")
                        }
                        "signature_delta" -> {
                            // Thinking signature (when display=omitted) — ignore for now
                            StreamChunk.Content("")
                        }
                        "citations_delta" -> {
                            // Citations from sources — emit as content
                            val citation = delta["citation"]?.jsonObject
                            val citedText = citation?.get("cited_text")?.jsonPrimitive?.content ?: ""
                            if (citedText.isNotEmpty()) StreamChunk.Content(citedText)
                            else StreamChunk.Content("")
                        }
                        else -> StreamChunk.Content("")
                    }
                }
                "content_block_stop" -> {
                    // Block complete — no content to emit
                    StreamChunk.Content("")
                }
                "message_delta" -> {
                    // Stop reason and usage info
                    val stopReason = obj["delta"]?.jsonObject?.get("stop_reason")?.jsonPrimitive?.content
                    DebugLog.log("Anthropic", "message_delta stop_reason=$stopReason")
                    StreamChunk.Content("")
                }
                "message_stop" -> {
                    // Final event — stream is complete
                    StreamChunk.Done
                }
                else -> StreamChunk.Content("")
            }
        } catch (e: Exception) {
            DebugLog.log("Anthropic", "Parse error: ${e.message}")
            StreamChunk.Content("")
        }
    }
}
