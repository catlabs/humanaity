---
name: commit-message
description: Generate Conventional Commit messages for the Humanaity monorepo by analyzing staged/unstaged changes and splitting unrelated work into focused commits.
---

# Commit Message Generator

## Goal
Create clear, human-readable Conventional Commit messages and make local commits when requested.

## Source of truth
Before writing a message, inspect:
- `git status --porcelain`
- `git diff --staged` (or `git diff` when nothing is staged)
- related active docs (`docs/concepts/`, `docs/specs/`, `docs/roadmap.md`) when useful for context

## Format
Title:

```text
<type>(<scope>): <concise summary>
```

Body (optional): rationale, constraints, or traceability context.

## Rules
- Prioritize human readability.
- Keep title under 72 chars.
- Use imperative mood (`Add`, `Fix`, `Refactor`, `Update`).
- Do not include internal planning IDs in the title.
- Use scope when one area clearly dominates; omit for broad changes.
- Split unrelated changes into separate commits when practical.
- Do not push unless explicitly requested.

## Types
`feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `chore`, `style`, `build`, `ci`, `config`, `security`

## Procedure
1. Detect changed files.
2. Group by coherent intent.
3. Draft one message per group.
4. Stage each group and commit.
5. Report local hashes and titles.
