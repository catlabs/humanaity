---
name: spec-update
description: Sync `docs/specs/` when implementation changes domain invariants, contracts, read models, or deterministic behavior.
---

# Spec Update

## Use when
- behavior/contracts changed in code
- deterministic or domain invariants need to be clarified
- concept docs need a linked source-of-truth update

## Workflow
1. Identify the relevant spec file in `docs/specs/`.
2. Update only the affected invariant/contract sections.
3. Keep references aligned with active docs structure (`docs/concepts/`, `docs/archive/`).
4. Avoid broad rewrites when only one rule changed.

## Guardrails
- Specs are semantic anchors; avoid roadmap/planning language.
- Keep examples and acceptance semantics deterministic and testable.
- If a spec change materially changes the active execution order, sync `docs/milestones.md` separately instead of adding planning detail to the spec.
