# Sprint Execution Contract

This document defines the **end-of-task** behavior when implementing sprint work. It is repo-visible so both Cursor and Codex can follow it. When delegating sprint tasks to Codex, include this doc so the agent knows to update status and commit after each task.

## When it applies

Whenever work is done on a sprint task (single task or a range of sprints), the following applies at the **end of each task** before moving to the next.

## End-of-task steps

1. **Scope and validation**
   - Confirm the work stayed within the task ID and in-scope files.
   - Run chunk-level validation (tests for touched behavior).

2. **Update execution status**
   - Update the sprint doc `## Execution status` block:
     - Set `Last completed chunk` to the task just finished.
     - Set `Active chunk` / `Next chunk` to the next task.
     - Update the chunk status table row for this task to `done`.

3. **Commit the task**
   - Stage only the files changed for **this task**.
   - Create **one commit** for this task.
   - Use the commit message format below.
   - **Do not push** unless the user explicitly asks.

## Commit message format

Use Conventional Commit with epic and task in the subject:

```text
<type>(<scope>): E<epic> T<task> <subject>
```

- **type**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, etc.
- **scope**: domain (e.g. `simulation`, `auth`, `api`) when one area dominates.
- **epic**: roadmap epic number from `docs/roadmap.md` (e.g. 8, 9, 10).
- **task**: task ID from the sprint doc (e.g. `1a`, `2a`, `2b`).
- **subject**: imperative, short, no period; e.g. "Add orchestration endpoint skeleton".

Examples:

- `feat(api): E8 T2a Add city-scoped agent chat endpoint skeleton`
- `refactor(simulation): E9 T3a Wire orchestration response into console`

Map epic from the roadmap; map task from the sprint doc. One commit per task. Do not push unless requested.

## For Cursor

Cursor uses `.cursor/rules/sprint-chunk-completion-gate.mdc` and `.cursor/rules/docs-chunk-review-loop.mdc` plus the commit-message skill (`.cursor/skills/commit-message/SKILL.md`) for full format and staging rules.

## For Codex

When delegating a task to Codex, include this file so Codex can follow the same contract: update sprint execution status and create one commit per task with the format above; do not push.
