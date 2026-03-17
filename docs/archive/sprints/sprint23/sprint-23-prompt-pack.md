# Sprint 23 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 23 task only.

References for every task:

- `docs/roadmap.md` (Epics 16-19)
- `docs/specs/chat-goals-tech-tree-spec.md`
- `docs/sprints/sprint23/sprint-23-tech-tree.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 23:

- knowledge graph is config-backed and deterministic
- no LLM inside unlock evaluation
- applications unlock state, but do not yet execute new action families

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: define and load a stable `tech-tree.json` for discoveries, inventions, and applications.

Acceptance criteria:

- Add a schema or validator for `tech-tree.json`.
- Each node declares stable id, node type, prerequisite ids, and any minimal metadata needed for progression.
- Invalid config fails clearly during startup or loading.
- Seed a small initial tree sufficient to exercise discovery -> invention -> application progression.

In scope: config file, schema/validator, loader, tests.

Out of scope: unlock evaluation, simulation actions.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: evaluate discovery, invention, and application unlocks deterministically from canonical simulation state.

Acceptance criteria:

- Unlock rules derive from persisted facts and prerequisite ids, not from LLM output or frontend inference.
- Discoveries unlock inventions; inventions unlock applications.
- Application unlock state is stored or derived in a way that later action scheduling can consume.
- Add deterministic tests that assert stable unlock results for the same seed and history.

In scope: backend knowledge evaluation, persistence/derivation, tests.

Out of scope: new action behaviors, UI redesign.

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal: expose knowledge progression state through backend read models and MCP parity surfaces.

Acceptance criteria:

- Snapshot, overview, timeline, or dedicated read endpoints include enough data to show unlocked discoveries, inventions, and applications.
- Generated clients and MCP tools are updated where required.
- Consumers do not need to reconstruct prerequisite logic themselves.

In scope: API/read-model work, MCP parity, generated client updates.

Out of scope: action execution.
