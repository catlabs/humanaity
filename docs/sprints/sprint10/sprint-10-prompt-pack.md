# Sprint 10 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 10" in a single pass.

Before using a prompt, check `docs/sprints/sprint10/sprint-10-guided-human-workflows.md` and keep its `## Execution status` section current.

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references:
  - `docs/sprints/sprint10/sprint-10-guided-human-workflows.md`
  - `docs/specs/agent-chat-orchestration-spec.md`
  - `docs/roadmap.md`
  - relevant `.cursor/rules/*.mdc`
- include the hard boundary: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- verify guided commands remain observation-first
- confirm director/intervention behavior did not slip into the sprint
- run chunk-level validation for touched backend/UI paths

## Per-chunk review/test/handoff checklist

For Sprint 10, also verify:

- follow semantics are bounded
- compare outputs are structured enough for UI use
- focus/tracking behavior uses backend-owned ids and effects

---

## Task 1a - Guided Command Semantics Lock

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Lock focus, compare, and follow semantics so Sprint 10 deepens observation without becoming an intervention sprint.

Acceptance criteria (copied from sprint doc):
- a contributor can distinguish guided commands from director commands without guessing
- bounded inputs and outputs are explicit

In-scope files:
- docs/sprints/sprint10/sprint-10-guided-human-workflows.md
- docs/specs/agent-chat-orchestration-spec.md
- docs/roadmap.md (read-only reference)

Out of scope:
- intervention command design
- backend implementation
- frontend component changes

Instructions:
Implement only Task 1a. Tighten guided semantics and make the non-intervention boundary explicit.
```

## Task 2a - Backend Guided Workflow Support

```text
Task ID: Task 2a
Owner: Codex
Mode: modify code

Goal (one sentence):
Add backend orchestration support for focus, compare, and follow workflows.

Acceptance criteria (copied from sprint doc):
- guided workflows are backend-owned and city-scoped
- compare/follow responses expose structured data plus UI effects

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/agent/
- apps/backend/src/main/java/eu/catlabs/humanaity/human/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/
- apps/backend/src/test/java/eu/catlabs/humanaity/
- docs/specs/agent-chat-orchestration-spec.md (read-only reference)

Out of scope:
- director commands
- broad domain redesign
- frontend rendering

Instructions:
Implement only Task 2a. Keep guided workflows observation-first and return structured data plus stable ids/effects.
```

## Task 2b - Narrow Read-Model and Effect Helpers

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Add only the narrow read-model or response-shaping helpers needed to make guided workflows usable in the console.

Acceptance criteria (copied from sprint doc):
- guided workflows are backend-owned and city-scoped
- compare/follow responses expose structured data plus UI effects

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/agent/
- apps/backend/src/main/java/eu/catlabs/humanaity/human/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/
- apps/backend/src/test/java/eu/catlabs/humanaity/

Out of scope:
- large API redesign
- intervention persistence/modeling
- frontend-only work

Instructions:
Implement only Task 2b. Add the smallest backend support necessary for guided focus/compare/follow usability.
```

## Task 3a - Console Integration for Guided Workflows

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Integrate guided focus/tracking behavior into the simulation console so the new commands feel native to the page.

Acceptance criteria (copied from sprint doc):
- the UI can highlight or track a human based on backend effects
- compare/follow flows feel like part of the same console rather than a side feature

In-scope files:
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/features/city/services/

Out of scope:
- intervention confirmation flows
- unrelated console redesign
- backend orchestration redesign

Instructions:
Implement only Task 3a. Add intentional focus/tracking behavior without turning the page into a separate guided-workflow workstation.
```

## Task 4a - Validation and Sprint Closeout

```text
Task ID: Task 4a
Owner: Codex
Mode: modify code (tests/docs/status sync allowed)

Goal (one sentence):
Validate the guided workflow path and close Sprint 10 with explicit deferred-intervention notes.

Acceptance criteria (copied from sprint doc):
- the guided path is validated on the touched boundaries
- deferred intervention work remains explicit

In-scope files:
- docs/sprints/sprint10/sprint-10-guided-human-workflows.md
- touched backend/UI files from Sprint 10 chunks

Out of scope:
- director command implementation
- unrelated cleanup
- broad UX redesign

Instructions:
Implement only Task 4a. Run the nearest useful validation, sync the sprint status, and record the remaining gap before Sprint 11.
```
