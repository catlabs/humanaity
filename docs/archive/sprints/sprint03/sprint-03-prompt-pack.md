# Sprint 3 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 3" in a single pass.

Before using a prompt, check `docs/sprints/sprint03/sprint-03-simulation-read-model.md` and keep its `## Execution status` section current so the active chunk, next chunk, and blocked items stay visible in one place.

Each template includes:
- exact task ID
- one-sentence goal
- acceptance criteria copied from `docs/sprints/sprint03/sprint-03-simulation-read-model.md`
- explicit in-scope files
- explicit out-of-scope items
- whether the tool should modify code or only propose a patch

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references in the prompt body:
  - `docs/sprints/sprint03/sprint-03-simulation-read-model.md` for scope and acceptance criteria
  - `docs/roadmap.md` for epic alignment
  - `docs/specs/simulation-read-model-spec.md` once Task 1a is complete
  - relevant `.cursor/rules/*.mdc` files when they contain mandatory policy for the chunk
- do not assume hidden Cursor skills are visible to Codex; restate mandatory constraints directly
- include a hard boundary line: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- Cursor reviews scope fit, package/layer alignment, and side effects
- Cursor runs chunk-level tests for the touched area
- if backend read-model endpoints changed, Cursor regenerates both `apps/ui` and `apps/mcp` contracts before integration review completes
- if MCP tools changed, Cursor validates at least one smoke path through MCP
- Cursor compares result to sprint acceptance criteria and definition of done
- Cursor updates sprint/spec docs if implementation changed sprint-shaping decisions

## Per-chunk review/test/handoff checklist

Use `.cursor/rules/docs-chunk-review-loop.mdc` as the standard checklist and go/no-go gate after every chunk implementation.

For Sprint 3, also verify:

- backend-owned read-model semantics are not duplicated in frontend/MCP code
- empty-state behavior is explicit and tested
- generated client usage replaces fallback logic where the contract now exists

When a chunk adds or changes backend read-model endpoints, include contract alignment in the handoff gate:

- regenerate the UI OpenAPI client
- run `npm run api:generate` in `apps/mcp`
- update MCP wrappers in `apps/mcp/src/tools/`
- run `npm run build` in `apps/mcp`
- validate one MCP smoke flow and one UI integration path for the new endpoint contract

---

## Task 1a - Simulation Read-Model Spec

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Write and lock the simulation read-model spec so overview and snapshot contracts are implementable without guessing.

Acceptance criteria (copied from sprint doc):
- a developer can read the spec and implement snapshot/overview contracts without guessing field semantics
- the spec clearly states which fields are canonical, derived, and deferred

In-scope files:
- docs/specs/simulation-read-model-spec.md
- docs/sprints/sprint03/sprint-03-simulation-read-model.md (read-only reference)
- docs/roadmap.md (read-only reference)

Out of scope:
- backend Java refactors
- API/controller implementation
- OpenAPI regeneration
- frontend page wiring

Instructions:
Implement only Task 1a. Define the MVP city overview and unified simulation snapshot contracts, year/era projection rules, empty-state semantics, and stable fields required for contract tests and client regeneration.
```

## Task 2a - Overview and Snapshot API Surface

```text
Task ID: Task 2a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add backend DTOs and controller/service surface for city overview and unified simulation snapshot reads.

