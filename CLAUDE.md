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
