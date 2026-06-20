package com.chatapp.data.remote.provider

import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.data.remote.sse.SseClient
import com.chatapp.domain.model.ChatRequest
import com.chatapp.domain.model.ChatResponse
import com.chatapp.domain.model.ProviderType
import com.chatapp.domain.model.StreamChunk
import com.chatapp.util.DebugLog
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
class CustomProvider @Inject constructor(
    private val sseClient: SseClient,
    private val securePrefs: SecurePrefs,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) : AiProvider {

    override val type: ProviderType = ProviderType.CUSTOM
    override val supportsThinking: Boolean get() = false

    private val baseUrl: String get() = securePrefs.getProviderBaseUrl("CUSTOM")

    override suspend fun chat(request: ChatRequest): ChatResponse = error("Use stream()")

    override suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = securePrefs.getApiKey("CUSTOM") ?: return@withContext emptyList()
            val url = baseUrl.ifEmpty { return@withContext emptyList() }
            val client = okHttpClient.newBuilder().readTimeout(10, TimeUnit.SECONDS).build()
            val req = Request.Builder().url("$url/v1/models").header("Authorization", "Bearer $apiKey").get().build()
            val response = client.newCall(req).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            if (!response.isSuccessful) return@withContext emptyList()
            val obj = json.parseToJsonElement(body).jsonObject
            obj["data"]?.jsonArray?.mapNotNull {
                it.jsonObject["id"]?.jsonPrimitive?.content
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    override fun stream(request: ChatRequest): Flow<StreamChunk> {
        val apiKey = try {
            securePrefs.getApiKey("CUSTOM") ?: return flow { emit(StreamChunk.Error(IllegalStateException("Custom API Key not configured"))) }
        } catch (e: Exception) { return flow { emit(StreamChunk.Error(e)) } }
        val url = baseUrl.ifEmpty { return flow { emit(StreamChunk.Error(IllegalStateException("Custom Base URL not configured"))) } }
        val model = securePrefs.getProviderModel("CUSTOM").ifEmpty { "gpt-4o" }
        val body = buildRequestBody(request, model)
        return sseClient.connect(
            url = "$url/v1/chat/completions",
            headers = mapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json"),
            body = body
        ).map { event ->
            when (event) {
                is com.chatapp.data.remote.sse.SseEvent.Data -> parseChunk(event.text)
                is com.chatapp.data.remote.sse.SseEvent.Done -> StreamChunk.Done
                is com.chatapp.data.remote.sse.SseEvent.Error -> StreamChunk.Error(event.throwable)
            }
        }
    }

    private fun buildRequestBody(request: ChatRequest, model: String): String {
        val obj = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature.toDouble().let { (it * 100).toInt() / 100.0 })
            request.topP?.let { put("top_p", it.toDouble().let { v -> (v * 100).toInt() / 100.0 }) }
            putJsonArray("messages") {
                request.messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    private fun parseChunk(raw: String): StreamChunk {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            obj["choices"]?.jsonArray?.forEach { choice ->
                val delta = choice.jsonObject["delta"]?.jsonObject ?: return@forEach
                val content = delta["content"]?.jsonPrimitive?.content ?: ""
                if (content.isNotEmpty() && content != "null") return StreamChunk.Content(content)
            }
            StreamChunk.Content("")
        } catch (e: Exception) { StreamChunk.Content("") }
    }
}
