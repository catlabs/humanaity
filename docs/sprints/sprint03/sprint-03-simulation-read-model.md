# Sprint 3: Simulation Read Model

## Execution status

- Current phase: Sprint 3 planned
- Active chunk: none
- Next chunk: `Task 1a` - simulation read-model spec
- Blocked items: none
- Last completed chunk: Sprint 2 closeout (`Task 5b` - timeline ordering/scoping/API contract validation)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | planned | Lock the read-model contract and remove ambiguity around snapshot/overview semantics. |
| Task 2a | planned | Add backend snapshot and city-overview query DTOs/endpoints. |
| Task 2b | planned | Centralize deterministic metrics/read-model assembly behind backend query services. |
| Task 3a | planned | Regenerate OpenAPI clients and align MCP tools with the new read surfaces. |
| Task 3b | planned | Replace frontend fake/synthesized city overview data with backend-owned contracts. |
| Task 3c | planned | Replace simulation detail placeholder metadata with backend snapshot data while preserving current layout. |
| Task 4a | planned | Add backend/API tests for read-model ordering, scoping, and empty-state behavior. |
| Task 4b | planned | Add MCP smoke validation and minimal frontend integration checks for the new contract path. |

## Sprint intent

Sprint 3 exists to turn the deterministic backend core plus history ledger into clean product-facing read models that UI and MCP can consume without inventing data.

This sprint is mixed, but still backend-led.

It does **not** try to redesign the frontend. Its job is to make simulation pages and tools read real state through stable contracts.

## Why this sprint comes next

Sprint 2 made simulation history deterministic and queryable, but product-facing consumers still assemble partial state themselves.

If Sprint 3 is skipped:

- the city list will keep synthesizing fake status/year/invention metadata
- the simulation detail page will keep relying on placeholder state
- MCP and UI will continue deriving snapshots differently
- future frontend work will be forced to build against unstable or duplicated contract logic

## Sprint outcome

At the end of Sprint 3, HUMANAIty should expose backend-owned simulation snapshot and city overview read models, with OpenAPI-aligned UI and MCP consumers using those contracts instead of placeholder or fallback logic.

## Sprint scope

### In scope

- define the MVP read-model contract for simulation snapshot and city overview surfaces
- add backend endpoints/DTOs for unified simulation snapshot and city overview data
- centralize deterministic metrics/read-model assembly in backend query code
- regenerate `apps/ui` and `apps/mcp` clients after contract milestones
- replace frontend fake city-list metadata with backend-owned overview data
- replace simulation-detail placeholder info-bar/history summary data with backend snapshot data while keeping the current page structure
- add automated tests and smoke checks for read-model scoping, ordering, and empty states

### Out of scope

- major frontend redesign or new visual system work
- Pixi canvas feature expansion
- AI-generated summaries or dialogue surfaces
- real-time transport upgrades beyond current polling/runtime model
- production hardening, CI, or deployment work
- broader simulation-rule expansion outside what is needed to project read models

## Product and technical decisions for this sprint

### Global decision: product-facing contracts are backend-owned

UI and MCP should consume backend-owned read models for overview and snapshot surfaces rather than re-deriving product semantics from raw entities.

This includes:

- simulation status
- tick/year/era projection
- population and basic aggregate metrics
- timeline/invention summaries needed by overview/detail surfaces

### Decision 1: keep read models city-scoped and MVP-sized

Sprint 3 should add only the contracts needed to unblock UI and MCP consumers:

- city overview list data
- simulation snapshot detail data

Avoid introducing a generic reporting layer or broad analytics API.

### Decision 2: deterministic facts remain the source of truth

Read models may aggregate and project deterministic data, but they must not introduce non-deterministic derived state.

Avoid:

- wall-clock-derived simulation year
- randomized frontend labels
- client-side invention counts inferred from placeholder rules

### Decision 3: generated clients are part of the feature

Any backend endpoint added or changed in this sprint must ship with regenerated OpenAPI clients for both `apps/ui` and `apps/mcp` in the same execution loop.

Minimum contract sync for endpoint changes:

- regenerate `apps/ui` OpenAPI client
- regenerate `apps/mcp` OpenAPI types (`npm run api:generate` in `apps/mcp`)
- update MCP wrappers and any frontend services that can now stop using fallback HTTP calls
- verify one MCP smoke flow and one UI integration path for the new contract

### Decision 4: frontend integration is thin, not a redesign

Sprint 3 should wire real contracts into existing pages/components and remove obvious placeholder/fake metadata, but keep layout and interaction changes minimal.

### Decision 5: one backend snapshot should be the canonical detail surface

The simulation detail page and MCP snapshot tool should converge on the same backend snapshot contract instead of computing different aggregates in parallel.

### Decision 6: empty states must be explicit contract behavior

Snapshot and overview endpoints should define stable behavior when a city has:

