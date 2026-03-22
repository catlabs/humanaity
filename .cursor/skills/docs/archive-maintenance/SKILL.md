---
name: archive-maintenance
description: Maintain historical docs in `docs/archive/` for traceability without turning archive files into active planning documents.
---

# Archive Maintenance

## Use when
- links to archived files need correction
- historical docs need factual fixes
- migration steps require archive indexing updates

## Workflow
1. Treat `docs/archive/` as mostly read-only.
2. Apply only factual/path corrections or migration metadata updates.
3. Prefer factual fixes only; for current behavior, rely on code (see `agent-code-source-of-truth.mdc`).

## Guardrails
- Do not restart sprint planning inside archive docs.
- Keep archive changes minimal and traceability-focused.
