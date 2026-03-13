# Simulation Read-Model Spec (Sprint 3)

## Status

- Status: LOCKED for Sprint 3 planning
- Scope anchor: `docs/sprints/sprint03/sprint-03-simulation-read-model.md` (Feature 1 and Sprint Definition of Done)
- Applies to: backend-owned overview and snapshot contracts for UI and MCP consumers

## Purpose

This document fixes the read-model semantics that Sprint 3 implementation must follow before endpoint and client wiring begins.

If client code or controller behavior conflicts with this spec during Sprint 3, this spec is the source of truth.

## Contract Goal

Sprint 3 introduces two backend-owned product surfaces:

- a city overview contract for list pages and MCP summaries
- a unified simulation snapshot contract for city detail surfaces

These contracts exist to stop UI and MCP consumers from synthesizing product-critical state from raw endpoints or placeholder logic.

## Deterministic Contract Rules

All read-model fields in Sprint 3 must be derived only from deterministic backend state already persisted or computed from stable ordered inputs.

Allowed derived inputs:

- city identity and ownership
- simulation run state (`seed`, `tick`, `status`, timestamps)
- ordered humans for a city
- ordered deterministic events
- ordered deterministic inventions

Not allowed:

- random client-side status labels
- wall-clock-derived simulation year or era
- frontend-only invention or population approximations

## Contract 1: City Overview

The city overview contract is the minimum list-row payload needed by UI and MCP for product-facing simulation summaries.

### Required fields

- `cityId`
- `cityName`
- `hasRun`
- `runStatus`
- `running`
- `tick`
- `year`
- `era`
- `population`
- `inventionCount`
- `eventCount`
- `updatedAt`

### Field semantics

- `cityId` and `cityName` come from the city aggregate.
- `hasRun` is `true` only when a persisted `SimulationRun` exists for the city.
- `runStatus` mirrors persisted run status when a run exists; otherwise it is `null`.
- `running` mirrors runtime wrapper state and is `false` when no run exists.
- `tick` is the persisted simulation tick when a run exists; otherwise `0`.
- `year` is deterministically derived from tick using the Sprint 2 history mapping rules.
- `era` is deterministically derived from the same year mapping used by events/inventions.
- `population` is the current human count for the city.
- `inventionCount` is the persisted invention count for the city.
- `eventCount` is the persisted event count for the city.
- `updatedAt` is the most relevant backend timestamp for row freshness:
  - use run `updatedAt` when a run exists
  - otherwise use city `updatedAt` or `createdAt` if available by current domain model

### Empty-state behavior

For a city with no simulation run yet:

- return a valid overview row
- set `hasRun = false`
- set `runStatus = null`
- set `running = false`
- set `tick = 0`
- set `year` and `era` from tick `0` using the deterministic mapping
- keep counts at real persisted values (`population`, `eventCount`, `inventionCount`)

## Contract 2: Unified Simulation Snapshot

The simulation snapshot contract is the canonical detail-surface payload for a single city.

### Required sections

- `city`
- `run`
- `timelineSummary`
- `humans`
- `metrics`
- `recentEvents`
- `recentInventions`

### City section

Minimum fields:

- `id`
- `name`

### Run section

Minimum fields:

- `hasRun`
- `runId`
- `seed`
- `status`
- `running`
- `tick`
- `year`
- `era`
- `createdAt`
- `updatedAt`

### Humans section

The snapshot should include the current city humans needed by the existing detail surface.

Minimum fields per human:

- `id`
- `name` if available in the canonical API/domain
- `x`
- `y`
- `busy`

Ordering rule:

- humans must be returned in stable ascending `id` order

### Metrics section

The snapshot metrics are backend-owned deterministic aggregates for current state presentation.

Minimum fields:

- `population`
- `busyCount`
- `busyRatio`
- `centroid`
- `bounds`
- `eventCount`
- `inventionCount`

Metric rules:

- `population` equals the size of the humans list
- `busyCount` counts humans where `busy == true`
- `busyRatio` is `busyCount / population`, or `0` if population is `0`
- `centroid` and `bounds` use only humans with finite coordinates
- `eventCount` and `inventionCount` come from persisted history ledgers, not client-side estimates

### Timeline summary section

The timeline summary is a lightweight rollup for current detail-page summary fields.

Minimum fields:

- `latestEventTick`
- `latestInventionTick`
- `recentEventCount`
- `recentInventionCount`

The exact recent-window size may be implementation-defined for Sprint 3, but it must be:

- documented in endpoint behavior if exposed as a fixed window
- deterministic
- shared by UI and MCP through the backend response instead of recomputed client-side

### Recent events and recent inventions sections

These sections provide the current detail surface with a bounded, already-ordered summary feed.

Rules:

- events are ordered by tick ascending, then sequence-in-tick ascending
- inventions are ordered by tick created ascending, then invention key ascending
- if a limit is applied, ordering semantics must remain explicit and stable

## Year and Era Mapping

Sprint 3 reuses the deterministic year/era mapping defined for Sprint 2 history output.

Rules:

- overview and snapshot year/era must be computed from the same mapping used by event/invention history metadata
- UI and MCP must not maintain alternate year/era formulas for the same city state

## Endpoint Semantics

Sprint 3 should expose:

- one city overview read surface for list-style consumers
- one simulation snapshot read surface for detail-style consumers

Naming may follow current simulation controller conventions, but behavior must satisfy this spec.

Error/empty-state expectations:

- `404` when the city itself does not exist
- `200` with explicit empty-state fields when the city exists but has no run/history yet

## Canonical Fields For Contract Tests

Backend/API tests in Sprint 3 must verify at least:

- overview `cityId`, `hasRun`, `runStatus`, `running`, `tick`, `year`, `era`, `population`, `inventionCount`, `eventCount`
- snapshot `run.tick`, `run.year`, `run.era`, ordered `humans`, `metrics.population`, `metrics.eventCount`, `metrics.inventionCount`
- empty-state semantics for cities with no run/history
- stable ordering of humans/events/inventions inside the snapshot

## Deferred (Explicitly Not In Sprint 3 Contract)

- full frontend redesign
- timeline panel design decisions beyond summary/read-model support
- AI-authored summaries, dialogues, or narrative cards
- streaming/live subscription protocols
- cross-city dashboards or analytics views
- deep historical analytics beyond the MVP summary fields above

## Implementation Guardrails

- backend read models should be assembled in one clear projection/query path
- avoid duplicating product-facing metric logic in `apps/ui` and `apps/mcp`
- prefer generated OpenAPI clients once the endpoints exist; remove fallback direct HTTP calls where Sprint 3 contracts cover the need
- preserve current page structure in UI work unless a minimal binding change is required by the real contract
