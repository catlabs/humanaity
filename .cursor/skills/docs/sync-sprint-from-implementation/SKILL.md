---
name: sync-sprint-from-implementation
description: Reconcile an active sprint document with recent implementation changes and decisions in the Humanaity repo. Use when the user asks to sync a sprint, update sprint scope after coding, reflect implementation decisions in `docs/sprints/`, or adapt a sprint plan to the current codebase.
---

# Sync Sprint From Implementation

## Goal

Keep an existing sprint document accurate after implementation work or product/technical decisions change.

This includes keeping a lightweight execution status block current so the user can see the active chunk without asking again.

## Related durable rule

- Pair this skill with `.cursor/rules/sprint-doc-sync.mdc` for the stable sprint-update policy that should remain visible outside the skill layer.

## Use This Skill When

- the user asks to sync or update a sprint doc
- implementation changed the plan materially
- scope, deliverables, or definition of done need reconciliation
- technical decisions made during coding should be reflected in `docs/sprints/`

## Inputs To Gather

Determine:

1. which sprint file is active
2. what code or docs changed recently
3. what decisions were made explicitly in chat
4. whether the sprint should reflect reality only, or also include recommended scope corrections

Infer from context when possible. Ask only if the active sprint or intended update mode is unclear.

## Source Of Truth

Read, in this order when relevant:

1. the active sprint file in `docs/sprints/`
2. `docs/roadmap.md`
3. `docs/README.md`
4. recent implementation evidence:
   - current git diff/status when appropriate
   - touched code files
   - related docs
   - explicit user decisions from the conversation

Do not invent progress or decisions that are not supported by code or user direction.

## What Counts As A Sprint-Shaping Change

Update the sprint doc when implementation changes any of these:

- sprint scope
- in-scope or out-of-scope boundaries
- product or technical decisions
- deliverables
- definition of done
- task breakdown or implementation order
- risks
- handoff to the next sprint

Usually do not update the sprint doc for:

- minor renames
- local refactors that keep the same plan
- low-level class or method organization changes

## Sync Workflow

### 1. Read the existing sprint before editing

Identify the stable anchors:

- execution status
- sprint intent
- sprint outcome
- scope
- product and technical decisions
- definition of done

### 2. Compare plan vs reality

Classify findings into:

- still valid
- implemented
- partially implemented
- no longer accurate
- newly decided during implementation
- current active chunk / next chunk changes

### 3. Edit only the sections that changed

Prefer targeted updates over a full rewrite.

Typical sections to update:

- `## Execution status`
- `## Sprint scope`
- `## Product and technical decisions for this sprint`
- `## Deliverables`
- `## Definition of done`
- `## Suggested file targets`
- `## Features and task breakdown`
- `## Recommended implementation order`
- `## Risks`
- `## Handoff to next sprint`

### 4. Preserve planning quality

- keep the sprint concrete and executable
- keep roadmap alignment unless the user wants a change
- state deferred work explicitly
- keep tasks delegable and testable
- distinguish implemented facts from remaining work
- make the current position in the sprint obvious from a quick scan

### 5. Report the delta

After editing, summarize:

- what changed in the sprint doc
- why it changed
- whether the sprint scope stayed the same or drifted
- any open decisions still needing the user

## Editing Rules

- Treat the sprint doc as a living plan, not a status diary.
- Keep status updates concise: active, next, blocked, and done are enough.
- Do not mark work complete unless there is evidence in code or explicit user confirmation.
- Do not silently expand sprint scope.
- If implementation conflicts with the roadmap, reflect reality in the sprint doc but call out the conflict explicitly.
- If the code suggests a better structure, present it as a recommended change rather than a hidden rewrite.

## Output Expectations

When syncing a sprint doc, aim to leave it:

- accurate
- concise
- still useful for delegation
- explicit about what changed during implementation
