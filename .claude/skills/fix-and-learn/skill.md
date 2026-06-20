---
name: fix-and-learn
description: Use when ANY project error occurs — build failures, compilation errors, runtime crashes, UI glitches, data persistence bugs, API errors, navigation issues, state management bugs. Triggers immediately after fixing any error regardless of type. Analyze root cause, determine if it was self-inflicted (AI invented versions, guessed APIs, missed imports, wrong assumptions), extract a prevention rule, and append it to this skill.
---

# Fix and Learn

## Overview

Every error caused by Claude is a learning opportunity. The skill is loaded BEFORE fixing and written AFTER fixing.

## The Loop

```
Error occurs → Load this skill → Fix it → Ask: "Was this my fault?"
    → Read existing entries → Consolidate or add → Write to this skill

If user sends follow-up (same error, fix didn't work):
    → Load this skill again → Note: "Why did previous fix fail?" → Fix properly
    → Write: include both the failed fix reason AND the final fix

If user does NOT send follow-up:
    → Error is fully resolved → Final write stands
```

## Trigger Timing (Critical)

| Phase | Action |
|-------|--------|
| **Before fixing** any error | Load (read) this skill to see past patterns |
| **After fixing** an error | Write to this skill — then tell user what was added |
| **User sends follow-up** (same error persists) | Load skill again; the next write MUST explain why the previous fix failed |
| **User does NOT send follow-up** | Error is resolved; the last write is the final record |

## How to Add an Entry

### Step 1: Load and read existing entries first

Before fixing OR writing, read the full skill. Ask: does this error share a root cause with an existing entry?

### Step 2: Decide — merge, generalize, or add

| Situation | Action |
|-----------|--------|
| Same root cause, same API surface | **Merge**: add the new example to the existing entry's Prevention list |
| Same root cause, broader pattern | **Generalize**: rewrite the existing entry to cover both cases, with examples of each |
| Genuinely new root cause | **Add**: create a new entry with a unique number |

### Step 3: Multi-attempt fixes — record the failed fix

If an error needed MORE THAN ONE attempt to fix, the entry MUST include:

```
**Failed Fix:** [What was tried first — and WHY it didn't work]

**Final Fix:** [What actually resolved it]
```

This is the most valuable learning data. The failed fix reveals a wrong assumption.

### Step 4: Report to user

After writing to this skill, tell the user explicitly:

```
[fix-and-learn] 写入: [merge/generalize/add] 到 Entry N — [一句话描述变动]
```

This confirms the edit happened and what changed. Do NOT silently write to the skill.

### Step 5: Keep it compact (unchanged — merge/generalize/add)

- One entry = one root cause category, not one error instance
- Prevention section is what matters — compress Error/Fix lines
- Delete entries fully covered by `verify-before-edit` (cross-reference instead)
- Target: under 150 lines. If exceeding, consolidate aggressively.

### Step 6: Format for new entries

```
### Entry N: [Short description of the root cause category]

**Error:** [What the build/log showed]

**Root Cause:** [Why Claude made this mistake]

**Fix:** [What was changed]

**Prevention:** [Concrete rule + reference if needed]
```

If multi-attempt:

```
### Entry N: [Short description]

**Error:** [What the build/log showed]

**Root Cause:** [Deeper reason]

**Failed Fix:** [What was tried first] — **Why it failed:** [The wrong assumption]

**Final Fix:** [What worked]

**Prevention:** [Concrete rule]
```

---

## Accumulated Errors

### Entry 1: KSP version 2.0.10-1.0.28 does not exist

**Error:** `Plugin [id: 'com.google.devtools.ksp', version: '2.0.10-1.0.28'] was not found`

**Root Cause:** Claude changed the KSP version from `2.0.10-1.0.24` to `2.0.10-1.0.28` without verifying the version exists in Maven Central. The version was fabricated — Claude assumed a higher patch number would exist. It does not.

**Fix:** Reverted to `2.0.10-1.0.24`, which was previously confirmed working.

