# Thinking vs Content UI Separation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Visually distinguish DeepSeek reasoning_content (thinking) from formal output within the same AI chat bubble using dividers and a collapsible thinking section.

**Architecture:** Split `streamingContent` into `streamingThinking` + `streamingOutput` in ChatUiState, accumulate separately in ChatViewModel, render with dividers and toggle in StreamingBubble. All in existing files, no new files.

**Tech Stack:** Kotlin, Jetpack Compose, Material3

---

### Task 1: ChatUiState — split thinking from content

**Files:**
- Modify: `app/src/main/java/com/chatapp/ui/chat/ChatUiState.kt`

**Purpose:** Add `streamingThinking`, `isThinkingCollapsed`, `thinkingTokenCount` fields. Rename `streamingContent` → `streamingOutput` for clarity.

- [ ] **Step 1: Update ChatUiState data class**

Replace the entire file content:

```kotlin
package com.chatapp.ui.chat

import com.chatapp.domain.model.Conversation
import com.chatapp.domain.model.Message

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val streamingOutput: String = "",
    val streamingThinking: String = "",
    val isThinkingCollapsed: Boolean = false,
    val thinkingTokenCount: Long = 0,
    val enableSearch: Boolean = false,
    val errorMessage: String? = null
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/chatapp/ui/chat/ChatUiState.kt
git commit -m "feat: split streamingContent into streamingThinking + streamingOutput in ChatUiState"
```

---

### Task 2: ChatViewModel — split accumulation logic

**Files:**
- Modify: `app/src/main/java/com/chatapp/ui/chat/ChatViewModel.kt`

**Purpose:** Route Thinking chunks to `streamingThinking`, Content chunks to `streamingOutput`. Add `toggleThinkingCollapse()`. Update all `streamingContent` references.

- [ ] **Step 1: Update Thinking chunk handler (lines ~137-148)**

Replace:
```kotlin
is StreamChunk.Thinking -> {
    val safe = chunk.text.replace("null", "")
    if (safe.isNotEmpty()) {
        DebugLog.log("NULL", "S5_THK+ txt='${safe.take(80)}'")
        _uiState.update {
            val newContent = it.streamingContent + safe
            it.copy(streamingContent = newContent)
        }
    } else if (chunk.text.isNotEmpty()) {
        DebugLog.log("NULL", "S5_SKIP nullThink len=${chunk.text.length}")
    }
}
```

With:
```kotlin
is StreamChunk.Thinking -> {
    val safe = chunk.text.replace("null", "")
    if (safe.isNotEmpty()) {
        DebugLog.log("NULL", "S5_THK+ txt='${safe.take(80)}'")
        _uiState.update {
            val newThinking = it.streamingThinking + safe
            it.copy(
                streamingThinking = newThinking,
                thinkingTokenCount = (newThinking.length / 2.5).toLong()
            )
        }
    } else if (chunk.text.isNotEmpty()) {
        DebugLog.log("NULL", "S5_SKIP nullThink len=${chunk.text.length}")
    }
}
```

- [ ] **Step 2: Update Content chunk handler (lines ~121-136)**

Replace `it.streamingContent` with `it.streamingOutput`:

```kotlin
is StreamChunk.Content -> {
    val safe = chunk.text.replace("null", "")
    if (safe.isNotEmpty()) {
        DebugLog.log("NULL", "S5_CVM+ txt='${safe.take(80)}'")
        _uiState.update {
            val newContent = it.streamingOutput + safe
            if (safe.isEmpty()) {
                DebugLog.log("NULL", "S6_ACC len=${newContent.length} (no change)")
            }
            it.copy(streamingOutput = newContent)
        }
    } else if (chunk.text.isNotEmpty()) {
        DebugLog.log("NULL", "S5_SKIP nullContent len=${chunk.text.length}")
    }
}
```

- [ ] **Step 3: Update Done handler (lines ~151-171)**

Replace `_uiState.value.streamingContent` and `it.streamingContent` references:

