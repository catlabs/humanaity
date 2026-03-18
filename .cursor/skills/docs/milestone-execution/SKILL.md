---
name: milestone-execution
description: Use when implementing or delegating milestone-oriented work in HUMANAIty; explains how to split work, which docs/rules to read first, when to update `docs/milestones.md` and `docs/dev-log.md`, and how to prepare a Codex handoff packet.
---

# Milestone Execution

## Read order
1. `docs/agent-context.md`
2. `docs/milestones.md`
3. the relevant `docs/concepts/*.md`
4. the relevant `docs/specs/*.md`
5. the relevant `.cursor/rules/*.mdc`
6. any workflow-specific `.cursor/skills/*/SKILL.md`

## Work split
1. Pick one milestone slice that can be implemented and validated independently.
2. Lock explicit in-scope and out-of-scope files before editing.
3. Prefer one coherent slice at a time instead of mixing multiple milestones unless the code path truly requires it.

## When to update docs
- Update `docs/milestones.md` after each completed task or meaningful milestone slice.
- Append `docs/dev-log.md` after each meaningful work slice or workday.
- Update the relevant `docs/concepts/*.md` when active architecture or behavior framing changes.
- Update the relevant `docs/specs/*.md` when contracts, DTOs, invariants, or deterministic rules change.
- Touch `docs/archive/*` only for traceability or factual corrections.

## Codex handoff rule
Do not assume Codex plugin or Codex online can discover `.cursor/skills/` automatically.

When handing work to Codex, pass:

- the goal
- explicit in-scope and out-of-scope files
- the read-first file list
- the relevant `.cursor/rules/*.mdc`
- the relevant skill paths plus a one-line summary of why each skill matters
- the docs update obligations before completion

## Codex handoff packet

```text
Goal:

In scope:

Out of scope:

Read first:
- docs/agent-context.md
- docs/milestones.md
- docs/concepts/<relevant>.md
- docs/specs/<relevant>.md

Rules:
- .cursor/rules/agent-context-layering.mdc
- .cursor/rules/sprint-doc-sync.mdc
- .cursor/rules/sprint-chunk-completion-gate.mdc
- <other relevant rules>

Skills to follow:
- .cursor/skills/docs/milestone-execution/SKILL.md — read order, work split, docs update cadence, Codex handoff
- <other relevant skill path> — <why it matters>

Docs to update before finishing:
- docs/milestones.md
- docs/dev-log.md
- <relevant concept/spec docs>
```
