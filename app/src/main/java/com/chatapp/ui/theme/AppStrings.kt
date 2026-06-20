package com.chatapp.ui.theme

import androidx.compose.runtime.compositionLocalOf

data class AppStrings(
    val settings: String,
    val appearance: String,
    val theme: String,
    val language: String,
    val light: String,
    val dark: String,
    val system: String,
    val apiKeyToken: String,
    val addProvider: String,
    val network: String,
    val proxy: String,
    val about: String,
    val version: String,
    val search: String,
    val newConversation: String,
    val history: String,
    val model: String,
    val temperature: String,
    val contextRounds: String,
    val maxTokens: String,
    val multimodal: String,
    val you: String,
    val message: String,
    val thought: String,
    val edit: String,
    val delete: String,
    val copy: String,
    val regenerate: String,
    val cancel: String,
    val send: String,
    val english: String,
    val chinese: String,
    val thirdPartyMultimodal: String,
    val provider: String,
    val apiEndpoint: String,
    val apiKey: String,
    val providerName: String,
    val noConversations: String,
    val tokens: String,
    val topP: String
)

val EN = AppStrings(
    settings = "Settings", appearance = "Appearance", theme = "Theme", language = "Language",
    light = "Light", dark = "Dark", system = "System", apiKeyToken = "API Key / Token",
    addProvider = "Add Provider", network = "Network", proxy = "Proxy", about = "About",
    version = "Version", search = "Search", newConversation = "New conversation",
    history = "History", model = "Model", temperature = "Temperature",
    contextRounds = "Context Rounds", maxTokens = "Max Tokens", multimodal = "Multimodal",
    you = "You", message = "Message", thought = "Thought", edit = "Edit", delete = "Delete",
    copy = "Copy", regenerate = "Regenerate", cancel = "Cancel", send = "Send",
    english = "English", chinese = "中文",
    thirdPartyMultimodal = "Third-party Multimodal", provider = "Provider",
    apiEndpoint = "API Endpoint URL", apiKey = "API Key", providerName = "Provider Name",
    noConversations = "No conversations yet", tokens = "tokens", topP = "Top-p"
)

val ZH = AppStrings(
    settings = "设置", appearance = "外观", theme = "主题", language = "语言",
    light = "浅色", dark = "深色", system = "跟随系统", apiKeyToken = "API 密钥",
    addProvider = "添加厂商", network = "网络", proxy = "代理", about = "关于",
    version = "版本", search = "搜索", newConversation = "新建对话",
    history = "历史记录", model = "模型", temperature = "温度",
    contextRounds = "记忆轮数", maxTokens = "回复上限", multimodal = "多模态",
    you = "你", message = "输入文本", thought = "思考过程", edit = "编辑", delete = "删除",
    copy = "复制", regenerate = "重新生成", cancel = "取消", send = "发送",
    english = "English", chinese = "中文",
    thirdPartyMultimodal = "第三方多模态", provider = "厂商",
    apiEndpoint = "API 地址", apiKey = "API 密钥", providerName = "厂商名称",
    noConversations = "暂无对话", tokens = "Token", topP = "核采样"
)

val LocalStrings = compositionLocalOf { EN }
