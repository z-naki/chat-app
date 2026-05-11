# AI Chat App — Android Design Spec

**Date:** 2026-05-11
**Status:** Approved

---

## 1. Product Definition

- **Type:** AI aggregator chat assistant for Android
- **Core:** Unified client calling multiple AI vendor APIs (OpenAI-compatible + proprietary)
- **Initial vendor:** DeepSeek (deepseek-v4-pro), with multi-provider extension architecture
- **Deployment:** Pure client-side, no backend; API keys encrypted locally
- **Multi-modal:** Deferred to future iterations

## 2. Feature Scope (V1)

| Feature | Status | Notes |
|---|---|---|
| Multi-conversation management | V1 | Create, switch, delete conversations |
| Local history persistence | V1 | Room DB, messages and conversations |
| Streaming output (SSE) | V1 | Typewriter-style rendering |
| Web search | V1 | Via DeepSeek tool-use API parameter |
| Multi-provider support | V1 (arch) | Provider abstraction; only DeepSeek wired in V1 |
| API key encryption | V1 | Android Keystore + EncryptedSharedPreferences + AES-GCM envelope |
| Proxy support | V1 | Manual proxy address + toggle; reminder on adding blocked-vendor keys |
| Light / Dark theme | V1 | Material3 color scheme, off-white / off-black palette |
| Model parameters | V1 | Temperature, max tokens, context rounds — user configurable |

## 3. Architecture — Clean Architecture MVVM

### 3.1 Layer Map

```
ui/                  Compose Screens + ViewModels
       → depends on ←
domain/              Pure Kotlin: models, use-cases, repository interfaces
       → depends on ←
data/                Implementations: Room, OkHttp, SecurePrefs, Provider impls
```

