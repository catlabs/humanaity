# Docs migration plan

## Proposed structure

```
docs/
├── concepts/
│   ├── simulation-engine.md
│   ├── commands.md
│   ├── human-goals.md
│   ├── tech-tree.md
│   ├── ui-simulation.md
│   ├── mcp.md
│   └── ci.md
├── specs/
├── roadmap.md
├── docs-migration-plan.md
└── archive/
    ├── README.md
    ├── roadmap-legacy.md
    ├── sprint-execution-contract.md
    └── sprints/
```

## Archived
- `docs/sprints/**` → `docs/archive/sprints/**`
- `docs/roadmap.md` (legacy sprint-heavy roadmap) → `docs/archive/roadmap-legacy.md`
- `docs/sprint-execution-contract.md` → `docs/archive/sprint-execution-contract.md`

## Merged or renamed
- Replaced sprint-first navigation with concept-first navigation in:
  - `docs/README.md`
  - root `README.md`
- Added concept docs as lightweight entry points that consolidate references from multiple sprint docs.

## Rollout steps
1. Use `docs/concepts/*` as the default place for active documentation updates.
2. Keep `docs/specs/*` for low-level rules/contracts.
3. Treat `docs/archive/*` as historical, mostly read-only.
4. When new work lands, update one concept file + one spec only if needed.
