---
name: verify-before-edit
description: Use when editing project files, adding dependencies, changing versions, or introducing new libraries. Triggers on any edit that references external resources (Maven coordinates, plugin versions, library APIs) or adds new functionality that might duplicate existing code.
---

# Verify Before Edit

## Overview

Before any edit that adds a dependency, changes a version, or introduces new functionality, verify it exists and isn't already in the project.

## When to Use

- Adding or changing a dependency version in any build file
- Adding imports that reference external libraries
- Introducing new functionality that may duplicate existing code
- After editing: if you added new external references, verify them

## Two Verification Rules

### Rule 1: Dependency versions MUST be verified

Before adding or changing ANY version string (plugin, library, tool), use `WebSearch` with query `"<group>:<artifact>:<version> maven"` to confirm the artifact exists.

```
// BEFORE adding this to build.gradle.kts:
implementation("io.coil-kt:coil-compose:3.1.0")

// MUST run:
WebSearch(query = "io.coil-kt coil-compose 3.1.0 maven central")
```

**Applies to:**
- `libs.versions.toml` version changes
- `build.gradle.kts` dependency additions
- Plugin version changes
- Gradle wrapper version changes

**No exceptions:**
- Not for "common libraries I know exist"
- Not for "versions I remember using before"
- Not for "patch bumps that should be safe"
- Any version change = verification required

### Rule 2: New functionality MUST check for existing implementations

Before adding new code for a feature, search the project for existing implementations:

```
# BEFORE adding image loading:
Grep(pattern = "coil|glide|picasso|imageload", path = "app/")
Grep(pattern = "AsyncImage|rememberAsyncImagePainter", path = "app/")
Glob(pattern = "**/build.gradle*")  # Check existing dependencies
```

**Failure to check = potential duplicate code, broken builds, wasted time.**

## Quick Reference

| Action | Verification |
|--------|-------------|
| New dependency version | `WebSearch` to confirm artifact exists in Maven/Google repo |
| New plugin version | `WebSearch` for plugin portal or Gradle plugin repository |
| New library import | `Grep` project for existing usage, `WebSearch` for API availability |
| New feature/function | `Grep` project for similar implementations |
| Gradle version change | `WebSearch` "gradle X.Y.Z release" |
| AGP version change | `WebSearch` "android gradle plugin X.Y.Z release notes" |

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| "I know this version exists" | Versions get pulled, renamed. Verify anyway. |
| "It's just a patch bump" | Patch bumps can disappear. 30 seconds saves rebuild. |
| "Project is small, nothing to duplicate" | Even small projects have hidden patterns. Grep first. |
| "I'll check after adding it" | Broken builds waste more time than a 10-second search. |
| "I saw it on maven.org before" | Check NOW, not from memory. |

## Red Flags - STOP and Verify

- Adding any version string to any file
- Copying a dependency from another project
- Assuming a library "probably" exists at a version
- "I'll fix the version later if it fails"
- Adding functionality without searching the codebase first

**All of these mean: Pause. Verify. Then edit.**
