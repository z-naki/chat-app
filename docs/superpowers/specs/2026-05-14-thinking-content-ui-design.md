# Thinking vs Content UI Separation — Design Spec

**Goal:** Visually distinguish DeepSeek reasoning_content (thinking) from formal output (content) within the same AI chat bubble.

**Date:** 2026-05-14

---

## Requirements

1. Thinking and formal output share ONE bubble (not separate bubbles)
2. A thin solid divider separates the thinking section from the content section
3. A title bar above the thinking divider shows: "Thinking" label + token count + expand/collapse toggle
4. A second thin solid divider separates the thinking section from the formal output
5. Thinking text uses smaller, muted typography; formal output keeps current style
6. Default state: EXPANDED (user manually collapses)
7. No emoji anywhere
8. Divisor line: thin solid line (0.5dp)

---

## Bubble Layout

### Expanded State

```
┌─────────────────────────────────────────┐
│  Thinking  2.3k tokens             [▲]  │  ← title bar (visible only when thinking exists)
│  ────────────────────────────────────── │  ← HorizontalDivider, 0.5dp
│  Let me think about this step by step... │  ← thinking text (bodySmall, onSurfaceVariant)
│  ────────────────────────────────────── │  ← HorizontalDivider, 0.5dp
│  The answer is 42...                 |  │  ← formal output (bodyLarge, onSurface, blinking cursor)
└─────────────────────────────────────────┘
```

### Collapsed State

```
┌─────────────────────────────────────────┐
│  Thinking  2.3k tokens             [▶]  │
│  ────────────────────────────────────── │
│  The answer is 42...                 |  │
└─────────────────────────────────────────┘
```

### No Thinking (Regular Message)

```
┌─────────────────────────────────────────┐
│  The answer is 42...                    │
└─────────────────────────────────────────┘
```

---

## Data Model Changes

### ChatUiState

Add:
```kotlin
val streamingThinking: String = "",      // accumulated reasoning_content
val isThinkingCollapsed: Boolean = false, // user toggle state
val thinkingTokenCount: Long = 0,         // token estimate
```

Rename for clarity:
```kotlin
streamingContent → streamingOutput  // formal output only (no longer mixed)
```

### ChatViewModel.sendMessage()

- `StreamChunk.Thinking` → accumulate into `streamingThinking`, NOT `streamingOutput`
- `StreamChunk.Content` → accumulate into `streamingOutput`
- Both update `_uiState` atomically

---

## Component Changes

### Files to Modify

| File | Change |
|------|--------|
| `ui/chat/ChatUiState.kt` | Add `streamingThinking`, `isThinkingCollapsed`, `thinkingTokenCount`; rename `streamingContent` |
| `ui/chat/ChatViewModel.kt` | Split thinking/content accumulation; add `toggleThinkingCollapse()` |
| `ui/chat/ChatScreen.kt` | Update field references |
| `ui/components/StreamingText.kt` | Restructure into sections with dividers, toggle button, dual text styles |

### Files to Create

None — all changes are modifications to existing files.

---

## Theme

- Thinking title bar: `bodySmall`, `onSurfaceVariant`
- Thinking text: `bodySmall`, `onSurfaceVariant` (muted)
- Formal output: `bodyLarge`, `onSurface` (current style, unchanged)
- Dividers: `outline` color, 0.5dp thickness
- Toggle icon: `onSurfaceVariant`, 18dp size

Light and dark themes use the same token colors — they resolve through Material3 color scheme automatically.

---

## Token Count Calculation

Simple character-based estimate (matching DeepSeek's approximate tokenization):
```kotlin
thinkingTokenCount = (streamingThinking.length / 2.5).toLong()
```

Display format:
- < 1k: "523 tokens"
- >= 1k: "2.3k tokens"

---

## Edge Cases

1. **No thinking at all** — No title bar, no divider, just regular message bubble (current behavior)
2. **Thinking present, content empty** — Show thinking section with blinking cursor in content area
3. **User collapses during streaming** — Thinking text hidden but continues accumulating in state
4. **Message complete** — Thinking stays in collapsed/expanded state as user left it
