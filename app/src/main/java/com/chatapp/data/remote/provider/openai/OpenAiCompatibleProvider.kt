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
class MoonshotProvider @Inject constructor(
    sseClient: SseClient, securePrefs: SecurePrefs, json: Json, okHttpClient: OkHttpClient
) : OpenAiCompatibleProvider(sseClient, securePrefs, json, okHttpClient, ProviderType.MOONSHOT, "https://api.moonshot.cn")

@Singleton
class QwenProvider @Inject constructor(
    sseClient: SseClient, securePrefs: SecurePrefs, json: Json, okHttpClient: OkHttpClient
) : OpenAiCompatibleProvider(sseClient, securePrefs, json, okHttpClient, ProviderType.QWEN, "https://dashscope.aliyuncs.com/compatible-mode")

open class OpenAiCompatibleProvider(
    private val sseClient: SseClient,
    private val securePrefs: SecurePrefs,
    private val json: Json,
    private val okHttpClient: OkHttpClient,
    override val type: ProviderType,
    private val defaultBaseUrl: String
) : AiProvider {

    override val supportsThinking: Boolean = false

    override fun supportsFileType(mimeType: String): Boolean = when (type) {
        ProviderType.MOONSHOT -> mimeType.startsWith("image/")
        ProviderType.QWEN -> false
        else -> false
    }

    override suspend fun chat(request: ChatRequest): ChatResponse = error("Use stream()")

    override suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = securePrefs.getApiKey(type.name) ?: return@withContext getDefaultModels()
            val baseUrl = securePrefs.getProviderBaseUrl(type.name).ifEmpty { defaultBaseUrl }
            val client = okHttpClient.newBuilder().readTimeout(10, TimeUnit.SECONDS).build()
            val req = Request.Builder().url("$baseUrl/v1/models").header("Authorization", "Bearer $apiKey").get().build()
            val response = client.newCall(req).execute()
            val body = response.body?.string() ?: return@withContext getDefaultModels()
            if (!response.isSuccessful) return@withContext getDefaultModels()
            val obj = json.parseToJsonElement(body).jsonObject
            val data = obj["data"]?.jsonArray ?: return@withContext getDefaultModels()
            data.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.filter { it.isNotEmpty() }
        } catch (e: Exception) { getDefaultModels() }
    }

    private fun getDefaultModels(): List<String> = when (type) {
        ProviderType.MOONSHOT -> listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
        ProviderType.QWEN -> listOf("qwen-turbo", "qwen-plus", "qwen-max")
        else -> emptyList()
    }

    override fun stream(request: ChatRequest): Flow<StreamChunk> {
        val apiKey = try { securePrefs.getApiKey(type.name) ?: return flow { emit(StreamChunk.Error(IllegalStateException("${type.displayName} API Key not configured"))) } } catch (e: Exception) { return flow { emit(StreamChunk.Error(e)) } }
        val baseUrl = securePrefs.getProviderBaseUrl(type.name).ifEmpty { defaultBaseUrl }
        val body = buildRequestBody(request)
        return sseClient.connect(
            url = "$baseUrl/v1/chat/completions",
            headers = mapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json"),
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
        val model = securePrefs.getProviderModel(type.name).ifEmpty { getDefaultModels().firstOrNull() ?: "" }
        val obj = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature.toDouble().let { (it * 100).toInt() / 100.0 })
            request.topP?.let { put("top_p", it.toDouble().let { v -> (v * 100).toInt() / 100.0 }) }
            putJsonArray("messages") {
                request.systemPrompt?.let { sp ->
                    add(buildJsonObject { put("role", "system"); put("content", sp) })
                }
                request.messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        val imageAtts = msg.attachments.filter {
                            it.type == com.chatapp.domain.model.AttachmentType.IMAGE && it.dataBase64.isNotBlank()
                        }
                        if (imageAtts.isNotEmpty() && supportsFileType("image/")) {
                            putJsonArray("content") {
                                add(buildJsonObject { put("type", "text"); put("text", msg.content) })
                                imageAtts.forEach { att ->
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
            // Merge custom params (user-provided JSON overrides keys above)
            request.customParams?.let { raw ->
                try {
                    val customObj = json.parseToJsonElement(raw).jsonObject
                    customObj.forEach { (key, value) -> put(key, value) }
                } catch (_: Exception) { }
            }
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    private fun parseChunk(raw: String): StreamChunk {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val choices = obj["choices"]?.jsonArray ?: return StreamChunk.Content("")
            for (choice in choices) {
                val delta = choice.jsonObject["delta"]?.jsonObject ?: continue
                val content = delta["content"]?.jsonPrimitive?.content ?: ""
                if (content.isNotEmpty() && content != "null") return StreamChunk.Content(content)
            }
            StreamChunk.Content("")
        } catch (e: Exception) {
            DebugLog.log("OpenAiCompat", "Parse error: ${e.message}")
            StreamChunk.Content("")
        }
    }
}
