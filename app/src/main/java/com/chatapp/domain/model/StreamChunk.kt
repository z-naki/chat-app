package com.chatapp.domain.model

sealed class StreamChunk {
    data class Content(val text: String) : StreamChunk()
    data class Thinking(val text: String) : StreamChunk()
    data class SearchStatus(val query: String) : StreamChunk()
    data object Done : StreamChunk()
    data class Error(val throwable: Throwable) : StreamChunk()
}
