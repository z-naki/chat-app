package com.chatapp.domain.model

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM;

    companion object {
        fun fromStringOrDefault(value: String): MessageRole {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                USER
            }
        }
    }
}
