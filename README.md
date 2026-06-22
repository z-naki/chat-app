# Chat AI

<p align="center">
  <b>简洁 · 现代 · 轻量</b><br>
  一个 App 聚合多个 AI 厂商，自带 Key 即用
</p>

---

**English** · [中文](#中文)

A clean, minimalist Android AI chat client. **Personal practice project** — built to learn Kotlin + Compose, not a commercial product.

### Design

- No bubbles, no emoji, no spring/bounce animations
- Off-white (#F5F5F5) / off-black (#121212) color hierarchy
- All animations use `tween()` — expand 280ms, shrink 240ms, fade 220ms
- Full-width messages, user text right-aligned
- 32dp icons with 18dp inner icon
- System font only — zero external UI dependencies

### Why this app?

| Your situation | Solution |
|---|---|
| You have API keys for DeepSeek, OpenAI, Gemini... | Manage them all in one app, switch providers anytime |
| You want to compare models side by side | Create conversations with different providers, switch with one tap |
| You need multimodal (PDF, image analysis) but your provider doesn't support it | Configure a third-party multimodal API to handle unsupported file types |
| You live in a restricted network | Built-in HTTP proxy support |
| You worry about API key safety | Android Keystore AES-256 encryption + biometric lock |

### Quick Start

```
1. Download APK from Releases
2. Open Settings → tap a provider → paste your API key
3. Tap + to start a conversation
4. Chat
```

> No API key? Sign up at [DeepSeek Platform](https://platform.deepseek.com) (free trial credits) or any supported provider.

### Supported Providers

| Provider | Model List | Thinking | Search | Multimodal |
|---|---|---|---|---|
| DeepSeek | Auto-fetch | ✅ | ✅ | Image |
| OpenAI | Auto-fetch | ✅ | — | Image |
| Anthropic | Manual input | ✅ | — | Image + PDF |
| Gemini | Auto-fetch | ✅ | — | Image + Audio + Video |
| Moonshot/Kimi | Auto-fetch | — | — | Image |
| Qwen (Tongyi) | Auto-fetch | — | — | — |
| Custom (×3) | Manual input | — | — | — |

### Key Features

- **Dynamic Provider Switching** — Switch AI providers mid-conversation or per-conversation
- **Third-Party Multimodal** — Configure an external API to analyze files your provider doesn't support (PDF, Word, etc.)
- **Streaming SSE** — Real-time token-by-token display
- **Thinking/Reasoning** — Collapsible reasoning view with token count
- **Full Markdown** — Code blocks, tables, task lists, blockquotes, inline formatting, images, links
- **Per-Provider Settings** — API key, base URL, system prompt, custom JSON parameters
- **Conversation Management** — Multi-conversation, delete, regenerate messages
- **Dark Mode** — Auto / Light / Dark
- **Chinese/English** — Full localization
- **Proxy** — HTTP proxy for restricted networks

### Build from Source

```bash
# Prerequisites: Android Studio (JDK 17 included)
git clone https://github.com/your-username/chat-app.git
cd chat-app
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

### Tech Stack

Kotlin + Jetpack Compose + Material 3 · Clean Architecture MVVM · Hilt DI · Room DB · OkHttp 4 SSE · Kotlinx Serialization

### License

[MIT](LICENSE) — Free to use, modify, and distribute.

### Disclaimer

This is a personal learning project. All API keys are stored locally with Android Keystore AES-256 encryption and never transmitted anywhere except to their respective provider APIs.

---

## 中文

一个简洁现代的 Android AI 聊天客户端。**个人练手作品**，用来学习 Kotlin + Compose，非商业产品。

### 设计理念

- 无气泡、无 emoji、无弹跳动画
- 灰白 (#F5F5F5) / 灰黑 (#121212) 层级配色
- 全部使用 `tween()` 线性动画 — 展开 280ms、收缩 240ms、淡入 220ms
- 全宽消息，用户文字右对齐
- 图标 32dp，内部图标 18dp
- 仅使用系统字体，UI 层零外部依赖

### 为什么用这个？

| 你的场景 | 解决方式 |
|---|---|
| 手上有 DeepSeek、OpenAI、Gemini 等多个 API Key | 一个 App 统一管理，随时切换 |
| 想对比不同模型的回答质量 | 创建多个对话，一键切换厂商 |
| 需要分析 PDF/图片，但当前厂商不支持多模态 | 配置第三方多模态 API，自动解析后发给对话厂商 |
| 身处网络受限环境 | 内置 HTTP 代理 |
| 担心 API Key 泄露 | Android 系统级 AES-256 加密 + 生物识别解锁 |

### 快速开始

```
1. 从 Releases 下载 APK 安装
2. 打开设置 → 点击厂商 → 粘贴 API Key
3. 点右上角 + 新建对话
4. 开始聊天
```

> 没有 API Key？去 [DeepSeek 开放平台](https://platform.deepseek.com) 注册（有免费额度），或其他支持的厂商。

### 支持的厂商

| 厂商 | 模型列表 | 思考过程 | 联网搜索 | 多模态 |
|---|---|---|---|---|
| DeepSeek | 自动拉取 | ✅ | ✅ | 图片 |
| OpenAI | 自动拉取 | ✅ | — | 图片 |
| Anthropic | 手动填写 | ✅ | — | 图片 + PDF |
| Gemini | 自动拉取 | ✅ | — | 图片 + 音频 + 视频 |
| Moonshot/Kimi | 自动拉取 | — | — | 图片 |
| Qwen (通义) | 自动拉取 | — | — | — |
| 自定义 (×3) | 手动填写 | — | — | — |

### 核心功能

- **动态切换厂商** — 对话中随时切换 AI 厂商，每个对话独立配置
- **第三方多模态** — 配置外部 API 解析当前厂商不支持的文件（PDF、Word 等），自动注入解析结果
- **流式响应** — 实时逐字显示
- **思考过程** — 可折叠的推理过程展示，带 Token 计数
- **完整 Markdown** — 代码块、表格、任务列表、引用块、行内格式、图片、链接
- **厂商独立设置** — API Key、接口地址、系统提示词、自定义 JSON 参数
- **对话管理** — 多对话、删除、重新生成
- **深色模式** — 自动 / 浅色 / 深色
- **中英双语** — 完整本地化
- **代理** — HTTP 代理支持

### 从源码构建

```bash
# 需要 Android Studio（自带 JDK 17）
git clone https://github.com/your-username/chat-app.git
cd chat-app
./gradlew assembleDebug
# APK 位置: app/build/outputs/apk/debug/app-debug.apk
```

### 技术栈

Kotlin + Jetpack Compose + Material 3 · Clean Architecture MVVM · Hilt DI · Room DB · OkHttp 4 SSE · Kotlinx Serialization

### 开源协议

[MIT](LICENSE) · 自由使用、修改、分发。

### 声明

个人练手项目。所有 API Key 使用 Android Keystore AES-256 加密存储于本地，不会传输到除对应厂商 API 之外的任何地方。
