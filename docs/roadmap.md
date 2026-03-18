# Documentation roadmap (simplified)

This repo now documents the system by **concept blocks** plus lightweight execution docs.

`docs/roadmap.md` is navigation-only. It is not a feature status tracker.

## Primary navigation
- `docs/concepts/` — current, human-readable documentation by system block
- `docs/specs/` — locked technical specs and domain contracts
- `docs/milestones.md` — active milestone tracker for current delivery work
- `docs/dev-log.md` — rolling dated implementation log
- `docs/archive/` — historical sprint-by-sprint planning/execution material

## Current concept blocks
- product-architecture
- simulation-engine
- commands
- ai-narration
- human-goals
- tech-tree
- ui-simulation
- mcp
- ci
- testing-strategy

## Maintenance rule
When a feature changes, update the matching file in `docs/concepts/` first.
Update `docs/specs/` when contracts or invariants change.
Update `docs/milestones.md` for active delivery order and `docs/dev-log.md` for meaningful execution slices.
Only update archived files for historical correction.