- no simulation run yet
- zero humans
- no events or inventions yet

Clients should not need special-case guesswork for those states.

## Deliverables

By the end of Sprint 3, the repo should contain:

- a written simulation read-model spec for overview/snapshot semantics
- backend DTOs/endpoints for city overview and unified simulation snapshot
- backend query/projection logic for deterministic metrics and summary fields
- regenerated OpenAPI clients for `apps/ui` and `apps/mcp`
- frontend city list/detail pages using backend-owned read data instead of placeholder synthesis
- MCP tooling aligned with the new snapshot/overview contracts
- automated tests and smoke checks proving the new read surfaces are stable

## Definition of done

Sprint 3 is done only if all of the following are true:

- a city overview surface can be queried without frontend-generated fake status/year/invention values
- a unified simulation snapshot can be queried from the backend for a city
- snapshot and overview fields are documented, deterministic, and stable for empty states
- `apps/ui` no longer needs direct `HttpClient` fallback for product-critical simulation read paths introduced in this sprint
- `apps/mcp` can exercise the new read-model contract through tool wrappers
- at least one automated backend/API test covers snapshot or overview contract behavior
- one MCP smoke flow and one frontend integration path validate the new contract end to end

## Suggested file targets

These are the most likely files or folders Sprint 3 will touch:

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/SimulationController.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/`
- `apps/ui/src/app/features/city/city.service.ts`
- `apps/ui/src/app/features/city/pages/list/city-list.page.ts`
- `apps/ui/src/app/features/city/pages/simulation-detail/simulation-detail.component.ts`
- `apps/mcp/src/backend-client.ts`
- `apps/mcp/src/tools/simulation-tools.ts`

Likely new code areas:

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/query/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/SimulationSnapshotOutput.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/CityOverviewOutput.java`
- `docs/specs/simulation-read-model-spec.md`

## Features and task breakdown

## Feature 1: Simulation read-model specification

### Goal

Define exactly what the city overview and simulation snapshot surfaces contain so backend and frontend work do not drift.

### Tasks

1. Define the minimum city overview contract for list pages and MCP summaries.
2. Define the unified simulation snapshot contract for detail surfaces.
3. Define deterministic rules for derived fields such as year, era, counts, and summary metrics.
4. Define explicit empty-state behavior when run/history data is absent.
5. Define which fields are canonical for contract tests and regeneration checks.

Locked Sprint 3 spec artifact:

- `docs/specs/simulation-read-model-spec.md`

### Acceptance criteria

- a developer can read the spec and implement snapshot/overview contracts without guessing field semantics
- the spec clearly states which fields are canonical, derived, and deferred

### Best owner

- You

## Feature 2: Backend read-model endpoints

### Goal

Expose stable backend-owned overview and snapshot surfaces for a city.

### Tasks

1. Add a city overview DTO/projection with status, population, tick/year/era, invention count, and update metadata.
2. Add a unified simulation snapshot DTO/projection with run metadata, humans, metrics, events, and inventions.
3. Add controller endpoints for overview and snapshot reads.
4. Define city-not-found and no-run-yet semantics explicitly.
5. Keep endpoint naming and DTO placement aligned with existing simulation API conventions.

Task 2a boundary note:

- focus first on DTOs, controller surface, and compile-safe query wiring
- deeper projection cleanup and metric centralization happens in Task 2b

### Acceptance criteria

- overview and snapshot data can be queried directly from backend endpoints
- clients do not need to compose product-critical fields from multiple unrelated endpoints for the Sprint 3 surfaces

### Best owner

- Cursor chat or Codex

## Feature 3: Deterministic projection and contract alignment

### Goal

Make overview/snapshot projection logic deterministic and align generated consumers with the new contract.

### Tasks

1. Centralize derived metric computation and summary-field mapping in backend query code.
2. Regenerate `apps/ui` and `apps/mcp` clients after the contract is stable.
3. Update MCP snapshot/overview tools to use backend-owned fields instead of local recomputation where applicable.
4. Remove temporary frontend fallback calls when generated services now support the needed endpoints.

### Acceptance criteria

- snapshot/overview projection logic is defined in one backend-owned path
- UI and MCP consume the regenerated contract rather than maintaining divergent derived logic

### Best owner

- Codex

## Feature 4: Thin frontend adoption

### Goal

Replace obvious placeholder/synthesized simulation metadata with backend-owned read data without redesigning the pages.

### Tasks

1. Wire the city list to backend overview data for status/year/era/invention counts.
2. Remove random or placeholder-derived list metadata.
3. Wire the simulation detail page info bar and history summary areas to backend snapshot data.
4. Preserve current page composition unless a minimal contract-driven adjustment is required.

### Acceptance criteria

- city list rows use backend-owned simulation metadata
- simulation detail no longer depends on placeholder info-bar values for Sprint 3-covered surfaces
- UI changes stay contract-focused and do not expand into a redesign sprint