**Prevention:** Before changing ANY version string (plugin, library, tool, Gradle, AGP), use `WebSearch` with the exact version to confirm the artifact exists in the target repository. This is enforced by the `verify-before-edit` project skill.

---

### Entry 2: Icons.AutoMirrored.Filled.Refresh does not exist

**Error:** `Unresolved reference 'Refresh'` at `Icons.AutoMirrored.Filled.Refresh`

**Root Cause:** Claude pattern-matched from other `Icons.AutoMirrored.Filled.*` usages in the codebase (ArrowBack, Send, List) and assumed `Refresh` also lived there. It does not. `AutoMirrored.Filled` only contains directionally-sensitive icons that flip in RTL layouts. `Refresh` is rotationally symmetric and only exists in `Icons.Filled`.

**Fix:** Changed to `Icons.Filled.Refresh`.

**Prevention:** NEVER guess Material Icons namespace membership based on pattern-matching from nearby code.
- `Icons.AutoMirrored.Filled` = ONLY directionally-sensitive icons (arrows, send, navigation, undo, redo)
- `Icons.Filled` / `Icons.Default` = ALL icons (safe fallback)
- `Icons.Outlined` = ALL icons, outlined style
- If unsure whether an icon belongs in `AutoMirrored.Filled`, use `Icons.Filled` instead.
- For new icons, search: `WebSearch` `"Icons.AutoMirrored.Filled.<Name> site:developer.android.com"`
- Examples that exist in AutoMirrored: `ArrowBack`, `ArrowForward`, `Send`, `List`, `OpenInNew`, `Undo`, `Redo`, `NavigateNext`, `NavigateBefore`, `KeyboardArrowLeft`, `KeyboardArrowRight`
- Examples that do NOT exist in AutoMirrored: `Refresh`, `Close`, `Check`, `Add`, `Delete`, `Edit`, `Search`, `Settings`, `Home`, `Person`, `MoreVert`, `SearchOff`

---

### Entry 3: Unresolved reference 'imePadding' on Modifier

**Error:** `Unresolved reference 'imePadding'` when building after adding `Modifier.imePadding()` usage in `ChatScreen.kt`.

**Root Cause:** Claude wrote `modifier.imePadding()` (an extension function on `Modifier`) without adding the corresponding import `androidx.compose.foundation.layout.imePadding`. Kotlin extension functions require explicit imports — they are NOT transitively available just because other `androidx.compose.foundation.layout.*` members are already imported. Claude assumed the extension function would resolve from existing package-level imports or simply overlooked the missing import.

**Fix:** Added `import androidx.compose.foundation.layout.imePadding` to the imports block in `ChatScreen.kt`.

**Prevention:** When writing ANY `Modifier.xxx()` call (or any extension function), verify the corresponding import exists in the file. Extension functions in Kotlin are individual top-level declarations — there is no "import the whole Modifier surface" shortcut.
- Rule: For each new `Modifier.xxx()` usage, ensure there is a matching `import <package>.xxx` line.
- Common Compose modifier imports and their packages:
  - `fillMaxSize`, `fillMaxWidth`, `fillMaxHeight`, `padding`, `size`, `height`, `width`, `offset`, `wrapContentSize` → `androidx.compose.foundation.layout`
  - `imePadding`, `navigationBarsPadding`, `statusBarsPadding`, `systemBarsPadding`, `windowInsetsPadding` → `androidx.compose.foundation.layout`
  - `clip`, `border`, `background`, `shadow` → `androidx.compose.foundation`
  - `clickable`, `combinedClickable`, `toggleable`, `selectable` → `androidx.compose.foundation`
  - `semantics`, `testTag`, `clearAndSetSemantics` → `androidx.compose.ui.semantics`
- After adding any new Modifier extension, run the build (or check the IDE gutter for red underlines) to catch missing imports immediately.

---

### Entry 4: Markdown table parser causes app freeze (infinite loop)

**Error:** AI output containing markdown tables caused the app to become completely unresponsive. No crash log — just a freeze.

