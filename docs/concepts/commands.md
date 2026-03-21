# Commands

## Scope
Deterministic city-scoped command handling for the primary simulation control loop.

## Covers
- explicit fail-closed command grammar for simulation control
- backend-owned command execution and UI-friendly response effects
- a structured command/query builder UX that stays separate from AI narration
- `GET /api/simulations/{cityId}/command-builder` as backend-owned metadata for action/actor/target selection
- `POST /api/simulations/{cityId}/commands` as the primary command surface for the main simulation page
- legacy agent orchestration retained only as secondary or historical context while deterministic commands become the forward path

## Source docs
- `docs/specs/deterministic-command-contract-spec.md`
- `docs/specs/agent-chat-orchestration-spec.md` (historical reference)
