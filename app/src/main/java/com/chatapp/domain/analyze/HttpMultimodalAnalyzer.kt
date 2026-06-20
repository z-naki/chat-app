package com.chatapp.domain.analyze

import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.domain.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpMultimodalAnalyzer @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : MultimodalAnalyzer {

    override suspend fun analyze(file: Attachment): Result<FileContent> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = securePrefs.getMultimodalApiUrl().ifEmpty {
                return@withContext Result.failure(IllegalStateException("Multimodal API URL not configured"))
            }
            val apiKey = securePrefs.getMultimodalApiKey() ?: ""
            val body = buildJsonObject {
                put("file_data", file.dataBase64)
                put("mime_type", file.mimeType)
                put("file_name", file.name)
            }.toString()

            val client = okHttpClient.newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("API error: ${response.code}"))
            }

            val obj = json.parseToJsonElement(responseBody).jsonObject
            val text = obj["text"]?.jsonPrimitive?.content ?: responseBody
            val pageCount = obj["page_count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
            val sourceType = obj["source_type"]?.jsonPrimitive?.content ?: file.mimeType
            Result.success(FileContent(text = text, pageCount = pageCount, sourceType = sourceType))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