**Root Cause:** Claude's `parseSegments()` function had a nested detection deadlock. The inner plain-text accumulation loop tried to look ahead and detect table headers, then `break` without advancing `i`. The outer table-detection block used the same regex but could fail to match (e.g., separator row had different formatting). When both loops deferred to each other, `i` never advanced — infinite loop on the same line.

Specifically: inner loop condition `l.contains("|") && lines[i+1].contains("|") && lines[i+1].contains("-")` triggered a break, but outer detection `nextLine.trim().matches(Regex(...))` failed the regex. `i` stayed at the same value, and the while-loop re-ran identically.

**Fix:** Extracted table separator check into `isTableSeparator()` function. Inner loop now uses the same function as outer loop — no dual detection paths that can disagree. Removed table lookahead from plain-text accumulation entirely (let outer loop handle it alone).

**Prevention:** When writing ANY parsing or state-machine loop (`while (i < size)` with conditional `continue`/`break`):
- Never have TWO code paths that both try to detect the same pattern and both assume the OTHER will advance `i`.
- If pattern detection can fail, the same code path that detects MUST also advance past (or skip) the ambiguous input.
- Extract ambiguous condition checks into a single function, used by all loops.
- For any `while` loop over an index: verify there's exactly ONE path that increments `i` per iteration. If a `break` or `continue` can skip the increment, confirm the condition will be different next iteration.

---

### Entry 5: animateContentSize() on text label causes layout jump

**Error:** When selecting a theme option (Light/Dark/System), the label text visually jumps to the middle of the screen during the transition.

**Root Cause:** Claude added `animateContentSize()` to the Row containing the theme label. When the selected value changes, the label text width changes ("System" = 6 chars, "Dark" = 4 chars, "Light" = 5 chars). `animateContentSize()` animates this width delta, causing the text to appear to slide/jump while the Row resizes.

`animateContentSize()` is designed for containers where content conditionally appears/disappears (e.g., expandable sections, toggled detail text). Using it on a label whose text changes width is incorrect — the text itself isn't expanding, only changing to a different word.

**Fix:** Removed `animateContentSize()` from the Row modifier chain (and the now-unused import).

**Prevention:** Before adding `animateContentSize()` to any composable, verify the content inside is changing in a way that BENEFITS from size animation:
- Valid: conditional content appearing/disappearing (AnimatedVisibility content, expandable details)
- Invalid: label text changing to a different word (dropdown selection, toggle state label)
- Test: if the content can change to a SHORTER text, `animateContentSize()` will cause a visible shrink animation that may displace surrounding elements.
- For theme selectors, toggle labels, or any text that changes to a different value with different length: do NOT animate the container size.

---

### Entry 6: Kotlin lambda scope leak + Compose API version assumptions

**Error (5 compile errors in one build):**
1. `OpenAiProvider.kt:74` — `Unresolved reference 'id'` in `.sortedByDescending { id }`
2. `ChatScreen.kt:104` — `Unresolved reference 'launch'`
3. `ChatScreen.kt:105` — `Suspension functions can only be called within coroutine body`
4. `ChatScreen.kt:108` — `No parameter with name 'animationSpec' found`
5. `MarkdownContent.kt:227` — `@Composable invocations can only happen from the context of a @Composable function`

**Root Cause:**
- Error 1: Lambda parameter `id` from `.filter { id -> ... }` leaked into `.sortedByDescending { id }` chain. Kotlin lambda parameters are scoped to their own `{ }` block — chained lambda on the next line does NOT have access. Claude assumed chained method calls share scope.
- Errors 2-3: Claude used `scrollScope.launch { animateScrollToItem(...) }` without verifying that `scrollScope` was typed as a `CoroutineScope` or that the `launch` import existed. The original code used `LaunchedEffect` which correctly provides a coroutine context.
- Error 4: `animateScrollToItem(animationSpec = ...)` — Claude assumed the named parameter exists. The Compose BOM version in this project (`compose.bom` managed) does not expose `animationSpec` on `animateScrollToItem`. Named parameters must be verified against the actual API surface of the project's dependency versions.
- Error 5: `@Composable` function called inside `remember { }` block. `remember`'s lambda is NOT a `@Composable` context — `MaterialTheme` reads must happen OUTSIDE `remember`, with values captured as parameters.

