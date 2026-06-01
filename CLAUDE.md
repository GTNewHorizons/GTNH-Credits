# CLAUDE.md (GTNH Credits)

## Project Context

- **Forge 1.7.10** (`modId = gtnhcredits`). Subprojects: `libCredits` (shared), `creditsEditor` (Java 21).
- **Auto-generated**: `net.noiraude.gtnhcredits.Tags`. Do not edit.

## Standards: Rigor & Syntax

- **Java Syntax**: Modern (Java 21+) everywhere (via RFG). Use `var`, modern `switch`, lambdas. No inner classes.
- **API Baseline**: `mod`/`libCredits` = **Java 8 API only** (No `List.of`, `isBlank`, etc.).
  `creditsEditor` = **Java 21 API**.
- **Imports**: No wildcard imports.
- **Logic**: No `instanceof` (use polymorphism/visitors). Use guard clauses (flat paths).
- **Architecture**: Strict SOLID. No state duplication. No "TODO" hacks.
- **Ambiguity**: If a clean SOLID solution isn't clear: **STOP & ASK.**

## Hard Constraints

- **Locales**: Use `Locale.ROOT` for IDs/IO/Keys. No `toLowerCase()` without Locale. Use
  `String.valueOf(c).toLowerCase(Locale.ROOT)` for chars.
- **Nulls**:
  - Returns: `Optional<T>`. Params: No `Optional`.
  - Hot Paths: Use raw nulls and guard clauses in tight loops/rendering to avoid allocation.
  - Boundaries: Wrap legacy nulls in `Optional` unless in a performance-critical hot path.
  - Assertions: Use `Objects.requireNonNull()` for mandatory internal state.

## Workflow & Git

- **Formatting**: Run `./gradlew spotlessApply` before every commit.
- **Commits**: **Conventional Commits** (subject line only). Max 50 chars. Format: `type(scope): description`.

## Commands

- Build: `./gradlew build` | Editor: `./gradlew :creditsEditor:run` | Test: `./gradlew test`
