# Sprint 11 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 11" in a single pass.

Before using a prompt, check `docs/sprints/sprint11/sprint-11-controlled-director-interventions.md` and keep its `## Execution status` section current.

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references:
  - `docs/sprints/sprint11/sprint-11-controlled-director-interventions.md`
  - `docs/specs/agent-chat-orchestration-spec.md`
  - `docs/roadmap.md`
  - relevant `.cursor/rules/*.mdc`
- include the hard boundary: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- verify the work treats director commands as explicit interventions
- confirm provenance and confirmation semantics remain visible
- run chunk-level validation for touched backend/UI paths

## Per-chunk review/test/handoff checklist

For Sprint 11, also verify:

- the first director command is narrow and explicit
- intervention provenance is not implicit or hidden
- UI confirmation is meaningful, not decorative
- autonomous history and intervention history remain distinguishable

---

## Task 1a - Intervention Policy and Contract Lock

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Lock the policy, confirmation, and provenance rules for the first director intervention sprint.

Acceptance criteria (copied from sprint doc):
- a contributor can tell what makes a command an intervention
- confirmation, provenance, and visible labeling rules are unambiguous

In-scope files:
- docs/sprints/sprint11/sprint-11-controlled-director-interventions.md
- docs/specs/agent-chat-orchestration-spec.md
- docs/roadmap.md (read-only reference)

Out of scope:
- backend implementation
- UI confirmation implementation
- broad multi-command intervention planning

Instructions:
Implement only Task 1a. Tighten the first-command boundary and make the intervention rules explicit and small.
```

## Task 2a - Backend Intervention Model and Orchestration Path

```text
Task ID: Task 2a
Owner: Codex
Mode: modify code

Goal (one sentence):
Add the backend model, provenance handling, and orchestration path needed for one explicit intervention workflow.

Acceptance criteria (copied from sprint doc):
- the backend can execute one intervention through an explicit path
- provenance is preserved and queryable

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/agent/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/
- apps/backend/src/main/java/eu/catlabs/humanaity/event/
- apps/backend/src/test/java/eu/catlabs/humanaity/
- docs/specs/agent-chat-orchestration-spec.md (read-only reference)

Out of scope:
- several intervention powers
- guided workflow work
- frontend confirmation UI

Instructions:
Implement only Task 2a. Add the smallest explicit backend path that can support one intervention with provenance and policy checks.
```

## Task 2b - First Director Command

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Implement one narrow first director command, such as making two humans meet, without broadening the intervention surface.

Acceptance criteria (copied from sprint doc):
- the backend can execute one intervention through an explicit path
- provenance is preserved and queryable

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/agent/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/
- apps/backend/src/test/java/eu/catlabs/humanaity/

Out of scope:
- several intervention commands
- open-ended scenario editing
- UI confirmation behavior

Instructions:
Implement only Task 2b. Keep the first director command narrow, explicit, and clearly different from ordinary simulation commands.
```

## Task 3a - Console Confirmation and Intervention Labeling

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add a meaningful confirmation and labeling flow so the UI presents interventions differently from ordinary chat commands.

Acceptance criteria (copied from sprint doc):
- the user must confirm the intervention before execution
- the console clearly labels the action as a directed intervention

In-scope files:
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/features/city/services/

Out of scope:
- broad chat redesign
- new non-intervention commands
- backend intervention redesign

Instructions:
Implement only Task 3a. Keep the UX explicit and policy-aware rather than flashy or ambiguous.
```

## Task 4a - Validation and Sprint Closeout

```text
Task ID: Task 4a
Owner: Codex
Mode: modify code (tests/docs/status sync allowed)

Goal (one sentence):
Validate the first intervention path, record residual risks, and close Sprint 11 accurately.

Acceptance criteria (copied from sprint doc):
- the first intervention path is validated on the touched boundaries
- later intervention ideas remain explicitly deferred

In-scope files:
- docs/sprints/sprint11/sprint-11-controlled-director-interventions.md
- touched backend/UI files from Sprint 11 chunks

Out of scope:
- new intervention powers
- unrelated console cleanup
- broad simulation redesign

Instructions:
Implement only Task 4a. Run the nearest useful validation for the first intervention path and sync the sprint doc with the real result.
```
