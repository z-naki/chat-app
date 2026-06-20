# Markdown Link Clickable + Settings Multimodal Placeholder

**Date:** 2026-06-12
**Version:** v0.0.19-alpha → v0.0.20-alpha

## 1. Markdown Link Clickable

### Current State
`buildFormattedAnnotatedString()` in `MarkdownContent.kt` already parses `[text](url)` patterns and annotates them with `pushStringAnnotation("URL", url)`. However, the outer composables use plain `Text()`, which ignores clickable annotations.

### Change
Replace `Text()` with `ClickableText` from `androidx.compose.foundation.text` for all inline-formatted text segments:

- `MarkdownSegment.Text`
- `MarkdownSegment.UnorderedListItem`
- `MarkdownSegment.OrderedListItem`
- `MarkdownSegment.Blockquote`
- `MarkdownSegment.Heading`

On click, detect `StringAnnotation("URL")` at the tap offset and open via `LocalUriHandler.current.openUri(url)`.

- `ClickableText` is already in the Compose BOM dependency — no new dependency needed.
- Code blocks and tables are not affected (they don't use `buildFormattedAnnotatedString`).

### Files Modified
- `ui/components/MarkdownContent.kt`

## 2. Settings — Third-party Multimodal Placeholder

### Purpose
Reserve a category in Settings for future third-party multimodal API configuration. No functional logic — pure UI placeholder.

### UI Placement
Between "Network" section and "About" section in SettingsScreen:

```
╔══════════════════════════╗
║ Network                  ║  ← existing
╠══════════════════════════╣
║ 第三方多模态               ║  ← NEW section
║ ┌──────────────────────┐ ║
║ │ Provider: Default    │ ║  ← placeholder row, Edit click does nothing
║ └──────────────────────┘ ║
╠══════════════════════════╣
║ About                    ║  ← existing
╚══════════════════════════╝
```

### Data
- `SettingsUiState.multimodalProvider: String = "Default"`
- No persistence — value is session-only placeholder.

### Files Modified
- `ui/settings/SettingsUiState.kt` — add `multimodalProvider` field
- `ui/settings/SettingsScreen.kt` — add section UI
- `ui/settings/SettingsViewModel.kt` — add placeholder setter (no-op body)

## 3. Version Bump
- `versionCode`: 19 → 20
- `versionName`: "0.0.19-alpha" → "0.0.20-alpha"
- `HomeScreen.kt` title text: "Chat AI v0.0.19-a" → "Chat AI v0.0.20-a"
