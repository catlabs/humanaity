# History Ledger MVP Spec (Sprint 2)

## Status

- Status: LOCKED for Sprint 2 Task `1a`
- Scope anchor: `docs/sprints/sprint02/sprint-02-history-ledger.md`
- Applies to: backend event, invention, and timeline semantics for the Sprint 2 MVP

## Purpose

This document fixes the minimum history-ledger semantics that Sprint 2 implementation must follow.

If implementation behavior conflicts with this spec during Sprint 2, this spec is the source of truth for:

- MVP event taxonomy
- invention emergence behavior
- deterministic year and era mapping
- event emission points
- stable fields used for same-seed reproducibility checks

## Determinism Contract

History output is deterministic when all inputs below are identical:

- same city starting state
- same simulation run seed
- same ordered sequence of lifecycle commands and step executions

Then all canonical history outputs after N commands and steps must be identical:

- ordered event sequence
- ordered invention sequence
- year and era metadata derived from ticks
- deterministic event and invention payload fields

The following are **not** canonical for same-seed reproducibility:

- database-generated IDs
- persistence timestamps such as `createdAt` and `updatedAt`
- any later AI-enriched text

## Canonical History Model For Sprint 2

### Event fields

Every persisted event in Sprint 2 should support at least these fields:

- `cityId`
- `tick`
- `sequenceInTick`
- `eventCategory`
- `eventType`
- ordered `actorIds`
- deterministic `payload`
- `importance`
- `year`
- `era`
- deterministic `eventKey`

Field semantics:

- `tick` is the logical simulation tick for timeline ordering.
- `sequenceInTick` is the stable ordering slot inside a tick.
- `actorIds` must be stored and exposed in ascending ID order.
- `payload` must contain only deterministic backend-owned facts.
- `eventKey` is the stable logical reference used by tests and invention linkage.

Recommended `eventKey` shape:

- `<eventType>:<tick>:<sequenceInTick>`

This key is city-scoped. Persistence may still use a database ID, but the database ID is not the canonical reproducibility identity.

### Invention fields

Every persisted invention in Sprint 2 should support at least these fields:

- `cityId`
- `tickCreated`
- `category`
- deterministic `inventionKey`
- deterministic `title`
- deterministic `summary`
- ordered `sourceEventKeys`
- `impactScore`
- `yearCreated`
- `eraCreated`

Field semantics:

- `inventionKey` is the stable logical identity for same-seed comparison.
- `title` and `summary` are deterministic backend-owned strings in Sprint 2.
- `sourceEventKeys` must be ordered by source event `(tick, sequenceInTick)`.

## Event Taxonomy (Locked)

Sprint 2 locks the MVP event taxonomy to five categories.

### `LIFECYCLE`

Run and city simulation state changes.

Locked event types:

- `SIMULATION_STARTED`
- `SIMULATION_PAUSED`
- `SIMULATION_RESUMED`
- `SIMULATION_COMPLETED`

### `INTERACTION`

Deterministic human-to-human contact or encounter outcomes produced by stepping logic.

Locked event types:

- `HUMANS_COLLIDED`

### `DISCOVERY`

Deterministic knowledge or capability unlocks produced by backend logic.

Locked event types:

- `DISCOVERY_UNLOCKED`

### `DIALOGUE`

Deterministic dialogue facts if the backend produces structured dialogue artifacts.

Locked event types:

- `DIALOGUE_EXCHANGED`

### `MILESTONE`

City-level historical landmarks derived from already-deterministic source facts.

Locked event types:

- `INVENTION_EMERGED`

## Event Emission Points (Locked)

### Lifecycle seams

Emit lifecycle events at command boundaries using the run's current persisted tick:

1. `start(cityId)` emits `SIMULATION_STARTED` at the current tick.
2. `pause(cityId)` emits `SIMULATION_PAUSED` at the current tick.
3. `resume(cityId)` emits `SIMULATION_RESUMED` at the current tick.
4. a terminal stop/complete transition emits `SIMULATION_COMPLETED` at the current tick.

### Step seam

Step-derived history events are emitted after deterministic tick resolution for that step is complete.

Locked rule:

- the first successful call to `step(cityId)` produces step-derived events at `tick = 1`
- in general, step-derived events use the post-step tick value

### Step-derived event sources

Sprint 2 should treat these seams as valid event emission sources:

1. deterministic human collision or encounter resolution -> `HUMANS_COLLIDED`
2. deterministic discovery unlock logic -> `DISCOVERY_UNLOCKED`
3. deterministic structured dialogue generation -> `DIALOGUE_EXCHANGED`
4. invention creation from deterministic source events -> `INVENTION_EMERGED`

