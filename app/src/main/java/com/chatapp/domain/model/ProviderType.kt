package com.chatapp.domain.model

enum class ProviderType(val displayName: String, val requiresProxy: Boolean) {
    DEEPSEEK("DeepSeek", false),
    OPENAI("OpenAI", true),
    ANTHROPIC("Anthropic", true),
    GEMINI("Gemini", true)
}
