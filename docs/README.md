# Docs Structure

This `docs/` folder organizes product direction, execution, and domain knowledge for the Humanaity project. It reflects the **agile methodology** used to build the application: roadmap-driven planning, sprint-based delivery, and locked specs for domain invariants.

## Files and folders

| File / folder | Purpose |
|---------------|---------|
| `roadmap.md` | Product vision, epics, feature dependency map, implementation order |
| `sprints/` | Sprint-by-sprint execution documents with intent, scope, task breakdown, and completion status |
| `specs/` | Locked domain specs (deterministic simulation, history ledger, read model, etc.) |
| `agent-context.md` | How rules, skills, docs, and prompt packs are split for Cursor and Codex |

## Methodology in practice

- **Roadmap** — Defines what comes next. Epics are broken into features and implementation tasks with dependencies.
- **Sprints** — Each sprint has a clear intent, in-scope/out-of-scope, and chunk-level tasks. Execution status is kept up to date as work completes.
- **Specs** — Domain semantics (e.g. determinism rules, event schema) are locked before implementation and referenced by sprints.
- **Prompt packs** — Delegable task packs for Cursor/Codex point to sprint and spec files instead of restating the project.

This documentation represents **what was actually executed**, not aspirational planning. Sprints include completion tables and handoff notes.

## Current workflow

Use `roadmap.md` to decide **what comes next**.

Use files in `sprints/` to decide:

- what this sprint is trying to achieve
- what is in and out of scope
- which tasks can be delegated to Cursor or Codex
- what acceptance criteria define “done”

## Planned later additions

If the project grows, this folder can later expand with:

- `decisions/` for architecture and product ADR-style notes
