package com.chatapp.data.remote.sse

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue

class SseClientTest {
    // SSE event parsing test:
    // Given raw SSE stream:
    //   data: {"choices":[{"delta":{"content":"hello"}}]}
    //
    //   data: {"choices":[{"delta":{"content":" world"}}]}
    //
    //   data: [DONE]
    //
    // Expect: SseEvent.Data("{\"choices\"..."), SseEvent.Data("{\"choices\"..."), SseEvent.Done

    // Error handling test:
    // Given HTTP 401 response -> expect SseEvent.Error(HttpException(401, ...))

    // Cancellation test:
    // Given active stream, when flow collector cancelled -> call.enqueue Callback released
}