```kotlin
is StreamChunk.Done -> {
    val rawThinking = _uiState.value.streamingThinking
    val rawOutput = _uiState.value.streamingOutput
    val fullContent = rawOutput.replace("null", "")
    DebugLog.log("VM", "=== Stream DONE, think=${rawThinking.length} output=${rawOutput.length} clean=${fullContent.length} ===")
    DebugLog.log("ChatVM", "StreamChunk.Done received")
    chatRepository.updateMessageContent(streamingId, fullContent, null)
    val completedMsg = Message(
        id = streamingId,
        conversationId = activeConversationId,
        role = MessageRole.ASSISTANT,
        content = fullContent,
        status = MessageStatus.COMPLETE
    )
    _uiState.update {
        it.copy(
            isStreaming = false,
            streamingOutput = "",
            streamingThinking = "",
            isThinkingCollapsed = false,
            thinkingTokenCount = 0,
            messages = it.messages.filterNot { m -> m.id == streamingId } + completedMsg
        )
    }
    if (isNew) onConversationCreated?.invoke(activeConversationId)
}
```

- [ ] **Step 4: Update Error handler (lines ~173-189)**

Replace `_uiState.value.streamingContent` with `_uiState.value.streamingOutput`:

```kotlin
is StreamChunk.Error -> {
    DebugLog.log("VM", "Stream error: ${chunk.throwable.message}")
    DebugLog.log("ChatVM", "StreamChunk.Error: ${chunk.throwable.message}")
    chatRepository.updateMessageContent(
        streamingId,
        _uiState.value.streamingOutput,
        null
    )
    _uiState.update {
        it.copy(
            isStreaming = false,
            streamingOutput = "",
            streamingThinking = "",
            errorMessage = chunk.throwable.message ?: "Unknown error"
        )
    }
    if (isNew) onConversationCreated?.invoke(activeConversationId)
}
```

- [ ] **Step 5: Update sendMessage initial state (line ~72)**

Replace:
```kotlin
_uiState.update { it.copy(inputText = "", isStreaming = true, streamingContent = "", errorMessage = null) }
```

With:
```kotlin
_uiState.update { it.copy(inputText = "", isStreaming = true, streamingOutput = "", streamingThinking = "", isThinkingCollapsed = false, thinkingTokenCount = 0, errorMessage = null) }
```

- [ ] **Step 6: Update stopGeneration (lines ~196-209)**

Replace all `streamingContent` references with `streamingOutput`:

```kotlin
fun stopGeneration() {
    streamJob?.cancel()
    val partial = _uiState.value.streamingOutput
    _uiState.update {
        it.copy(isStreaming = false, streamingOutput = "", streamingThinking = "")
    }
    val streamingMsg = _uiState.value.messages.lastOrNull { it.status == MessageStatus.STREAMING }
    if (streamingMsg != null && partial.isNotEmpty()) {
        viewModelScope.launch {
            chatRepository.updateMessageContent(streamingMsg.id, partial, null)
        }
    }
}
```

- [ ] **Step 7: Add toggleThinkingCollapse() method**

Add new method after `stopGeneration()`:

```kotlin
fun toggleThinkingCollapse() {
    _uiState.update { it.copy(isThinkingCollapsed = !it.isThinkingCollapsed) }
}
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/chatapp/ui/chat/ChatViewModel.kt
git commit -m "feat: split thinking/content accumulation in ChatViewModel"
```

---

### Task 3: StreamingBubble — restructure with dividers and toggle

**Files:**
- Modify: `app/src/main/java/com/chatapp/ui/components/StreamingText.kt`

**Purpose:** Redesign StreamingBubble to render thinking section (with title bar, divider, collapsible thinking text) and formal output section, separated by dividers.

- [ ] **Step 1: Replace StreamingBubble composable**

Replace the entire file content:

```kotlin
package com.chatapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun StreamingBubble(
    output: String,
    thinking: String = "",
    isThinkingCollapsed: Boolean = false,
    thinkingTokenCount: Long = 0,
    onToggleThinking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val showThinking = thinking.isNotEmpty()

    Surface(
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 4.dp
        ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.widthIn(max = 320.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (showThinking) {
                // Title bar: "Thinking" + token count + toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thinking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatTokenCount(thinkingTokenCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (isThinkingCollapsed) {
                            Icons.Default.KeyboardArrowDown
                        } else {
                            Icons.Default.KeyboardArrowUp
                        },
                        contentDescription = if (isThinkingCollapsed) "Expand thinking" else "Collapse thinking",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable { onToggleThinking() }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )

                // Thinking text (hidden when collapsed)
                if (!isThinkingCollapsed) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = thinking,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Formal output with blinking cursor
            CursorText(text = output)
        }
    }
}

@Composable
private fun CursorText(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Row {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "|",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(alpha.value)
        )
    }
}

private fun formatTokenCount(tokens: Long): String {
    if (tokens >= 1000) {
        val whole = tokens / 1000
        val frac = ((tokens % 1000) / 100)
        return "$whole.${frac}k tokens"
    }
    return "$tokens tokens"
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/chatapp/ui/components/StreamingText.kt
git commit -m "feat: redesign StreamingBubble with thinking section, dividers, and toggle"
```

---

### Task 4: ChatScreen — update field references

**Files:**
- Modify: `app/src/main/java/com/chatapp/ui/chat/ChatScreen.kt`

**Purpose:** Pass new StreamingBubble parameters and update field references from `streamingContent` to `streamingOutput`.

- [ ] **Step 1: Update StreamingBubble call (lines ~202-211)**

Replace:
```kotlin
if (uiState.isStreaming && uiState.streamingContent.isNotEmpty()) {
    item(key = "streaming") {
        StreamingBubble(
            content = uiState.streamingContent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
}
```

With:
```kotlin
if (uiState.isStreaming && (uiState.streamingOutput.isNotEmpty() || uiState.streamingThinking.isNotEmpty())) {
    item(key = "streaming") {
        StreamingBubble(
            output = uiState.streamingOutput,
            thinking = uiState.streamingThinking,
            isThinkingCollapsed = uiState.isThinkingCollapsed,
            thinkingTokenCount = uiState.thinkingTokenCount,
            onToggleThinking = { viewModel.toggleThinkingCollapse() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
}
```

Note: The visibility condition changed from `streamingContent.isNotEmpty()` to `streamingOutput.isNotEmpty() || streamingThinking.isNotEmpty()` because thinking-only chunks should also show the bubble.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/chatapp/ui/chat/ChatScreen.kt
git commit -m "feat: wire ChatScreen to new StreamingBubble parameters"
```

---

### Task 5: Version bump and verify

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/chatapp/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/chatapp/MainActivity.kt`

- [ ] **Step 1: Bump version**

In `app/build.gradle.kts`:
```kotlin
versionCode = 13
versionName = "0.0.13-alpha"
```

In `app/src/main/java/com/chatapp/ui/home/HomeScreen.kt` line 198:
```kotlin
text = "Chat AI v0.0.13-a",
```

In `app/src/main/java/com/chatapp/MainActivity.kt`:
```kotlin
DebugLog.log("APP", "onCreate version=0.0.13-alpha")
```

- [ ] **Step 2: Build verification**

In Android Studio: Build → Clean Project → Rebuild Project. Fix any compilation errors.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/chatapp/ui/home/HomeScreen.kt app/src/main/java/com/chatapp/MainActivity.kt
git commit -m "v0.0.13-alpha: thinking/content UI separation"
```

---

### Task 6: Agent code review

After all tasks complete, dispatch a code-reviewer agent:

```bash
BASE_SHA=$(git rev-parse HEAD~4)
HEAD_SHA=$(git rev-parse HEAD)
```

Then dispatch agent with:
- DESCRIPTION: Split streamingContent into streamingThinking + streamingOutput, redesigned StreamingBubble with dividers and collapsible thinking section
- PLAN: docs/superpowers/plans/2026-05-14-thinking-content-ui-plan.md
- BASE_SHA + HEAD_SHA

Fix any issues found by the reviewer.
