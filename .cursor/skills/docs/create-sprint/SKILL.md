---
name: create-sprint
description: Create sprint planning documents for the Humanaity repo using the project docs structure, roadmap-first scoping, and feature-adapted file names such as `sprint-01-foundation.md`. Use when the user asks to create a sprint, define sprint scope, organize sprint docs, or break roadmap items into actionable tasks for Cursor or Codex.
---

# Create Sprint

## Goal

Create a sprint document that turns `docs/roadmap.md` into a short, executable plan for the next delivery slice.

## Related durable rule

- Pair this skill with `.cursor/rules/docs-sprint-planning.mdc` for the stable document-structure constraints that external agents may also need.

The sprint document must help the user:

- understand the sprint objective
- know what is in and out of scope
- see current progress at a glance
- delegate focused tasks to Cursor or Codex
- define what “done” means

## Project Conventions

### Docs location

- Keep long-term direction in `docs/roadmap.md`
- Keep sprint execution docs in `docs/sprints/`

### Sprint file naming

Always use a numbered, feature-adapted file name:

```text
sprint-01-foundation.md
sprint-02-events-and-inventions.md
sprint-03-simulation-read-model.md
```

Rules:

- prefix with `sprint-`
- use a two-digit sprint number
- use a short kebab-case feature/theme suffix
- the suffix must describe the sprint outcome, not a vague label like `work`, `next`, or `misc`

Good:

- `sprint-01-foundation.md`
- `sprint-02-history-ledger.md`
- `sprint-03-ui-integration.md`

Avoid:

- `sprint1.md`
- `sprint-1.md`
- `sprint-final.md`
- `sprint-01-stuff.md`

## When To Use This Skill

Use this skill when the user asks to:

- create a sprint file
- organize sprint planning
- translate the roadmap into implementation work
- define sprint scope and delegation
- prepare a handoff document for Cursor or Codex

## Inputs To Gather

Before creating the sprint doc, determine:

1. Which roadmap area the sprint covers
2. The sprint boundary
3. Whether the sprint is backend-first, frontend-first, or mixed
4. What is explicitly out of scope
5. Whether the user wants a spec-first, execution-first, or hybrid sprint doc

Infer from context when obvious. Ask only if the sprint boundary is unclear.

## Sprint Creation Workflow

### 1. Start from the roadmap

Read:

- `docs/roadmap.md`

If relevant, also read:

- `docs/README.md`
- the previous sprint file in `docs/sprints/`

Do not invent a sprint that conflicts with the roadmap unless clearly marked as a recommended change.

### 2. Name the sprint from the outcome

Choose the file name from the main delivery theme.

Examples:

- Deterministic simulation base -> `sprint-01-foundation.md`
- Event persistence and timeline -> `sprint-02-history-ledger.md`
- Frontend integration of real simulation state -> `sprint-03-ui-integration.md`

### 3. Keep the sprint small

A sprint should usually focus on one coherent milestone, not multiple major epics.

Prefer:

- one strong backend foundation sprint
- one history/event sprint
- one read-model/API sprint
- one frontend integration sprint

Avoid combining:

- deterministic engine
- event system
- AI enrichment
- full UI integration

all in the same sprint.

### 4. Write the sprint doc in execution order

Use this structure:

```markdown
# Sprint 01: [Name]

## Execution status
## Sprint intent
## Why this sprint comes first
## Sprint outcome
## Sprint scope
### In scope
### Out of scope
## Product and technical decisions for this sprint
## Deliverables
## Definition of done
## Suggested file targets
## Features and task breakdown
## Recommended implementation order
## Dependencies inside the sprint
## Suggested delegation
## Ready-to-delegate task list
## Risks
## Handoff to next sprint
```

The `## Execution status` section should stay lightweight and scan-friendly. Prefer:

- `Current phase`
- `Active chunk`
- `Next chunk`
- `Last completed chunk`
- a compact chunk status table using `planned`, `in_progress`, `blocked`, or `done`

### 5. Make the tasks delegable

Tasks must be:

- small enough for Cursor or Codex
- isolated
- understandable without hidden context
- testable

Each task should have:

- a title
- a clear expected output
- acceptance criteria or a done condition

### 6. Separate roles clearly

Use three buckets when helpful:

- best tasks for the user
- best tasks for Cursor chat
- best tasks for Codex

General rule:

- the user owns product decisions and semantic choices
- Cursor chat owns wiring, refactors, DTOs, routes, cleanup
- Codex owns deeper implementation blocks, deterministic logic, and test-heavy refactors

## Sprint Writing Rules

- Keep the sprint doc concrete and operational
- Optimize for the next implementation move, not broad strategy
- Make it possible to answer "where are we?" from the sprint doc alone
- Explicitly state what is out of scope
- Prefer “definition of done” over vague ambition
- Name the likely file targets when known
- Use the current repo structure rather than proposing a foreign planning system
- If architecture changes are suggested, label them as recommended refactors

## Scope Heuristics

### Good sprint scope

- deterministic simulation foundation
- event and invention persistence
- simulation snapshot read model
- UI integration of real backend state

### Bad sprint scope

- “finish the simulation”
- “make the app production-ready”
- “build all AI features”

## Example Naming Decisions

Roadmap theme:

- Deterministic simulation core

Sprint file:

```text
docs/sprints/sprint-01-foundation.md
```

Roadmap theme:

- Historical events and inventions ledger

Sprint file:

```text
docs/sprints/sprint-02-history-ledger.md
```

Roadmap theme:

- Simulation read model and API surface

Sprint file:

```text
docs/sprints/sprint-03-read-model.md
```

## Example Delegable Task Format

```markdown
### Task 3

**Title:** Refactor simulation service to pure deterministic `step()`

**Expected output:**

- extracted stepping logic
- deterministic update ordering
- no uncontrolled randomness in the core flow

**Acceptance criteria:**

- one-step execution works
- repeated execution with same seed matches previous run
```

## Final Check

Before finishing a sprint file, verify:

- the file name follows `sprint-XX-theme.md`
- the sprint maps to a real roadmap slice
- scope is narrow enough to execute
- tasks are delegable and testable
- out-of-scope items are explicit
- “definition of done” is concrete
