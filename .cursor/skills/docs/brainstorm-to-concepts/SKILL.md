---
name: brainstorm-to-concepts
description: Turn brainstorm input into a lightweight concept-first docs plan and concrete updates in `docs/concepts/`, with spec and archive links when needed.
---

# Brainstorm to Concepts

## Goal
Convert ideas into maintainable concept docs instead of sprint-heavy plans.

## Workflow
1. Cluster brainstorm items by system block.
2. Map each block to an existing or new `docs/concepts/*.md` file.
3. Put active execution order in `docs/milestones.md`, not in concept docs.
4. Update `docs/dev-log.md` only for dated execution notes, not planning.
5. Update `docs/roadmap.md` only for docs-navigation changes.
6. Update specs only where semantics/contracts changed.
7. Add archive references only for historical traceability.

## Output shape
- proposed concept structure
- milestone and logging file changes when execution planning is requested
- files to merge/rename/archive
- simple migration steps
- optional direct implementation when requested

## Guardrails
- Prefer short file names.
- Keep docs easy to scan.
- Do not over-engineer folder depth.
