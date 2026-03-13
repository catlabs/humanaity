# Sprint 2 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 2" in a single pass.

Before using a prompt, check `docs/sprints/sprint02/sprint-02-history-ledger.md` and keep its `## Execution status` section current so the active chunk, next chunk, and blocked items stay visible in one place.

Each template includes:
- exact task ID
- one-sentence goal
- acceptance criteria copied from `docs/sprints/sprint02/sprint-02-history-ledger.md`
- explicit in-scope files
- explicit out-of-scope items
- whether the tool should modify code or only propose a patch

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references in the prompt body:
  - `docs/sprints/sprint02/sprint-02-history-ledger.md` for scope and acceptance criteria
  - `docs/roadmap.md` for epic alignment
  - `docs/specs/history-ledger-spec.md` once Task 1a is complete
  - relevant `.cursor/rules/*.mdc` files when they contain mandatory policy for the chunk
- do not assume hidden Cursor skills are visible to Codex; restate mandatory constraints directly
- include a hard boundary line: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- Cursor reviews scope fit, package/layer alignment, and side effects
- Cursor runs chunk-level tests for the touched area
- if backend history endpoints changed, Cursor regenerates `apps/mcp` API types, updates MCP tools, and validates smoke flow through MCP
- Cursor compares result to sprint acceptance criteria and definition of done
- Cursor updates sprint/spec docs if implementation changed sprint-shaping decisions

## Per-chunk review/test/handoff checklist

Use `.cursor/rules/docs-chunk-review-loop.mdc` as the standard checklist and go/no-go gate after every chunk implementation.

For Sprint 2, also verify deterministic history generation, stable ordering, and city-scoped query behavior during integration review.

When a chunk adds or changes backend history endpoints, include MCP alignment in the handoff gate:

- run `npm run api:generate` in `apps/mcp`
- add or update tool wrappers in `apps/mcp/src/tools/`
- run `npm run build` in `apps/mcp`
- run at least one MCP smoke sequence covering the new history endpoint path

---

## Task 1a - History Ledger Spec

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Write and lock the history ledger spec so event, invention, and timeline behavior are implementable without guessing.

Acceptance criteria (copied from sprint doc):
- a developer can read the spec and implement event/invention persistence without guessing semantics
- the spec clearly states what is deterministic and what is deferred

In-scope files:
- docs/specs/history-ledger-spec.md
- docs/sprints/sprint02/sprint-02-history-ledger.md (read-only reference)
- docs/roadmap.md (read-only reference)

Out of scope:
- backend Java refactors
- API/controller changes
- AI prompt design
- frontend timeline work

Instructions:
Implement only Task 1a. Define the MVP event taxonomy, invention emergence rules, deterministic year/era mapping, event emission points, and stable fields required for same-seed reproducibility tests.
```

## Task 2a - Event Aggregate and Repository Scaffolding

```text
Task ID: Task 2a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add a first-class event aggregate and repository scaffolding in backend history packages.