### Best owner

- Cursor chat

## Feature 5: Contract validation and smoke coverage

### Goal

Make the new read surfaces safe to build on by testing empty states, ordering, and client integration.

### Tasks

1. Add backend/API tests for snapshot and overview contract behavior.
2. Add tests for empty-state handling when no run/history exists.
3. Validate generated client usage in `apps/ui` and `apps/mcp`.
4. Run at least one MCP smoke flow covering overview plus snapshot.

### Acceptance criteria

- regressions in snapshot/overview semantics are easy to detect
- contract drift between backend and consumers is caught before Sprint 4 frontend work

### Best owner

- Codex

## Recommended implementation order

1. Write the simulation read-model specification.
2. Add backend overview and snapshot DTOs/endpoints.
3. Centralize deterministic projection logic and finalize contract semantics.
4. Regenerate OpenAPI clients for UI and MCP.
5. Wire thin frontend adoption for city list and simulation detail.
6. Update MCP tools to the canonical backend snapshot/overview paths.
7. Add backend/API tests and smoke validation.
8. Remove any remaining Sprint 3-scoped placeholder/fallback seams that conflict with the new contracts.

## Chunk-by-chunk ownership split (Cursor vs Codex)

Use this split to execute Sprint 3 in reviewable sub-chunks while preserving the five-feature structure above.

| Chunk ID | Chunk focus | Primary owner | Notes |
| --- | --- | --- | --- |
| Task 1a | Write simulation read-model spec | You | Semantic lock before endpoint and client work starts. |
| Task 2a | Add overview/snapshot DTOs and controller endpoints | Cursor | Repo-fitting API scaffolding first. |
| Task 2b | Centralize deterministic projection and metrics | Codex | Keep query semantics backend-owned and deterministic. |
| Task 3a | Regenerate OpenAPI and align MCP | Cursor | Keep contract consumers current in the same loop. |
| Task 3b | Replace city-list fake metadata | Cursor | Thin UI adoption, no redesign. |
| Task 3c | Replace simulation-detail placeholder summary data | Cursor | Use snapshot contract while preserving structure. |
| Task 4a | Add backend/API contract tests | Codex | Focus on empty states and contract semantics. |
| Task 4b | MCP smoke + frontend integration validation | Cursor | Close the loop before Sprint 4. |

## Dependencies inside the sprint

- Task 1a should complete before Task 2a so DTO semantics do not drift.
- Task 2a should land before Task 3a/3b/3c because client regeneration depends on stable endpoints.
- Task 2b should happen before final contract validation so metrics are not duplicated between backend and clients.
- Task 3a should precede Task 3b and Task 3c if generated client changes are required.
- Task 4b depends on the contract being integrated in both MCP and UI.

## Suggested delegation

### Best tasks for you

- lock the read-model semantics in the spec
- decide any naming or product-facing field wording that affects UI labels
- approve whether empty-state responses should be `200` with null sections or `404`/domain errors for missing runs

### Best tasks for Cursor chat

- add DTO/controller scaffolding
- regenerate OpenAPI clients
- wire Angular services/pages to the new contract
- update MCP wrappers and smoke scripts

### Best tasks for Codex

- implement deterministic projection/query logic
- cleanly consolidate backend-owned derived metrics
- add contract tests covering empty states and ordering semantics

## Ready-to-delegate task list

Use these as small execution handoffs:

1. `Task 1a`: write `docs/specs/simulation-read-model-spec.md` and lock the MVP overview/snapshot contract.
2. `Task 2a`: add backend DTOs and endpoints for city overview plus unified simulation snapshot.
3. `Task 2b`: centralize deterministic projection/metric logic behind backend query code.
4. `Task 3a`: regenerate `apps/ui` and `apps/mcp` contract artifacts and align MCP tools.
5. `Task 3b`: replace `city-list.page.ts` fake metadata with backend overview data.
6. `Task 3c`: replace simulation-detail placeholder summary/info data with backend snapshot data.
7. `Task 4a`: add backend/API contract tests for snapshot/overview and empty states.
8. `Task 4b`: run MCP smoke validation and confirm one frontend integration path works with generated clients.

## Risks

- backend snapshot scope could bloat if it tries to satisfy all future UI needs instead of MVP detail needs
- frontend pages may currently depend on placeholder-only concepts that do not map cleanly to real backend data
- generated client changes could expose existing backend OpenAPI inconsistencies
- MCP currently computes some snapshot metrics client-side, so convergence on backend-owned semantics may require careful transition

## Handoff to next sprint

Sprint 4 should assume Sprint 3 provides stable overview and snapshot contracts.

That means Sprint 4 can focus on product experience rather than contract invention:

- real city overview list backed by Sprint 3 read models
- real simulation detail page backed by snapshot/history surfaces
- Pixi/event/timeline/invention UI work building on stable backend data instead of placeholders
