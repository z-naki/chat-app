---
name: verify-fix
description: Use after any bug fix or code change in the chat-app project — verifies the fix addresses the original issue, scans for the same anti-pattern in other files, and checks for regressions (unused imports, layout bugs). Loop until clean.
---

# Verify Fix — Post-Fix Self-Review Loop

After modifying code for a bug fix, run this checklist. If any check fails, fix the issue and restart from step 1. Only declare "done" when all checks pass.

## Checklist

Execute in order. For each check, report PASS or FAIL with file:line evidence.

### 1. Fix Completeness

Re-read the original bug description. Does the change actually solve it?

| Check | How |
|-------|-----|
| Root cause addressed | The diff targets the cause, not a symptom |
| No dead code left | Removed old patterns, not just commented out |
| Correct imports | New APIs imported, unused imports removed |

### 2. Pattern Scan — Same Bug Elsewhere

Search the codebase for the same anti-pattern just fixed.

- `DropdownMenu` inside `Row`/`Column` without `Box` wrapper → layout jump
- `AnimatedVisibility` inside 0-height parent → content clipped
- `clickable` before `padding` in modifier chain → click target wrong size
- `Modifier.widthIn(min=…)` on label next to popup → unnecessary now

**Command:** Grep for the fixed API/pattern across all `.kt` files under `app/src/`.

### 3. Compose Layout Sanity

For Compose UI changes, verify:

- `DropdownMenu` always wrapped in `Box` (never direct child of `Row`/`Column`)
- `Popup`-based composables (`DropdownMenu`, `AlertDialog`) placed in a parent that has non-zero bounds at the correct anchor position
- `AnimatedVisibility` parent does NOT clip children (no 0-height `Box` without `clipToBounds` disabled)
- `Modifier` ordering: `clickable` → `padding` (not reversed)

### 4. Import Hygiene

```bash
# Find unused imports in changed files
grep "^import" <file> | while read imp; do
  cls=$(echo "$imp" | sed 's/.*\.\([^.]*\)$/\1/')
  grep -q "$cls" <file> || echo "UNUSED: $imp"
done
```

### 5. Version Consistency (chat-app specific)

If the bug fix touches UI strings, verify `versionName` is consistent across:
- `app/build.gradle.kts` (`versionName`)
- `HomeScreen.kt` (title bar version)
- `SettingsScreen.kt` (About section version)

## Loop Rule

```
fix → verify → FAIL? → fix again → verify → PASS → done
```

Never skip a check after a re-fix. If the same check fails 3 times, escalate: the approach may be wrong.

## Common Failures & Fixes (chat-app)

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Label jumps on click | `DropdownMenu` inside `Row` | Wrap trigger+menu in `Box` |
| Dropdown covers trigger | Anchor at same Y as content | Use `Box(height=0, BottomCenter)` below content |
| Clipped animation | `AnimatedVisibility` in 0-height parent | Move outside, or use `Popup` |
| Dropdown expands wrong direction | Wrong anchor alignment | `Box(align=TopEnd)` inside bottom anchor |