Acceptance criteria (copied from sprint doc):
- a city run can persist ordered event records
- event creation is driven by deterministic backend logic, not wall-clock timing or AI output

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/event/ (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/history/ (new or update if needed for shared history types)
- apps/backend/src/main/java/eu/catlabs/humanaity/entity/Interaction.java (read-only unless migration/deprecation is strictly required)

Out of scope:
- simulation emission logic
- invention derivation
- controller/API endpoints
- MCP updates

Instructions:
Implement only Task 2a. Add the event domain model, persistence support, and supporting value types while keeping package placement aligned with existing backend conventions.
```

## Task 2b - Deterministic Event Emission

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Emit deterministic events from simulation and lifecycle seams into the persisted event ledger.

Acceptance criteria (copied from sprint doc):
- a city run can persist ordered event records
- event creation is driven by deterministic backend logic, not wall-clock timing or AI output

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/event/ (new or update)
- docs/specs/history-ledger-spec.md (read-only reference after Task 1a)

Out of scope:
- invention logic
- history query endpoints
- MCP tooling updates
- frontend integration

Instructions:
Implement only Task 2b. Hook deterministic event emission into simulation/lifecycle flow, keep ordering explicit, and avoid mixing in non-deterministic timestamps or AI-generated content as canonical fields.
```

## Task 3a - Invention Aggregate and Repository Scaffolding

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add a first-class invention aggregate and repository scaffolding in backend history packages.

Acceptance criteria (copied from sprint doc):
- inventions can be persisted for a city
- each invention links back to deterministic source events or equivalent evidence
- invention creation is reproducible for equivalent runs

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/invention/ (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/event/ (read-only unless source-linking types require minimal support)

Out of scope:
- deterministic invention-emergence logic
- controller/API endpoints
- MCP tooling updates
- AI enrichment fields

Instructions:
Implement only Task 3a. Add the invention domain model, persistence support, and source-event linkage structure without implementing the emergence algorithm yet.
```

## Task 3b - Deterministic Invention Derivation

```text
Task ID: Task 3b
Owner: Codex
Mode: modify code

Goal (one sentence):
Derive inventions deterministically from persisted event patterns without duplicates.

Acceptance criteria (copied from sprint doc):
- inventions can be persisted for a city
- each invention links back to deterministic source events or equivalent evidence
- invention creation is reproducible for equivalent runs

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/invention/ (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/event/ (new or update if needed for source-pattern reads)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java (only if minimal wiring is required)
- docs/specs/history-ledger-spec.md (read-only reference after Task 1a)

Out of scope:
- history query endpoints
- MCP tooling
- AI-generated summaries
- broad simulation expansion unrelated to invention emergence

Instructions:
Implement only Task 3b. Keep invention emergence deterministic, source-linked, and idempotent for equivalent runs and repeated step execution.
```

## Task 4a - History Query API and DTOs

```text
Task ID: Task 4a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Expose city-scoped event feed, timeline, and inventions query surfaces in the backend API.

Acceptance criteria (copied from sprint doc):
- history data can be queried by city without frontend-only synthesis
- API ordering and scoping are explicit enough for deterministic verification
- MCP can exercise the new endpoint path after contract updates

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/SimulationController.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/ (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/event/ (query/service support only if needed)
- apps/backend/src/main/java/eu/catlabs/humanaity/invention/ (query/service support only if needed)

Out of scope:
- frontend wiring
- unified snapshot read model
- AI enrichment
- broad endpoint redesign beyond city-scoped history queries

Instructions:
Implement only Task 4a. Add minimal but stable history endpoints and DTOs with explicit city scoping, ordering, and filtering semantics.
```

## Task 4b - OpenAPI/MCP Alignment and Smoke Validation

```text
Task ID: Task 4b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Regenerate OpenAPI/MCP artifacts for the new history endpoints and prove the contract through a smoke flow.

Acceptance criteria (copied from sprint doc):
- history data can be queried by city without frontend-only synthesis
- API ordering and scoping are explicit enough for deterministic verification
- MCP can exercise the new endpoint path after contract updates

In-scope files:
- apps/mcp/src/tools/ (new or update)
- apps/mcp/src/generated/ (generated updates as needed)
- backend OpenAPI-driven contract outputs touched by regeneration

Out of scope:
- frontend feature work
- new backend business rules unrelated to contract alignment
- large MCP UX redesign

Instructions:
Implement only Task 4b. Regenerate API artifacts, add or update MCP wrappers for history endpoints, build MCP, and validate one city-scoped history smoke flow.
```

## Task 5a - Event and Invention Reproducibility Tests

```text
Task ID: Task 5a
Owner: Codex
Mode: modify code

Goal (one sentence):
Add automated tests proving same seed and same step sequence yield the same ordered event and invention output.

Acceptance criteria (copied from sprint doc):
- deterministic history expectations are covered by automated tests
- regressions in event/invention reproducibility are easy to detect
- contract drift on new history endpoints is caught early

In-scope files:
- apps/backend/src/test/java/eu/catlabs/humanaity/ (new or update test classes)
- apps/backend/src/main/java/eu/catlabs/humanaity/event/ (only if minimal test seams are required)
- apps/backend/src/main/java/eu/catlabs/humanaity/invention/ (only if minimal test seams are required)

Out of scope:
- frontend tests
- AI/Faker-driven fixtures
- broad production refactors unrelated to testability

Instructions:
Implement only Task 5a. Use explicit deterministic fixtures and assert same-seed equivalence for ordered event and invention outputs.
```

## Task 5b - Timeline Ordering and API Contract Coverage

```text
Task ID: Task 5b
Owner: Codex
Mode: modify code

Goal (one sentence):
Add tests for timeline ordering, city scoping, and minimal history API contract behavior.

Acceptance criteria (copied from sprint doc):
- deterministic history expectations are covered by automated tests
- regressions in event/invention reproducibility are easy to detect
- contract drift on new history endpoints is caught early

In-scope files:
- apps/backend/src/test/java/eu/catlabs/humanaity/ (new or update test classes)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/ (read-only unless minimal testability hooks are required)
- apps/backend/src/main/java/eu/catlabs/humanaity/event/ (read-only unless minimal testability hooks are required)
- apps/backend/src/main/java/eu/catlabs/humanaity/invention/ (read-only unless minimal testability hooks are required)

Out of scope:
- frontend integration
- MCP tool UX changes
- new history business rules unrelated to ordering/scoping verification

Instructions:
Implement only Task 5b. Validate timeline ordering, city scoping, and minimal API contract expectations with deterministic fixtures and focused backend tests.
```