No event should be emitted from:

- wall-clock scheduler timing
- unsorted repository iteration
- AI-generated text
- frontend-only behavior

## Ordering Policy (Locked)

Database default ordering is not trusted for history correctness.

Events must be ordered first by `tick`, then by `sequenceInTick`.

Inside one tick, assign `sequenceInTick` only after applying this precedence:

1. `LIFECYCLE`
2. `INTERACTION`
3. `DISCOVERY`
4. `DIALOGUE`
5. `MILESTONE`

Within the same category and tick, use a deterministic tie-breaker:

1. ascending ordered `actorIds`
2. stable payload discriminator such as `discoveryKey`, `dialogueKey`, or `inventionKey`
3. final deterministic insertion order from already-sorted upstream inputs

## Year And Era Mapping (Locked)

Sprint 2 uses a simple logical calendar. It is for stable timeline grouping, not historical realism.

### Tick-to-year rule

- `ticksPerYear = 10`
- `year = floor(tick / 10) + 1`

Examples:

- `tick = 0` -> `year = 1`
- `tick = 1` -> `year = 1`
- `tick = 9` -> `year = 1`
- `tick = 10` -> `year = 2`

### Year-to-era rule

Era is derived from `year` only:

- `FOUNDING` for years `1-25`
- `EXPANSION` for years `26-50`
- `CONSOLIDATION` for years `51-75`
- `LEGACY` for years `76+`

Every event and invention must carry the year and era derived from its tick.

## Invention Emergence Rules (Locked)

Sprint 2 invention emergence is intentionally simple.

### MVP source rule

Only `DISCOVERY_UNLOCKED` events may create inventions in the Sprint 2 MVP.

Required deterministic discovery payload fields:

- `discoveryKey`
- `inventionKey`
- `inventionCategory`
- deterministic `title`
- deterministic `summary`
- `impactScore`

### MVP emergence rule

Create exactly one invention for the first `DISCOVERY_UNLOCKED` event with a given `(cityId, inventionKey)`.

Locked behavior:

1. the earliest qualifying discovery event creates the invention
2. the invention stores that event's `eventKey` as the first and only `sourceEventKey` in Sprint 2 MVP
3. later discovery events with the same `inventionKey` must not create duplicates
4. invention creation should also emit one `INVENTION_EMERGED` milestone event in the same tick

### Category rule

Sprint 2 invention categories are locked to:

- `TECHNIQUE`
- `SOCIAL_PRACTICE`
- `KNOWLEDGE`

The invention category must be copied from deterministic discovery logic, not inferred by AI.

## Stable Fields For Reproducibility Tests

### Events

Same-seed history tests should compare events using ordered logical output, not database IDs.

Minimum event comparison fields:

- `tick`
- `sequenceInTick`
- `year`
- `era`
- `eventCategory`
- `eventType`
- `eventKey`
- ordered `actorIds`
- `importance`
- normalized deterministic `payload`

### Inventions

Minimum invention comparison fields:

- `tickCreated`
- `yearCreated`
- `eraCreated`
- `category`
- `inventionKey`
- deterministic `title`
- deterministic `summary`
- ordered `sourceEventKeys`
- `impactScore`

### Excluded from reproducibility assertions

Do not use these as canonical equality fields:

- database IDs
- `createdAt`
- `updatedAt`
- run-local persistence foreign keys
- later AI-enriched wording

## Deferred (Explicitly Not Locked By Sprint 2 MVP)

- multi-event invention recipes
- calendar months, dates, or real-world chronology semantics
- cross-city or world-level era definitions
- AI-authored canonical event or invention text
- narrative recaps, lore, or historian-style summaries
- complex event taxonomy expansion beyond the locked categories and types above

## Acceptance Criteria For Task `1a`

This task is complete only if all statements are true:

- a developer can implement event persistence, invention persistence, and timeline metadata from this document without guessing semantics
- deterministic behavior and deferred behavior are explicitly separated
- same-seed reproducibility fields are listed without relying on database IDs or wall-clock timestamps
- this spec remains aligned with Sprint 2 scope in `docs/sprints/sprint02/sprint-02-history-ledger.md`

## Implementation Guardrails

- keep event and invention ordering explicit in application logic
- keep canonical history fields deterministic and backend-owned
- allow persistence IDs and timestamps for storage concerns, but do not treat them as logical truth
- do not introduce AI enrichment into canonical history persistence in Sprint 2
