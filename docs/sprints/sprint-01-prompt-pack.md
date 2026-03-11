# Sprint 1 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 1" in a single pass.

Before using a prompt, check `docs/sprints/sprint-01-foundation.md` and keep its `## Execution status` section current so the active chunk, next chunk, and blocked items stay visible in one place.

Each template includes:
- exact task ID
- one-sentence goal
- acceptance criteria copied from `docs/sprints/sprint-01-foundation.md`
- explicit in-scope files
- explicit out-of-scope items
- whether the tool should modify code or only propose a patch

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include all three repo-visible references in the prompt body:
  - `docs/sprints/sprint-01-foundation.md` for scope and acceptance criteria
  - `docs/specs/simulation-deterministic-spec.md` for deterministic rules
  - relevant `.cursor/rules/*.mdc` files when they contain mandatory policy for the chunk
- do not assume hidden Cursor skills are visible to Codex; restate mandatory constraints directly
- include a hard boundary line: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- Cursor reviews scope fit, package/layer alignment, and side effects
- Cursor runs chunk-level tests for the touched area
- If backend API endpoints changed, Cursor regenerates `apps/mcp` API types, updates MCP tools, and validates smoke flow through MCP
- Cursor compares result to sprint acceptance criteria/DoD
- Cursor updates sprint/spec docs if implementation changed sprint-shaping decisions

## Per-chunk review/test/handoff checklist

Use `.cursor/rules/docs-chunk-review-loop.mdc` as the standard checklist and go/no-go gate after every chunk implementation.

For Sprint 1, also verify deterministic guarantees and scheduler-vs-step separation during integration review.

When a chunk adds or changes backend endpoints, include MCP alignment in the handoff gate:

- run `npm run api:generate` in `apps/mcp`
- add/update tool wrappers in `apps/mcp/src/tools/`
- run `npm run build` in `apps/mcp`
- run at least one MCP smoke sequence covering the new endpoint path

---

## Task 1a - Deterministic Simulation Rules Spec

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Write and lock the deterministic simulation rules spec so `step()` semantics are implementable without guessing.

Acceptance criteria (copied from sprint doc):
- a developer can read the spec and implement `step()` without guessing core semantics
- the spec clearly states what is deterministic and what is deferred

In-scope files:
- docs/specs/simulation-deterministic-spec.md
- docs/sprints/sprint-01-foundation.md (read-only reference)

Out of scope:
- backend Java refactors
- scheduler changes
- UI changes
- events/inventions design beyond explicit deferred notes

Instructions:
Implement only Task 1a. Update the spec with tick semantics, deterministic RNG policy, stable output expectations for reproducibility tests, and explicit deferred behavior. Keep wording implementation-ready and concise.
```

## Task 2a - SimulationRun Aggregate and Status Enum

```text
Task ID: Task 2a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add a first-class `SimulationRun` aggregate and status enum in backend domain packages.

