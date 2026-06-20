# CLAUDE.md — Chat App Project

## Project Overview

Android AI chat assistant. Aggregates 8 AI vendor APIs through a unified interface. Pure client, no backend.

**Stack:** Kotlin + Jetpack Compose + Material3 + Clean Architecture MVVM + Hilt DI + Room v5 + OkHttp SSE

**Directory:** `C:\Users\45857\chat-app`
**Version:** `v0.0.21-alpha` (versionCode=21)

---

## User Terminology (what the user means)

| User says | Refers to |
|---|---|
| "右上角" / "右上角设置" | HomeScreen TopAppBar 3-dot menu (MoreVert icon dropdown) |
| "厂商" / "模型厂商" | Provider (DeepSeek, OpenAI, Gemini, etc.) |
| "模型" | Specific model name under a provider (deepseek-v4-pro, gpt-4o) |
| "底部输入框" | InputBar in ChatScreen bottomBar |
| "信息栏" | Provider-model display row above InputBar |
| "侧栏" / "历史对话" | ModalNavigationDrawer conversation list |
| "气泡" | Surface-wrapped message (user explicitly does NOT want bubbles) |
| "思考" / "thinking" / "思考过程" | AI reasoning content (collapsible "Thought" section) |
| "自定义厂商" | ProviderType.CUSTOM_1/2/3 |
| "核采样" | Top-p |

## Architecture

```
UI (Compose screens) → ViewModel (StateFlow) → UseCase → Repository → DAO/Provider
                                                              ↓
Domain models (ProviderType, ChatRequest, Conversation, StreamChunk, etc.)
```

### Key Files by Layer

| Layer | Key Files |
|---|---|
| **Screens** | `ui/home/HomeScreen.kt`, `ui/chat/ChatScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/provideredit/ProviderEditScreen.kt` |
| **Components** | `ui/components/MessageBubble.kt` (ChatMessage), `InputBar.kt`, `MarkdownContent.kt`, `StreamingText.kt` |
| **Theme/Anim** | `ui/theme/Color.kt`, `AppStrings.kt`, `ui/animation/AnimationSpecs.kt` |
| **ViewModels** | `ui/chat/ChatViewModel.kt`, `ui/settings/SettingsViewModel.kt`, `ui/provideredit/ProviderEditViewModel.kt` |
| **Domain** | `domain/model/`, `domain/repository/`, `domain/usecase/`, `domain/analyze/` |
| **Data** | `data/repository/ChatRepositoryImpl.kt`, `data/local/prefs/SecurePrefs.kt`, `data/local/db/` |
| **Providers** | `data/remote/provider/` — 8 providers + `SseClient.kt` |
| **DI** | `di/AppModule.kt` |
| **Nav** | `ui/navigation/NavGraph.kt` |

## 8 Providers

| Provider | Class | API Format | Thinking | top_p | Notes |
|---|---|---|---|---|---|
| DeepSeek | `deepseek/DeepSeekProvider.kt` | OpenAI SSE | non-chat models | ❌ when thinking | web search, reasoner |
| OpenAI | `openai/OpenAiProvider.kt` | OpenAI SSE | o-series | ✅ | max_completion_tokens for reasoning |
| Anthropic | `anthropic/AnthropicProvider.kt` | Messages SSE | all | ✅ | temp capped 0-1, system as top-level |
| Gemini | `gemini/GeminiProvider.kt` | generateContent | 2.5 series | ✅ (topP) | systemInstruction |
| Kimi | `openai/OpenAiCompatibleProvider.kt` | OpenAI compat | ❌ | ✅ | base: moonshot.cn |
| Qwen | `openai/OpenAiCompatibleProvider.kt` | OpenAI compat | ❌ | ✅ | base: dashscope |
| Custom 1-3 | `CustomProvider.kt` | OpenAI compat | ❌ | ✅ | user-configurable |

**ProviderType enum** (10 values): DEEPSEEK, OPENAI, ANTHROPIC, GEMINI, MOONSHOT, QWEN, CUSTOM_1, CUSTOM_2, CUSTOM_3

**All when-expressions must be exhaustive** for all 10 values.

## Design Language

- **No bubbles, no emoji, no spring/bounce animations**
- **Light mode:** background #F5F5F5, surface #FAFAFA, surfaceVariant #EEEEEE, TopAppBar outline 0.5f alpha
- **Dark mode:** background #121212, surface #1E1E1E, surfaceVariant #2C2C2C
- **Animations:** all `tween()` — expand 280ms, shrink 240ms, fadeIn 220ms, fadeOut 180ms (`AnimationSpecs.kt`)
- **User messages:** right-aligned (`Row(Arrangement.End)` + `textAlign = TextAlign.End`)
- **Icons:** 32dp size, 18dp inner icon
- **Localization:** `AppStrings` data class (EN/ZH), provided via `CompositionLocalProvider(LocalStrings)`. Always use `s.xxx` not hardcoded strings.

## Development Workflow (MUST follow)

1. **All changes first** — all features, all files, no gradle between edits
2. **Agent debug** — ≤12 files/agent, embed systematic-debugging Phase 1-4, report ALL bugs including LOW
3. **Fix all bugs** — high to low, every single one
4. **Compile once** — `./gradlew compileDebugKotlin` at the very end

**PROHIBITED:** Compiling after individual edits. Write everything → debug → fix → compile once.

## Version Bump Rule

Every commit must update: `build.gradle.kts` (versionCode + versionName), `HomeScreen.kt` title text, `SettingsScreen.kt` version text.

## Common Gotchas (from fix-and-learn)

1. Kotlin `TextStyle.copy(textAlign)` doesn't accept `TextAlign?` — check null first
2. Lambda scope: `.filter { id -> ... }.sortedBy { id }` — `id` from filter NOT in sortedBy scope
3. Composable inside `remember {}` — `remember` block is NOT composable. Read `MaterialTheme`/`Local*` OUTSIDE
4. `DropdownMenu` cannot contain `LazyColumn` — use `Column + verticalScroll` instead
5. `hiltViewModel()` in same NavBackStackEntry returns the same instance — don't create duplicate VMs in nested composables
6. Conversation params (temperature/contextRounds/maxTokens/topP) must sync in BOTH `init` AND `loadConversation`
7. `stopGeneration()` must remove orphaned STREAMING message from uiState.messages
8. System prompt + custom params must be wired through ChatRepositoryImpl → ChatRequest → provider buildRequestBody
9. Gemini `topP` is camelCase; others are `top_p` snake_case
10. Anthropic `system` is top-level field, not a message in the array

## Compile

```bash
export JAVA_HOME="D:/android-studio/android_studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
cd C:/Users/45857/chat-app
./gradlew compileDebugKotlin
```

## Project Skills

| Skill | When to use |
|---|---|
| `verify-before-edit` | Adding/changing dependencies, versions, new functionality |
| `fix-and-learn` | After fixing ANY error — analyze root cause, append prevention rule |
| `verify-fix` | After every fix — scan for same anti-pattern, check regressions |
