# CLAUDE.md

## Project Skills

These skills MUST be invoked when their triggering conditions are met:

| Skill | When to use |
|-------|-------------|
| `verify-before-edit` | Adding/changing dependencies, versions, or new functionality |
| `fix-and-learn` | After fixing ANY project error (build, runtime, UI, data, API, navigation, state) — analyze root cause, append prevention rule |
| `verify-fix` | After every bug fix or code change — loop: verify completeness, scan for same anti-pattern elsewhere, check regressions, fix+repeat until clean |

### verify-before-edit

Ensures dependency versions exist before being added, and new features don't duplicate existing code.

Trigger: any edit that references external resources (Maven coordinates, plugin versions, library APIs, Material Icons namespaces) or adds new functionality.

### fix-and-learn

After ANY project error is fixed (build, runtime crash, UI glitch, data bug, API error, navigation issue, state bug), analyzes whether the error was caused by Claude and writes a prevention rule to the skill file.

Trigger: immediately after any project error is resolved.

## Development Workflow (MUST follow this order)

1. **All changes first** — implement every feature, fix, and modification across all files. Do NOT run gradle between individual changes.
2. **Agent debug** — dispatch region-split agents (≤12 files each). Each agent MUST embed systematic-debugging methodology (Phase 1-4: read all files → pattern checks → hypothesis → output). Do NOT dismiss LOW bugs.
3. **Fix all bugs** — address every bug found by agents, including LOW severity.
4. **Compile once** — `./gradlew compileDebugKotlin` at the very end.

**DO NOT** compile after each edit. Compile only once after ALL work is complete.