- **domain/** has zero framework dependencies
- **data/** depends on domain interfaces
- **ui/** depends on domain; ViewModels expose `StateFlow<UiState>`

### 3.2 Package Structure

```
com.chatapp/
├── ChatApp.kt
├── di/
│   └── AppModule.kt
├── data/
│   ├── local/
│   │   ├── db/          AppDatabase, ConversationDao, MessageDao, entities
│   │   ├── prefs/       SecurePrefs (EncryptedSharedPreferences wrapper)
│   │   └── security/    CryptoManager (Keystore + AES-GCM envelope)
│   ├── remote/
│   │   ├── sse/         SseClient (OkHttp-based SSE parser)
│   │   └── provider/    AiProvider interface + deepseek/ implementation
│   └── repository/      ChatRepositoryImpl, SettingsRepositoryImpl
├── domain/
│   ├── model/           Conversation, Message, StreamChunk, ProviderType, ...
│   ├── repository/      ChatRepository, SettingsRepository (interfaces)
│   └── usecase/         CreateConversation, SendMessage, StreamMessage, ...
└── ui/
    ├── navigation/      NavGraph
    ├── theme/           Theme, Color, Type
    ├── components/      MessageBubble, StreamingText, InputBar, ConversationItem
    ├── conversationlist/ ConversationListScreen + VM
    ├── chat/            ChatScreen + ChatVM + ChatUiState
    └── settings/        SettingsScreen + SettingsVM
```

## 4. Core Data Flow — Streaming Chat

```
User types "什么是熵" → InputBar → ChatVM.sendMessage()
  1. Insert UserMessage into Room DB
  2. Update ChatUiState (append user bubble)
  3. Launch coroutine:
       StreamMessageUseCase(convId, messages)
         → ChatRepository.streamReply(provider, messages)
           → ProviderRouter.getActive() → DeepSeekProvider.stream()
             → SseClient.connect(POST /v1/chat/completions, stream=true)
               → Flow<StreamChunk> (parsed SSE events)
         → .collect { chunk → uiState.append(chunk) }  // incremental render
  4. On Flow completion: write full AssistantMessage to Room DB
```

### 4.1 Stream Cancellation

- `viewModelScope` cancellation → OkHttp `Call.cancel()` → TCP RST
- "Stop generation" button sets a ViewModel flag, cancels the scope

### 4.2 Context Window

- Keep last N conversation rounds (default 20, configurable)
- deepseek-v4-pro: 1M context, max output 384K tokens
- Truncation strategy: oldest messages first when exceeding configurable limit

## 5. Multi-Provider Abstraction

### 5.1 Interface (domain)

```kotlin
interface AiProvider {
    val type: ProviderType
    suspend fun chat(request: ChatRequest): ChatResponse
    fun stream(request: ChatRequest): Flow<StreamChunk>
}

data class ChatRequest(
    val model: String,
    val messages: List<ProviderMessage>,
    val temperature: Float,
    val maxTokens: Int,
    val enableSearch: Boolean,
    val extraParams: Map<String, Any> = emptyMap()
)

sealed class StreamChunk {
    data class Content(val text: String) : StreamChunk()
    data class Thinking(val text: String) : StreamChunk()
    data class SearchStatus(val query: String) : StreamChunk()
    object Done : StreamChunk()
    data class Error(val throwable: Throwable) : StreamChunk()
}
```

### 5.2 Provider Registration

```kotlin
object ProviderRouter {
    private val providers = mutableMapOf<ProviderType, AiProvider>()
    fun register(provider: AiProvider)
    fun resolve(type: ProviderType): AiProvider
    fun getActive(): AiProvider  // reads active setting from SecurePrefs
}
```

Adding a new vendor = 3 steps:
1. New `XxxProvider` implements `AiProvider`
2. Register in `AppModule`
3. Add entry to `ProviderType` enum

## 6. Storage & Encryption

### 6.1 Room Database

- **ConversationTable**: id, title, provider, createdAt, updatedAt
- **MessageTable**: id, conversationId (FK), role, content, thinking, timestamp, status

### 6.2 API Key Encryption

```
Store:  plainKey → AES-256-GCM encrypt → cipherText
        AES key → RSA-2048 (Android Keystore) encrypt → encryptedKey
        Save: encryptedKey || cipherText → EncryptedSharedPreferences

Retrieve: read from EncryptedSharedPreferences
          RSA private key (Keystore) decrypts AES key
          AES-GCM decrypts cipherText → plainKey (in memory only)
```

- **Android Keystore** (TEE/SE): RSA keypair never leaves hardware
- **EncryptedSharedPreferences**: AES-256-GCM file-level encryption
- **Envelope encryption**: Extra AES layer so raw key is never in prefs

### 6.3 Non-Sensitive Settings

- Proxy address, active provider, model parameters → DataStore (plain text)
- User can view decrypted API key after biometric/password challenge (V2 enhancement)

## 7. Network Layer

### 7.1 OkHttp Configuration

- Singleton `OkHttpClient` managed by Hilt
- `readTimeout(0)` for unbounded streaming
- Proxy support: user-configurable HTTP proxy address + toggle
- When proxy enabled and address set → `builder.proxy(Proxy(...))`

### 7.2 Proxy Reminder

- Upon adding API key for blocked vendors (OpenAI, Gemini, Anthropic):
  - Check `proxy_enabled == false`
  - If so → show dialog: "This provider may not be accessible from mainland China without a proxy. [Configure Proxy] [Ignore]"

### 7.3 Web Search

- DeepSeek: sends `tools=[{"type": "web_search"}]` in request body
- UI renders `StreamChunk.SearchStatus` as interim indicator, then content

## 8. Navigation & Screens

### 8.1 Routes

```
"conversation_list"        → ConversationListScreen (start destination)
"chat/{conversationId}"    → ChatScreen
"settings"                 → SettingsScreen
```

### 8.2 Screens

**ConversationListScreen**
- Top bar: title + settings gear + search icon
- LazyColumn: conversation cards (title, last message preview, timestamp)
- Swipe-to-delete
- FAB: new conversation
- Empty state: prompt to start a conversation

**ChatScreen**
- Top bar: back arrow + conversation title + provider badge + overflow menu
- LazyColumn: message bubbles (user right-aligned, AI left-aligned)
- Streaming text: animated cursor, incremental rendering
- Bottom bar: web-search toggle, temperature quick-adjust, input field, send button, stop-generation button (during streaming)
- AI message: copy button on each bubble

**SettingsScreen**
- Appearance: theme selector (Light / Dark / System)
- API Key Management: per-provider key list, add/edit, masked display with reveal toggle
- Network: proxy toggle + address input
- Default Parameters: temperature slider, max tokens input, context rounds slider
- About: version, license

## 9. Theme — Color System

### 9.1 Light Theme
| Token | Hex | Usage |
|---|---|---|
| background | `#F5F5F5` | Main background |
| surface | `#FAFAFA` | Cards, bubbles |
| surfaceVariant | `#EEEEEE` | Input field |
| outline | `#E0E0E0` | Dividers |
| onBackground | `#1A1A1A` | Primary text |
| onSurface | `#616161` | Secondary text |
| onSurfaceVariant | `#9E9E9E` | Caption text |

### 9.2 Dark Theme
| Token | Hex | Usage |
|---|---|---|
| background | `#121212` | Main background (not pure black) |
| surface | `#1E1E1E` | Cards, bubbles |
| surfaceVariant | `#2C2C2C` | Input field |
| outline | `#383838` | Dividers |
| onBackground | `#E8E8E8` | Primary text |
| onSurface | `#A0A0A0` | Secondary text |
| onSurfaceVariant | `#6E6E6E` | Caption text |

**Principle:** Off-white / off-black, never pure #000 or #FFF. Natural depth hierarchy via subtle gray shifts.

## 10. Design Language Rules

- No emoji in copy or labels
- Professional terminology: "API Key", "Token", "Endpoint", "Provider", "Conversation"
- Minimalist, clean layout
- Follow Material3 conventions

## 11. Error Handling

```
sealed class UiError {
    data class Network(val code: Int, val message: String)
    data class Auth(val provider: ProviderType)
    data class RateLimit(val retryAfter: Duration)
    data class Quota(val detail: String)
    data class Server(val code: Int)
    data class Unknown(val throwable: Throwable)
}
```

- Network error → Snackbar with retry
- Auth error → Inline "API Key invalid, [Go to Settings]" link
- Rate limit → Countdown + retry prompt
- Server error → Snackbar "Service unavailable, retry later"

## 12. Testing Strategy

| Layer | Framework | Focus |
|---|---|---|
| Unit (domain, data) | JUnit5 + MockK | UseCase logic, Provider request building, SSE parsing, CryptoManager round-trip |
| Repository | JUnit5 + MockK | Data assembly, local/remote coordination |
| ViewModel | JUnit5 + Turbine | State transitions, streaming chunk assembly, error mapping |
| UI | Compose Testing | Streaming render, theme switching |
| E2E | Manual | Core flow walkthrough, multi-provider switching |

## 13. Tech Stack Summary

| Category | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Navigation | Compose Navigation |
| Database | Room |
| Encryption | Android Keystore + EncryptedSharedPreferences + AES-256-GCM |
| Network | OkHttp (SSE + REST) |
| Serialization | Kotlinx Serialization |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Architecture | MVVM + Clean Architecture |
