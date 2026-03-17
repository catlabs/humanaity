# Deterministic Simulation Step Spec (Sprint 1)

## Status

- Status: LOCKED for Sprint 1 implementation
- Scope anchor: `docs/archive/sprints/sprint-01-foundation.md` (Feature 1 and Sprint Definition of Done)
- Applies to: backend simulation core only

## Purpose

This document fixes the simulation semantics that implementation must follow before further refactors begin.  
If code behavior conflicts with this spec during Sprint 1, this spec is the source of truth.

## Determinism Contract

The simulation is deterministic when all inputs below are identical:

- same city starting state
- same simulation run seed
- same ordered sequence of step executions

Then all canonical outputs after N steps must be identical.

## Canonical State For Sprint 1

### Run-owned state

- `cityId`
- `seed` (immutable once run is created)
- `tick` (starts at `0`, increments by `1` per successful step)
- `status` (`CREATED`, `RUNNING`, `PAUSED`, `COMPLETED`)
- timestamps (`createdAt`, `updatedAt`)

### Human-owned state (canonical for deterministic checks)

- `id`
- `city.id`
- `x`
- `y`
- `busy`

### City-owned state (relevant to stepping)

- city identity and membership relation to humans

## Tick Semantics (Locked)

One call to `step(cityId)` means exactly one deterministic tick for that city.

For each tick:

1. Load humans for the city.
2. Establish processing order as **stable ascending `Human.id`**.
3. For each human in that order:
   - if `busy == true`, do not move this human in this tick.
   - if `busy == false`, compute collision outcome against the current tick view using deterministic order.
   - on collision, set `busy = true` for both humans involved.
   - if no collision, apply deterministic movement deltas to `x` and `y`.
4. Clamp movement to `[0.0, 1.0]` bounds for both axes.
5. Persist all changed humans.
6. Increment run `tick` by `1`.

## Randomness Policy (Locked)

Allowed randomness for Sprint 1:

- only seeded pseudo-random generation derived from the run seed
- no `new Random()` without explicit seed
- no `Collections.shuffle(...)` for business decisions

Deterministic RNG requirements:

- same run seed + same initial state + same tick count => same random draws at each decision point
- draw count per human decision path must remain deterministic (no hidden/conditional extra draws that depend on non-canonical iteration order)

## Ordering Policy (Locked)

- Repository/default database order is **not trusted** for simulation correctness.
- Application layer must enforce deterministic ordering explicitly (ascending `Human.id`).
- Any subset selection in Sprint 1 must also be deterministic and derived from that same stable ordering.

## Stable Outputs For Reproducibility Tests

After any fixed number of steps, tests must compare at least:

- run `tick`
- ordered list of humans by `id`
- each human's canonical deterministic fields: `x`, `y`, `busy`

## Deferred (Explicitly Not In Sprint 1 Tick Contract)

- events timeline semantics
- inventions emergence semantics
- AI-generated narrative/content side effects
- long-term human behavior models beyond current movement/collision MVP
- scheduler frequency tuning or performance optimization

## Acceptance Criteria For Task `spec-first`

This task is complete only if all statements are true:

- a developer can implement deterministic `step()` behavior from this doc without guessing ordering or RNG rules
- deterministic versus deferred behavior is explicitly separated
- canonical reproducibility outputs are listed for tests
- this spec remains consistent with Sprint 1 Definition of Done in `docs/archive/sprints/sprint-01-foundation.md`

## Implementation Guardrails

- use deterministic fixtures in tests; avoid AI/Faker/randomized generation in deterministic test setup
- keep scheduler/runtime concerns as wrapper behavior, not simulation source of truth
- avoid adding new simulation domains in Sprint 1 beyond deterministic foundation scope
