package com.chatapp.domain.model

enum class ProviderType(
    val displayName: String,
    val requiresProxy: Boolean,
    val supportsMultimodal: Boolean = false
) {
    DEEPSEEK("DeepSeek", false, supportsMultimodal = true),
    OPENAI("OpenAI", true, supportsMultimodal = true),
    ANTHROPIC("Anthropic", true, supportsMultimodal = true),
    GEMINI("Gemini", true, supportsMultimodal = true),
    MOONSHOT("Moonshot/Kimi", false, supportsMultimodal = true),
    QWEN("Qwen", false, supportsMultimodal = false),
    CUSTOM("Custom", false, supportsMultimodal = false);

    companion object {
        fun fromStringOrDefault(value: String): ProviderType {
            return try {
                ProviderType.valueOf(value)
            } catch (e: IllegalArgumentException) {
                DEEPSEEK
            }
        }
    }
}
