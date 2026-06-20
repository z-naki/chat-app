package com.chatapp.data.remote.provider.gemini

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
class GeminiProvider @Inject constructor(
    private val sseClient: SseClient,
    private val securePrefs: SecurePrefs,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) : AiProvider {

    override val type: ProviderType = ProviderType.GEMINI
    override val supportsThinking: Boolean = true

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com"
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        error("Use stream() for chat completions")
    }

    override suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = securePrefs.getApiKey("GEMINI") ?: return@withContext emptyList()
            val baseUrl = securePrefs.getProviderBaseUrl("GEMINI").ifEmpty { BASE_URL }
            val client = okHttpClient.newBuilder()
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder()
                .url("$baseUrl/v1beta/models?key=$apiKey")
                .get()
                .build()
            val response = client.newCall(req).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            if (!response.isSuccessful) return@withContext emptyList()
            val obj = json.parseToJsonElement(body).jsonObject
            val models = obj["models"]?.jsonArray ?: return@withContext emptyList()
            models.mapNotNull { model ->
                val modelObj = model.jsonObject
                val name = modelObj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val methods = modelObj["supportedGenerationMethods"]?.jsonArray
                    ?.map { it.jsonPrimitive.content } ?: emptyList()
                // Only include models supporting generateContent, exclude vision-only and embedding models
                if (methods.contains("generateContent") &&
                    !name.contains("embedding") && !name.contains("aqa")) {
                    name.removePrefix("models/")
                } else null
            }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            DebugLog.log("Gemini", "fetchModels failed: ${e.message}")
            emptyList()
        }
    }

    override fun stream(request: ChatRequest): Flow<StreamChunk> {
        DebugLog.log("Gemini", "stream() called")
        val apiKey: String
        try {
            apiKey = securePrefs.getApiKey("GEMINI")
                ?: return flow { emit(StreamChunk.Error(IllegalStateException("Gemini API Key not configured"))) }
        } catch (e: Exception) {
            return flow { emit(StreamChunk.Error(e)) }
        }

        val model = securePrefs.getProviderModel("GEMINI").ifEmpty { "gemini-2.5-flash" }
        val baseUrl = securePrefs.getProviderBaseUrl("GEMINI").ifEmpty { BASE_URL }
        val body = buildRequestBody(request)
        return sseClient.connect(
            url = "$baseUrl/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey",
            headers = mapOf("Content-Type" to "application/json"),
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
        // Separate system messages for systemInstruction
        val systemMessages = request.messages.filter { it.role == "system" }
        val chatMessages = request.messages.filter { it.role != "system" }

        // Cap maxOutputTokens: Gemini 2.5 models support up to 65536
        val effectiveMaxTokens = minOf(request.maxTokens, 65536)

        val obj = buildJsonObject {
            // System instruction (if any system messages exist)
            if (systemMessages.isNotEmpty()) {
                put("systemInstruction", buildJsonObject {
                    putJsonArray("parts") {
                        systemMessages.forEach { sysMsg ->
                            add(buildJsonObject {
                                put("text", sysMsg.content)
                            })
                        }
                    }
                })
            }
            putJsonArray("contents") {
                chatMessages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", if (msg.role == "assistant") "model" else msg.role)
                        putJsonArray("parts") {
                            val imageAttachments = msg.attachments.filter {
                                it.type == com.chatapp.domain.model.AttachmentType.IMAGE && it.dataBase64.isNotBlank()
                            }
                            imageAttachments.forEach { att ->
                                add(buildJsonObject {
                                    put("inlineData", buildJsonObject {
                                        put("mimeType", att.mimeType.ifEmpty { "image/png" })
                                        put("data", att.dataBase64)
                                    })
                                })
                            }
                            if (msg.content.isNotBlank() || imageAttachments.isEmpty()) {
                                add(buildJsonObject {
                                    put("text", msg.content)
                                })
                            }
                        }
                    })
                }
            }
            put("generationConfig", buildJsonObject {
                put("temperature", request.temperature.toDouble().let { (it * 100).toInt() / 100.0 })
                request.topP?.let { put("topP", it.toDouble().let { v -> (v * 100).toInt() / 100.0 }) }
                put("maxOutputTokens", effectiveMaxTokens)
            })
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    override fun supportsFileType(mimeType: String): Boolean {
        return mimeType.startsWith("image/") ||
            mimeType.startsWith("audio/") ||
            mimeType.startsWith("video/") ||
            mimeType == "application/pdf" ||
            mimeType == "text/plain" ||
            mimeType == "text/csv"
    }

    private fun parseChunk(raw: String): StreamChunk {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val candidates = obj["candidates"]?.jsonArray ?: return StreamChunk.Content("")
            for (candidate in candidates) {
                val candidateObj = candidate.jsonObject
                val content = candidateObj["content"]?.jsonObject ?: continue
                val parts = content["parts"]?.jsonArray ?: continue
                for (part in parts) {
                    val partObj = part.jsonObject
                    val isThought = partObj["thought"]?.jsonPrimitive?.content?.toBoolean() ?: false
                    val text = partObj["text"]?.jsonPrimitive?.content ?: ""
                    if (text.isNotEmpty() && text != "null") {
                        if (isThought) {
                            return StreamChunk.Thinking(text)
                        } else {
                            return StreamChunk.Content(text)
                        }
                    }
                }
            }
            StreamChunk.Content("")
        } catch (e: Exception) {
            DebugLog.log("Gemini", "Parse error: ${e.message}")
            StreamChunk.Content("")
        }
    }
}