Acceptance criteria (copied from sprint doc):
- a city can have a persisted simulation run
- simulation state can be resumed without losing determinism assumptions

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/domain/SimulationRun.java (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/domain/SimulationRunStatus.java (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java (read-only for now unless minimal wiring is required)

Out of scope:
- repository/service lifecycle wiring beyond minimal compile-safe scaffolding
- controller/API endpoints
- deterministic step engine logic
- scheduler behavior changes

Instructions:
Implement only Task 2a. Follow existing backend package conventions used by `city` and `human`. Include city reference, seed, tick, status, and timestamps in the aggregate.
```

## Task 2b - Persistence and Lifecycle Service Wiring

```text
Task ID: Task 2b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add persistence support and lifecycle service methods for `SimulationRun` without changing step-engine behavior.

Acceptance criteria (copied from sprint doc):
- a city can have a persisted simulation run
- simulation state can be resumed without losing determinism assumptions

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/infrastructure/persistence/SimulationRunRepository.java (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/domain/SimulationRun.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/domain/SimulationRunStatus.java

Out of scope:
- API/controller contract changes except what is strictly needed for compile
- deterministic step extraction/refactor
- scheduler wrapper refactor
- determinism test suite

Instructions:
Implement only Task 2b. Add create/load/pause/resume lifecycle methods and persistence wiring. Preserve current runtime behavior; do not refactor step logic here.
```

## Task 2c - API and Controller Lifecycle Surface

```text
Task ID: Task 2c
Owner: Cursor
Mode: modify code

Goal (one sentence):
Expose or refine simulation run lifecycle operations in API/controller layers.

Acceptance criteria (copied from sprint doc):
- a city can have a persisted simulation run
- simulation state can be resumed without losing determinism assumptions

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/SimulationController.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/ (new or update)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java (only required API wiring)

Out of scope:
- deterministic step engine internals
- scheduler wrapper logic changes
- broad endpoint redesign outside run lifecycle
- frontend integration work

Instructions:
Implement only Task 2c. Keep API changes minimal and aligned with existing project style. Add/create/load/pause/resume lifecycle surface; avoid unrelated endpoint expansion. Shape API/read-model choices to preserve city-first UX instead of introducing a standalone simulation screen contract.
```

## Task 3a - Pure Deterministic `step()` Extraction

```text
Task ID: Task 3a
Owner: Codex
Mode: modify code

Goal (one sentence):
Extract pure tick execution from the prototype simulation flow into a deterministic `step()` path.

Acceptance criteria (copied from sprint doc):
- simulation logic can run one step at a time
- repeated runs with the same seed produce the same state transitions
- scheduling is no longer the source of truth

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/SimulationController.java (only if entrypoint updates are required)
- apps/backend/src/main/java/eu/catlabs/humanaity/human/domain/Human.java (only if deterministic step needs domain-safe updates)

Out of scope:
- scheduler wrapper rewrite (Task 4a/4b)
- full RNG policy changes beyond what is needed for extraction (Task 3b)
- tests (Task 5a-5c)
- events/inventions/UI scope

Instructions:
Implement only Task 3a. Keep behavior bounded to deterministic single-step execution path and isolate business logic from scheduler flow.
```

## Task 3b - Seeded RNG Refactor

```text
Task ID: Task 3b
Owner: Codex
Mode: modify code

Goal (one sentence):
Replace uncontrolled randomness in core simulation step execution with seeded deterministic randomness.

Acceptance criteria (copied from sprint doc):
- simulation logic can run one step at a time
- repeated runs with the same seed produce the same state transitions
- scheduling is no longer the source of truth

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/domain/SimulationRun.java (if seed usage needs domain exposure)

Out of scope:
- deterministic ordering changes not required for RNG refactor (Task 3c)
- scheduler wrapper changes
- determinism test additions (Task 5a-5c)
- AI/Faker generation flow changes for production code

Instructions:
Implement only Task 3b. Remove unseeded randomness (`new Random()`, `Collections.shuffle(...)` without deterministic source) from core step logic and use run seed-derived deterministic RNG.
```

## Task 3c - Stable Deterministic Processing Order

```text
Task ID: Task 3c
Owner: Codex
Mode: modify code

Goal (one sentence):
Enforce explicit stable processing order so step outcomes are not dependent on implicit repository or collection ordering.

Acceptance criteria (copied from sprint doc):
- simulation logic can run one step at a time
- repeated runs with the same seed produce the same state transitions
- scheduling is no longer the source of truth

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/human/infrastructure/persistence/HumanRepository.java

Out of scope:
- scheduler wrapper changes
- broad repository redesign outside deterministic ordering needs
- reproducibility test suite additions

Instructions:
Implement only Task 3c. Use explicit stable ordering for humans processed during a step; do not rely on database default order or unordered collections.
```

## Task 3d - Repeated Deterministic Stepping

```text
Task ID: Task 3d
Owner: Codex
Mode: modify code

Goal (one sentence):
Support deterministic repeated stepping (`step(..., count)`) with consistent state evolution.

Acceptance criteria (copied from sprint doc):
- simulation logic can run one step at a time
- repeated runs with the same seed produce the same state transitions
- scheduling is no longer the source of truth

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/SimulationController.java (if repeated-step endpoint or contract needs update)

Out of scope:
- scheduler runtime orchestration changes (Task 4a/4b)
- test suite additions (Task 5a-5c)
- non-deterministic optimization work

Instructions:
Implement only Task 3d. Add bounded repeated stepping that composes deterministic single-step logic and preserves deterministic state progression across all iterations.
```

## Task 3e - Movement Smoke Mode (Neutralize Busy Lock)

```text
Task ID: Task 3e
Owner: Cursor
Mode: modify code

Goal (one sentence):
Remove or neutralize busy-lock semantics so human movement remains observable during short-interval smoke tests.

Acceptance criteria (copied from sprint doc):
- simulation logic can run one step at a time
- repeated runs with the same seed produce the same state transitions
- scheduling is no longer the source of truth
- movement smoke checks can observe position changes without humans becoming permanently stuck behind busy-lock semantics

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/human/domain/Human.java (only if needed to align behavior/read-model expectations)
- apps/backend/src/main/java/eu/catlabs/humanaity/human/api/dto/ (only if busy-field contract needs minimal alignment)

Out of scope:
- scheduler wrapper redesign (Task 4a/4b)
- broad simulation API redesign outside movement smoke-mode needs
- full long-term collision model design
- frontend redesign work

Instructions:
Implement only Task 3e. Keep the change explicit as a movement smoke-mode simplification and avoid mixing it with unrelated deterministic engine refactors.
```

## Task 4a - Scheduler Delegates to `step()` Only

```text
Task ID: Task 4a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Refactor runtime scheduling so it delegates to deterministic `step()` and no longer acts as the simulation engine.

Acceptance criteria (copied from sprint doc):
- wall-clock execution is optional and thin
- deterministic logic remains testable without background threads

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/SimulationController.java (only if start/stop contract requires adjustment)

Out of scope:
- changing deterministic step business rules
- new simulation features outside wrapper/orchestration
- broad concurrency redesign

Instructions:
Implement only Task 4a. Keep scheduler as orchestration that repeatedly calls deterministic `step()`; no scheduler-owned mutation logic.
```

## Task 4b - Remove Hidden Scheduler Business Logic

```text
Task ID: Task 4b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Remove any remaining scheduler-owned business logic so all canonical simulation mutation happens in deterministic step flow.

Acceptance criteria (copied from sprint doc):
- wall-clock execution is optional and thin
- deterministic logic remains testable without background threads

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java
- any simulation package classes directly involved in scheduler callbacks (minimal required scope only)

Out of scope:
- new simulation mechanics
- UI/API redesign not needed for wrapper cleanup
- test suite expansion beyond targeted coverage updates

Instructions:
Implement only Task 4b. Move hidden mutation logic out of scheduler path and keep runtime execution strictly as delegation/orchestration.
```

## Task 5a - Same-Seed Reproducibility Test

```text
Task ID: Task 5a
Owner: Codex
Mode: modify code

Goal (one sentence):
Add automated test coverage proving same seed + same initial state + same step count yields same result.

Acceptance criteria (copied from sprint doc):
- deterministic expectations are covered by automated tests
- regressions in simulation reproducibility are easy to detect

In-scope files:
- apps/backend/src/test/java/eu/catlabs/humanaity/ (new or update test classes)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java (only if minimal testability hooks are required)

Out of scope:
- broad production refactors unrelated to testability
- AI/Faker-driven fixture setup
- UI test work

Instructions:
Implement only Task 5a. Use explicit deterministic fixtures (no AI/Faker generation path) and assert reproducible state transitions for repeated equivalent runs.
```

## Task 5b - Pause/Resume Continuity Test

```text
Task ID: Task 5b
Owner: Codex
Mode: modify code

Goal (one sentence):
Add a test that pause/resume preserves simulation run continuity and determinism assumptions.

Acceptance criteria (copied from sprint doc):
- deterministic expectations are covered by automated tests
- regressions in simulation reproducibility are easy to detect

In-scope files:
- apps/backend/src/test/java/eu/catlabs/humanaity/ (new or update test classes)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java (only if minimal pause/resume test hooks are needed)

Out of scope:
- lifecycle API redesign
- unrelated scheduler feature work
- non-deterministic fixtures

Instructions:
Implement only Task 5b. Test that pausing and resuming does not corrupt run tick, seed usage, or deterministic continuation of state.
```

## Task 5c - Scheduler vs Manual Step Equivalence Test

```text
Task ID: Task 5c
Owner: Codex
Mode: modify code

Goal (one sentence):
Add a test that scheduler-wrapper execution and manual deterministic stepping converge to the same simulation result.

Acceptance criteria (copied from sprint doc):
- deterministic expectations are covered by automated tests
- regressions in simulation reproducibility are easy to detect

In-scope files:
- apps/backend/src/test/java/eu/catlabs/humanaity/ (new or update test classes)
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java (only if minimal seam exposure is needed)

Out of scope:
- scheduler redesign beyond parity hooks
- adding new simulation rules unrelated to equivalence verification
- frontend/test-infra expansion

Instructions:
Implement only Task 5c. Compare results from runtime-wrapper path and direct step path under the same deterministic initial fixture and assert equivalent final state.
```

