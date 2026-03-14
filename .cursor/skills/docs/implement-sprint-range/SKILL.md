---
name: implement-sprint-range
description: Execute a range of sprints (e.g. sprint 8 to 11) in order, task by task, updating execution status and creating one commit per task. Use when the user asks to implement sprints X to Y, run sprints 8–11, or do all work from sprint N through sprint M.
---

# Implement Sprint Range

## Goal

When the user says e.g. "implement sprint 8 to 11", work through the requested sprints in **numeric order**, and within each sprint through tasks in **sprint order**. At the end of **each task**, apply the chunk completion gate (update execution status, run validation, **commit that task's changes**, do not push).

## Related context

- Chunk completion: `.cursor/rules/sprint-chunk-completion-gate.mdc`
- Chunk review loop: `.cursor/rules/docs-chunk-review-loop.mdc`
- Commit format and epic/task mapping: `.cursor/skills/commit-message/SKILL.md` (Cursor); for Codex use `docs/sprint-execution-contract.md`
- Execution contract (Codex-visible): `docs/sprint-execution-contract.md`

## When to use this skill

- User says "implement sprint 8 to 11", "do sprints 8 through 11", "implement from sprint 8 to sprint 11", or similar.
- User wants one agent (Cursor or Codex) to perform all work for a contiguous set of sprints.

## How to execute

### 1. Resolve the range

- Parse sprint numbers (e.g. 8 to 11 → sprints 8, 9, 10, 11).
- Confirm sprint folders exist under `docs/sprints/sprintNN/` and each has a main sprint doc `sprint-NN-<theme>.md` and a prompt pack `sprint-NN-prompt-pack.md`.

### 2. Order of work

- For each sprint in ascending order (8, then 9, then 10, then 11):
  - Read the sprint doc and its prompt pack.
  - Work through tasks in the order given in the sprint (e.g. Task 1a, 2a, 2b, 3a, …).
  - Respect dependencies and "blocked" items; do not skip ahead if the sprint doc says a task is blocked.

### 3. Per-task completion (mandatory)

At the **end of each task**, before starting the next:

1. **Scope** — Confirm the diff is only for this task and in-scope files.
2. **Validation** — Run chunk-level tests for touched behavior.
3. **Execution status** — Update the sprint doc `## Execution status`: last completed chunk, next chunk, chunk table row to `done`.
4. **Commit** — Stage only this task's changes and create **one commit**:
   - In Cursor: use the commit-message skill (epic/task from roadmap and sprint doc).
   - For Codex: follow `docs/sprint-execution-contract.md` (format: `<type>(<scope>): E<epic> T<task> <subject>`; do not push).
5. Do **not** push unless the user explicitly asks.

### 4. Handoff to Codex

If the user delegates the range to Codex (or another external agent), provide:

- This skill’s expectations (sprint order, task order, one commit per task).
- `docs/sprint-execution-contract.md` so Codex can update status and format commits without access to Cursor skills.
- The relevant sprint doc(s) and prompt pack(s) for the range.
- Relevant specs and `.cursor/rules` that apply to the work.

So "implement sprint 8 to 11" is enough context once this contract and the execution doc are included in the handoff.

## Summary

- **Sprint range** = work sprint N, N+1, … M in order.
- **Task order** = follow each sprint doc’s task order and dependencies.
- **End of each task** = update execution status + one commit (commit-message skill or execution contract); no push unless asked.
