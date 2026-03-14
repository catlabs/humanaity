# Sprint 8 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 8" in a single pass.

Before using a prompt, check `docs/sprints/sprint08/sprint-08-agent-chat-mvp.md` and keep its `## Execution status` section current.

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references:
  - `docs/sprints/sprint08/sprint-08-agent-chat-mvp.md`
  - `docs/specs/agent-chat-orchestration-spec.md`
  - `docs/roadmap.md`
  - `docs/sprint-execution-contract.md` (end-of-task: update execution status, one commit per task, commit format; do not push)
  - relevant `.cursor/rules/*.mdc`
- restate any mandatory constraints directly
- include the hard boundary: "Implement only this task ID; do not expand to other sprint tasks"
- for multi-sprint runs (e.g. sprint 8 to 11), include `docs/sprint-execution-contract.md` and the implement-sprint-range expectations so the agent commits after each task

Re-integrate Codex output before opening the next chunk:

- verify the work stayed backend-orchestration-first
- confirm deterministic/backend-owned state remains canonical
- run chunk-level validation for touched backend or UI paths
- update sprint/spec docs if implementation changes sprint-shaping decisions
- ensure execution status was updated and one commit was created for the task (per `docs/sprint-execution-contract.md`); if Codex did not commit, Cursor does it before the next chunk

## Per-chunk review/test/handoff checklist

Use `.cursor/rules/docs-chunk-review-loop.mdc` as the standard checklist.

For Sprint 8, also verify:

- frontend does not orchestrate MCP directly
- response payloads are UI-facing rather than raw tool traces
- safe commands stay bounded and city-scoped
- action replies trigger explicit refresh/focus behavior

---

## Task 1a - Orchestration Spec and Sprint 8 Boundary Lock

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Lock the backend orchestration contract and safe MVP command boundary for the first agent chat sprint.

Acceptance criteria (copied from sprint doc):
- a contributor can tell which commands belong in Sprint 8 and which are deferred
- the backend/frontend contract direction is fixed before implementation

In-scope files:
- docs/specs/agent-chat-orchestration-spec.md
- docs/sprints/sprint08/sprint-08-agent-chat-mvp.md
- docs/roadmap.md (read-only reference)

Out of scope:
- backend implementation
- frontend component work
- guided/director command design beyond explicit defer notes

Instructions:
Implement only Task 1a. Tighten the safe-command boundary and orchestration response/effect semantics so Sprint 8 remains small and demoable.
```

## Task 2a - Backend Agent Chat API Skeleton

```text
Task ID: Task 2a
Owner: Codex
Mode: modify code

Goal (one sentence):
Add the first backend-owned city-scoped agent chat endpoint, DTOs, and orchestration service skeleton.

Acceptance criteria (copied from sprint doc):
- the backend can interpret one city-scoped chat request and return a UI-facing response payload
- safe MVP commands execute through backend-owned services or read models rather than UI-only logic

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/agent/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/
- apps/backend/src/test/java/eu/catlabs/humanaity/
- docs/specs/agent-chat-orchestration-spec.md (read-only reference)
- docs/sprints/sprint08/sprint-08-agent-chat-mvp.md (read-only reference)

Out of scope:
- frontend chat UI
- guided or director commands
- broad auth redesign

Instructions:
Implement only Task 2a. Add a minimal backend orchestration slice with stable request/response DTOs and clear package placement.
```

## Task 2b - Safe MVP Command Executors

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Implement the safe MVP command handlers for step, snapshot, recent summary, event explanation, and recent inventions.

Acceptance criteria (copied from sprint doc):
- the backend can interpret one city-scoped chat request and return a UI-facing response payload
- safe MVP commands execute through backend-owned services or read models rather than UI-only logic

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/agent/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/
- apps/backend/src/main/java/eu/catlabs/humanaity/event/
- apps/backend/src/main/java/eu/catlabs/humanaity/invention/
- apps/backend/src/test/java/eu/catlabs/humanaity/
- docs/specs/agent-chat-orchestration-spec.md (read-only reference)

Out of scope:
- guided human workflows
- director interventions
- frontend rendering work

Instructions:
Implement only Task 2b. Keep command parsing bounded and explicit, and return user-facing responses plus stable entity ids/effects.
```

## Task 3a - Simulation Page Chat Panel

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add a lightweight chat panel to the authoritative simulation page and hook it to the new backend orchestration endpoint.

Acceptance criteria (copied from sprint doc):
- a user can submit a chat request from the main simulation page
- the page refreshes relevant state after allowed actions without guessing from free text

In-scope files:
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/features/city/city.service.ts
- apps/ui/src/app/api/ (generated client usage only)
- docs/specs/agent-chat-orchestration-spec.md (read-only reference)

Out of scope:
- whole-page visual redesign
- guided/director command UI
- backend orchestration changes except narrow contract-fix feedback

Instructions:
Implement only Task 3a. Add one practical chat surface to the current authoritative simulation page without turning the chunk into a console redesign.
```

## Task 3b - UI Effects and Refresh Loop

```text
Task ID: Task 3b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Wire orchestration response effects into snapshot/history refresh and simple focus/highlight behavior on the simulation page.

Acceptance criteria (copied from sprint doc):
- a user can submit a chat request from the main simulation page
- the page refreshes relevant state after allowed actions without guessing from free text

In-scope files:
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/features/city/services/
- apps/ui/src/app/features/city/city.service.ts

Out of scope:
- large layout refactor
- guided/follow behaviors
- director command confirmation flows

Instructions:
Implement only Task 3b. Apply backend-owned effects pragmatically so action commands cause the right refresh and selection behavior.
```

## Task 4a - Validation and Sprint Closeout

```text
Task ID: Task 4a
Owner: Codex
Mode: modify code (tests/docs/status sync allowed)

Goal (one sentence):
Validate the first agent chat loop, record residual risks, and close Sprint 8 accurately.

Acceptance criteria (copied from sprint doc):
- regressions in the new orchestration path are reasonably visible
- the sprint doc records actual validation results and residual risks

In-scope files:
- docs/sprints/sprint08/sprint-08-agent-chat-mvp.md
- touched backend/UI files from Tasks 2a through 3b

Out of scope:
- new feature expansion beyond Sprint 8
- console redesign work
- guided/director command work

Instructions:
Implement only Task 4a. Run the nearest useful validation for the new chat path, sync the sprint execution status, and record any blocked environment-dependent follow-up.
```