**Fix:**
1. Changed `.sortedByDescending { id }` → `.sortedByDescending { it }`
2-3. Removed `scrollScope` entirely; reverted to `listState.animateScrollToItem(index)` (no custom animation spec)
4. Removed `CoroutineScope` import and `scrollScope` variable
5. Moved `MaterialTheme.colorScheme` reads into `ClickableMarkdownText` composable, passed colors as plain parameters to `buildFormattedAnnotatedString`, making it non-@Composable

**Failed Fix (error 5):** Left `@Composable` annotation on `buildFormattedAnnotatedString` after adding color params — still called inside `remember`. **Why it failed:** The function was still composable even though it no longer used any `@Composable` APIs — `@Composable` annotation overrides the call-site check. Only way to fix is to remove the annotation OR call it outside `remember`. Final fix: removed `@Composable` AND passed colors from the caller.

**Failed Fix (build 2 — KSP "Expecting a top level declaration"):** Removed `scrollScope.launch` body from `LaunchedEffect` but left behind the closing braces (`}}`) that were part of the removed code block. **Why it failed:** When `scrollScope.launch { animateScrollToItem(...) }` was removed and replaced with a simple `listState.animateScrollToItem(index)`, the extra two `}` from the old `launch { }` block + its enclosing scope remained. This caused the compiler to see the `ChatScreen` function body as closed prematurely — all subsequent code (another `LaunchedEffect`, `Scaffold`, `LazyColumn`) was treated as top-level declarations, triggering "Expecting a top level declaration" on every token. Final fix: removed the two orphaned `}`s.

**Prevention:**
1. **Chain lambda scope:** `list.map { x -> ... }.filter { y -> ... }` — each lambda's parameters are private to that block. `y` cannot reference `x` (unless captured via closure). After writing chained collection operations, verify each lambda parameter is self-contained.
2. **Compose BOM API surface:** This project uses `compose.bom` (version managed via `libs.versions.toml`). Named parameters on Foundation/Lazy APIs are NOT guaranteed across versions. Before using `animationSpec`, `scrollOffset`, or any named parameter on `animateScrollToItem`, `scrollToItem`, or similar functions — use `Grep` on the project's Compose dependency to verify the parameter exists, OR use the simplest overload (positional args only).
3. **CoroutineScope in Compose:** Never manually create `rememberCoroutineScope()` for one-off suspend calls in `LaunchedEffect`. `LaunchedEffect` already provides a coroutine context. Use `listState.animateScrollToItem()` directly (it's a suspend function callable inside `LaunchedEffect`'s lambda).
4. **@Composable inside remember:** `remember(key)` block is a plain Kotlin lambda, NOT composable. Move all `MaterialTheme.*` / `Local*` / `@Composable` reads OUTSIDE the remember block, capture as `val`, then pass into the block as key/value.
5. **Pre-commit check:** After ANY edit that adds/changes method calls with named parameters, lambda chains, or `@Composable` annotations, mentally verify the following before declaring work done:
   - Nullable mismatch: `val x: T?` passed to `fun f(p: T)` → needs `?:` or `if (x != null)` guard
   - Kotlin `copy()` strictness: `TextStyle.copy(textAlign = myAlign)` where `myAlign: TextAlign?` fails because `copy()` takes `TextAlign`, not `TextAlign?`
   - Lambda scope chain: `.filter { x -> ... }.sortedBy { x }` — second `x` is NOT the same variable
   - Brace count: after removing a block like `scope.launch { ... }`, check for leftover `}` from the removed block
   - Compose API version: named params like `animationSpec` on `animateScrollToItem` are Compose-version dependent — don't assume they exist
