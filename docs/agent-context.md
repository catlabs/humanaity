# Agent Context Model

This document defines how HUMANAIty should organize agent-facing context so both Cursor and Codex can work effectively.

## Context layers

### Rules

Use `.cursor/rules/` for durable project policy:

- architecture boundaries
- naming and placement conventions
- coding constraints
- scope guardrails
- document structure expectations

Rules should be concise, stable, and safe to show to external agents like Codex.

### Skills

Use `.cursor/skills/` for procedural workflows:

- how to run local systems
- how to execute a multi-step task
- how to review or hand off work
- tool-specific operating playbooks

Skills are primarily for Cursor and human operators. Do not assume Codex will discover or apply them automatically.

### Docs and specs

Use `docs/` for repo-visible product and domain context:

- `docs/concepts/` for active system-level documentation
- `docs/milestones.md` for the active delivery order
- `docs/dev-log.md` for dated execution notes
- `docs/specs/` for locked domain semantics and invariants
- `docs/archive/` for historical sprint and legacy planning context

If a constraint must be understood by Codex, it should live in `docs/` or `.cursor/rules/`, not only in a skill.

### Prompt packs

Use prompt packs when delegating bounded work to Codex or Cursor. A prompt pack should point to the relevant milestone/spec/rule files instead of trying to restate the whole project every time.

For active work, prefer milestone/spec/rule context over archived sprint docs unless the task is explicitly historical.

## Duplication policy

Do not create 1:1 rule and skill duplicates by default.

Mirror a skill into a rule only when the skill contains durable guidance that changes:

- code shape
- file placement
- architecture boundaries
- required validation
- scope boundaries
- output document structure

Keep step-by-step execution details in skills.

## Naming and organization conventions

### Rules

Prefer flat filenames with a domain prefix:

- `agent-*` for cross-cutting agent behavior
- `backend-*` for backend engineering constraints
- `ui-*` for UI engineering constraints
- `docs-*` for documentation structure and workflow constraints

This keeps the folder simple while still making rule purpose obvious.

### Skills

Group skills by workflow domain:

- `.cursor/skills/backend/`
- `.cursor/skills/ui/`
- `.cursor/skills/docs/`

Keep a skill at the top level only when it is truly cross-cutting, such as `commit-message`.

## Current pairing recommendations

- `docs/concept-update` -> pair with `docs-*` rules for concise concept-level updates
- `docs/spec-update` -> pair with domain-specific rules when invariants or schemas change
- `docs/milestone-execution` -> use for milestone slicing, docs update cadence, and Codex handoff packets
- `docs/archive-maintenance` -> use only for historical corrections
- `ui/feature-scaffold` -> pair with `ui-feature-architecture.mdc`
- `ui/openapi-regenerate-adapt` -> pair with `ui-api-generated-client.mdc` and `agent-openapi-contract-sync.mdc` (backend contract changes must also refresh `apps/mcp` generated types)
- `ui/shared-ui-component-authoring` -> pair with `ui-shared-ui-boundaries.mdc`
- `ui/modernize-legacy-angular` -> rely on the existing `ui-*` rule set rather than a single duplicate rule
- `backend/run-backend` -> no rule needed
- `ui/run-frontend` -> no rule needed
- `commit-message` -> keep as a skill unless commit automation via external agents becomes a recurring workflow

## API contract drift (UI + MCP)

Backend DTO/OpenAPI changes are not done until **both** consumers are regenerated: the Angular client under `apps/ui/src/app/api/` and MCP’s `apps/mcp/src/generated/api-types.ts`. CI enforces MCP drift via `npm run api:generate:check` in `apps/mcp`. See `.cursor/rules/agent-openapi-contract-sync.mdc`.

## Codex context contract

For Codex tasks, always provide:

1. `docs/agent-context.md`
2. `docs/milestones.md`
3. the relevant concept doc
4. the relevant spec doc (if invariants/contracts are involved)
5. the relevant rule files
6. exact target files
7. explicit out-of-scope items

If the task changes delivery status or task order, also require an update to `docs/milestones.md`.

If the task completes a meaningful implementation slice, also require an appended entry in `docs/dev-log.md`.

Do not rely on Codex discovering project skills.

When a skill matters, pass the skill path explicitly and summarize the workflow in one or two bullets.

## Active work read order

For milestone-oriented implementation, agents should read in this order:

1. `docs/agent-context.md`
2. `docs/milestones.md`
3. the relevant `docs/concepts/*.md`
4. the relevant `docs/specs/*.md`
5. the relevant `.cursor/rules/*.mdc`
6. the relevant `.cursor/skills/*/SKILL.md`

## Docs update cadence

- update `docs/milestones.md` after each completed task or meaningful milestone slice
- append `docs/dev-log.md` after each meaningful slice or workday
- update `docs/concepts/*.md` when active architecture or behavior framing changes
- update `docs/specs/*.md` when contracts, DTOs, invariants, or deterministic rules change
- update `docs/archive/*` only for historical correction or traceability

## Codex handoff template

Use a context packet like this when working with Codex plugin or Codex online:

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
- .cursor/rules/agent-openapi-contract-sync.mdc (when changing backend API/DTOs)
- <other relevant rules>

Skills to follow:
- .cursor/skills/docs/milestone-execution/SKILL.md — read order, work split, docs update cadence, Codex handoff
- <other relevant skill path> — <why it matters>

Docs to update before finishing:
- docs/milestones.md
- docs/dev-log.md
- <relevant concept/spec docs>
```

## Authoring test for new context

Before adding a new rule or skill, ask:

1. Is this durable project policy? If yes, create or update a rule.
2. Is this a procedural workflow? If yes, create or update a skill.
3. Does Codex need to know it without human interpretation? If yes, put it in a rule or `docs/`.
4. Is it both policy and workflow? Split it:
   - concise durable part in a rule
   - execution playbook in a skill
