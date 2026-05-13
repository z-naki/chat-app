package com.chatapp.data.remote.sse

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.chatapp.util.DebugLog
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    fun connect(
        url: String,
        headers: Map<String, String>,
        body: String
    ): Flow<SseEvent> = callbackFlow {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val call = okHttpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                trySend(SseEvent.Error(e))
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                DebugLog.log("SSE", "Response received: ${response.code}")
                if (!response.isSuccessful) {
                    val code = response.code
                    val message = response.body?.string() ?: "HTTP $code"
                    DebugLog.log("SSE", "HTTP error $code: $message")
                    android.util.Log.e("ChatApp", "SSE HTTP $code: $message")
                    trySend(SseEvent.Error(HttpException(code, message)))
                    close()
                    return
                }

                val body = response.body ?: run {
                    trySend(SseEvent.Error(IOException("Empty response body")))
                    close()
                    return
                }

                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var currentData = StringBuilder()

                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val ln = line ?: break

                        when {
                            ln.startsWith("data: ") -> {
                                currentData.append(ln.removePrefix("data: "))
                            }
                            ln.isEmpty() && currentData.isNotEmpty() -> {
                                val data = currentData.toString().trim()
                                currentData = StringBuilder()
                                if (data == "[DONE]") {
                                    trySend(SseEvent.Done)
                                    close()
                                    return
                                }
                                android.util.Log.e("ChatApp", "S1_RAW: ${data.take(300)}")
                                trySend(SseEvent.Data(data))
                            }
                        }
                    }
                    // Process any remaining buffered data before closing
                    if (currentData.isNotEmpty()) {
                        val data = currentData.toString().trim()
                        DebugLog.log("NULL", "S1_PEND: ${data.take(300)}")
                        if (data == "[DONE]") {
                            trySend(SseEvent.Done)
                            close()
                            return
                        }
                        if (data.isNotEmpty()) {
                            trySend(SseEvent.Data(data))
                        }
                    }
                    trySend(SseEvent.Done)
                    close()
                } catch (e: IOException) {
                    trySend(SseEvent.Error(e))
                    close(e)
                } finally {
                    response.close()
                }
            }
        })

        awaitClose {
            call.cancel()
        }
    }
}

sealed class SseEvent {
    data class Data(val text: String) : SseEvent()
    data object Done : SseEvent()
    data class Error(val throwable: Throwable) : SseEvent()
}

class HttpException(val code: Int, override val message: String) : IOException(message)
