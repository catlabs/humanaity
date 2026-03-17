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
- `docs/specs/` for locked domain semantics and invariants
- `docs/archive/` for historical sprint and legacy planning context

If a constraint must be understood by Codex, it should live in `docs/` or `.cursor/rules/`, not only in a skill.

### Prompt packs

Use prompt packs when delegating bounded work to Codex or Cursor. A prompt pack should point to the relevant sprint/spec/rule files instead of trying to restate the whole project every time.

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
- `docs/archive-maintenance` -> use only for historical corrections
- `ui/feature-scaffold` -> pair with `ui-feature-architecture.mdc`
- `ui/openapi-regenerate-adapt` -> pair with `ui-api-generated-client.mdc`
- `ui/shared-ui-component-authoring` -> pair with `ui-shared-ui-boundaries.mdc`
- `ui/modernize-legacy-angular` -> rely on the existing `ui-*` rule set rather than a single duplicate rule
- `backend/run-backend` -> no rule needed
- `ui/run-frontend` -> no rule needed
- `commit-message` -> keep as a skill unless commit automation via external agents becomes a recurring workflow

## Codex context contract

For Codex tasks, always provide:

1. the relevant concept doc
2. the relevant spec doc (if invariants/contracts are involved)
3. the relevant rule files
4. exact target files
5. explicit out-of-scope items

Do not rely on Codex discovering project skills.

## Authoring test for new context

Before adding a new rule or skill, ask:

1. Is this durable project policy? If yes, create or update a rule.
2. Is this a procedural workflow? If yes, create or update a skill.
3. Does Codex need to know it without human interpretation? If yes, put it in a rule or `docs/`.
4. Is it both policy and workflow? Split it:
   - concise durable part in a rule
   - execution playbook in a skill
