---
name: concept-update
description: Create or update concept-first docs in `docs/concepts/` with short, human-readable structure and links to deeper specs/archive sources.
---

# Concept Update

## Use when
- user asks to document a feature/system block
- docs should be simplified or consolidated
- sprint-era content should be represented as concept docs

## Workflow
1. Identify the concept block (e.g. `simulation-engine`, `commands`, `human-goals`).
2. Update or create one short file in `docs/concepts/`.
3. Keep sections minimal: `Scope`, `Covers`, `Source docs`.
4. Prefer short names and scanable bullets.
5. Link to `docs/specs/` for invariants and `docs/archive/` for legacy context.

## Guardrails
- Do not recreate sprint planning inside concept docs.
- Keep concept docs as current-state documentation, not task backlogs.
- If semantics are ambiguous, update a spec instead of overloading concept docs.
