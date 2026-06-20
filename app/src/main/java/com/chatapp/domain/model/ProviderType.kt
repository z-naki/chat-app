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
    CUSTOM_1("Custom 1", false, supportsMultimodal = false),
    CUSTOM_2("Custom 2", false, supportsMultimodal = false),
    CUSTOM_3("Custom 3", false, supportsMultimodal = false);

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
