---
name: milestone-execution
description: Use when implementing or delegating milestone-oriented work; split slices, optional milestone/dev-log updates, handoff hints. Code and rules before markdown.
---

# Milestone execution

## Read order

1. Relevant **application source** and **tests** for the task.
2. **`.cursor/rules/*.mdc`** that apply (e.g. `agent-code-source-of-truth.mdc`, stack rules).
3. **`docs/milestones.md`** for delivery context (non-authoritative).
4. Optional: **`docs/dev-log.md`** for recent notes.
5. Workflow-specific **`.cursor/skills/*/SKILL.md`** when needed.

Do **not** assume archived or narrative docs match the codebase.

## Work split

1. Pick one milestone slice that can be implemented and validated independently.
2. Lock explicit in-scope and out-of-scope files before editing.
3. Prefer one coherent slice at a time.

## When to update docs

- Update **`docs/milestones.md`** / append **`docs/dev-log.md`** when completing meaningful slices if the task calls for it—not as a substitute for verifying behavior in code.

## Codex handoff

Pass goal, in/out of scope paths, relevant rules, and skill paths with one-line summaries. Do not assume external tools discover `.cursor/skills/` automatically.
