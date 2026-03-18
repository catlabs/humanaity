# Simulation engine

## Scope
Core deterministic simulation lifecycle, world-state progression, event emission, and command execution.

## Covers
- run lifecycle (create/start/stop/step)
- deterministic stepping guarantees
- simulation snapshot/read-model contract
- event discovery and historical timeline persistence
- backend-owned command execution rather than frontend or AI-owned decisions

## Source docs
- `docs/specs/simulation-deterministic-spec.md`
- `docs/specs/simulation-read-model-spec.md`
- `docs/specs/history-ledger-spec.md`
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `docs/specs/deterministic-command-contract-spec.md`