Acceptance criteria (copied from sprint doc):
- overview and snapshot data can be queried directly from backend endpoints
- clients do not need to compose product-critical fields from multiple unrelated endpoints for the Sprint 3 surfaces

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/SimulationController.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/ (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java

Out of scope:
- frontend wiring
- OpenAPI client regeneration
- broad query/metrics cleanup beyond what is required for compile-safe endpoint delivery
- visual/UI redesign

Instructions:
Implement only Task 2a. Add minimal stable endpoint(s) and DTOs for city overview and simulation snapshot, keeping naming and package placement aligned with the existing simulation API style.
```

## Task 2b - Deterministic Projection Consolidation

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Centralize deterministic projection and metric assembly for snapshot/overview contracts inside backend query code.

Acceptance criteria (copied from sprint doc):
- snapshot/overview projection logic is defined in one backend-owned path
- UI and MCP consume the regenerated contract rather than maintaining divergent derived logic

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/query/ (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/ (only if projection output needs small adjustments)
- docs/specs/simulation-read-model-spec.md (read-only reference after Task 1a)

Out of scope:
- Angular page wiring
- MCP tool updates
- visual/UI changes
- non-read-model simulation behavior changes

Instructions:
Implement only Task 2b. Move product-facing derived fields and aggregate metrics behind one backend-owned projection path so downstream consumers stop recomputing them independently.
```

## Task 3a - OpenAPI and MCP Alignment

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Regenerate UI/MCP contract artifacts for the new read-model endpoints and align MCP wrappers with the canonical backend snapshot/overview responses.

Acceptance criteria (copied from sprint doc):
- snapshot/overview projection logic is defined in one backend-owned path
- UI and MCP consume the regenerated contract rather than maintaining divergent derived logic

In-scope files:
- apps/ui/src/app/api/ (generated updates as needed)
- apps/mcp/src/generated/ (generated updates as needed)
- apps/mcp/src/backend-client.ts
- apps/mcp/src/tools/simulation-tools.ts

Out of scope:
- new backend business rules unrelated to contract alignment
- frontend page rewrites
- large MCP UX redesign

Instructions:
Implement only Task 3a. Regenerate contract artifacts, update MCP wrappers to the new overview/snapshot endpoints, and remove duplicated snapshot derivation where the backend now owns the fields.
```

## Task 3b - City List Read-Model Adoption

```text
Task ID: Task 3b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Replace city-list fake simulation metadata with backend-owned city overview data.

Acceptance criteria (copied from sprint doc):
- city list rows use backend-owned simulation metadata
- simulation detail no longer depends on placeholder info-bar values for Sprint 3-covered surfaces
- UI changes stay contract-focused and do not expand into a redesign sprint

In-scope files:
- apps/ui/src/app/features/city/city.service.ts
- apps/ui/src/app/features/city/pages/list/city-list.page.ts
- apps/ui/src/app/features/city/pages/list/city-list.page.html (only if bindings need minimal adjustment)
- apps/ui/src/app/api/ (generated client usage only)

Out of scope:
- simulation detail page changes
- component redesign
- Pixi/canvas work
- admin page cleanup

Instructions:
Implement only Task 3b. Remove random/fake overview derivation and consume the backend overview contract through generated services where available.
```

## Task 3c - Simulation Detail Snapshot Adoption

```text
Task ID: Task 3c
Owner: Cursor
Mode: modify code

Goal (one sentence):
Replace simulation-detail placeholder summary/info-bar data with the backend snapshot contract while preserving the current page structure.

Acceptance criteria (copied from sprint doc):
- city list rows use backend-owned simulation metadata
- simulation detail no longer depends on placeholder info-bar values for Sprint 3-covered surfaces
- UI changes stay contract-focused and do not expand into a redesign sprint

In-scope files:
- apps/ui/src/app/features/city/city.service.ts
- apps/ui/src/app/features/city/pages/simulation-detail/simulation-detail.component.ts
- apps/ui/src/app/features/city/pages/simulation-detail/simulation-detail.component.html (only if bindings need minimal adjustment)
- apps/ui/src/app/api/ (generated client usage only)

Out of scope:
- broad timeline/event panel redesign
- Pixi canvas implementation
- AI-generated summaries
- unrelated city page refactors

Instructions:
Implement only Task 3c. Use the backend snapshot contract for summary/info fields and remove Sprint 3-scoped placeholder metadata without redesigning the component.
```

## Task 4a - Snapshot and Overview Contract Tests

```text
Task ID: Task 4a
Owner: Codex
Mode: modify code

Goal (one sentence):
Add backend/API tests covering snapshot and overview contract semantics, especially empty-state and deterministic summary behavior.

Acceptance criteria (copied from sprint doc):
- regressions in snapshot/overview semantics are easy to detect
- contract drift between backend and consumers is caught before Sprint 4 frontend work

In-scope files:
- apps/backend/src/test/java/eu/catlabs/humanaity/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/SimulationController.java (read-only unless test-driven contract fix is required)
- docs/specs/simulation-read-model-spec.md (read-only reference after Task 1a)

Out of scope:
- Angular UI tests
- major endpoint redesign
- MCP tool changes
- unrelated simulation-rule changes

Instructions:
Implement only Task 4a. Add focused tests for city overview and simulation snapshot endpoints, covering no-run-yet, no-history-yet, and stable aggregate field behavior.
```

## Task 4b - MCP Smoke and Frontend Integration Validation

```text
Task ID: Task 4b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Validate the new read-model contract through one MCP smoke flow and one frontend integration path.

Acceptance criteria (copied from sprint doc):
- regressions in snapshot/overview semantics are easy to detect
- contract drift between backend and consumers is caught before Sprint 4 frontend work

In-scope files:
- apps/mcp/src/tools/simulation-tools.ts
- apps/mcp/src/backend-client.ts
- docs/sprints/sprint03/sprint-03-simulation-read-model.md (execution-status update only)

Out of scope:
- new backend business rules
- visual UI redesign
- broad test-harness work outside the smoke path

Instructions:
Implement only Task 4b. Run one city-scoped MCP smoke flow covering overview plus snapshot, verify one frontend integration path uses the generated contract, and record the result in the sprint doc execution status.
```
