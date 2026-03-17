# Sprint 10: Guided Human Workflows

## Execution status

- Current phase: Sprint 10 complete
- Active chunk: none
- Next chunk: Sprint 11 `Task 1a` - intervention policy and contract lock
- Blocked items: depends on Sprint 9 console stabilization
- Last completed chunk: `Task 4a` - validation and closeout (2026-03-14)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Locked guided-command semantics as observation-first only (focus, compare, bounded follow) and explicitly excluded intervention behavior. |
| Task 2a | done | Added backend orchestration support for guided `focus`, `compare`, and bounded `follow` commands with city-scoped policy checks. |
| Task 2b | done | Added follow-window read-model helpers and bounded structured response/effect shaping for guided `follow` usability. |
| Task 3a | done | Added guided compare/follow console context rendering with tracked-human behavior driven by backend effects and structured data. |
| Task 4a | done | Validation passed for backend guided API contract and UI type checks; `npm run build` terminated in this environment (exit `-1`) and is tracked as residual risk. |

## Sprint intent

Sprint 10 exists to move the console from general city commands into human-centered guided exploration.

This sprint is still observation-first. It should deepen the product without crossing into explicit world intervention.

## Why this sprint comes next

Once the map + chat console is coherent, the next portfolio-impressive step is not random new powers. It is guided observation that makes the world feel inspectable and legible through the agent.

If Sprint 10 is skipped:

- the console remains useful but shallow
- users cannot easily move from city-level summaries to human-centric exploration
- Sprint 11 intervention work would arrive before the system proves richer observation workflows

## Sprint outcome

At the end of Sprint 10, the user can ask the console to focus on a human, compare two humans, or follow one human for bounded ticks, and the UI responds with guided map/context behavior grounded in backend-owned state.

## Sprint scope

### In scope

- define guided observation semantics for focus, compare, and follow
- add backend orchestration support for those workflows
- add narrow read-model helpers if current contracts are insufficient
- add UI effects and console behavior for focused/followed humans
- keep all workflows bounded and observation-first

### Out of scope

- director/intervention commands
- forcing interactions between humans
- open-ended autonomous agent loops
- broad data-model redesign unrelated to guided observation

## Product and technical decisions for this sprint

### Decision 1: guided commands are still reads

Sprint 10 commands may change what the UI emphasizes or how much bounded stepping happens during a follow flow, but they are still observation workflows, not interventions.

### Decision 2: focus and follow need stable ids and effects

The backend should return stable human ids and effect hints so the UI can focus or track the right entities without parsing free text.

Sprint 10 hard boundary:

- `focus` selects/centers one existing human by backend-owned id
- `compare` returns structured, side-by-side human facts from backend-owned state
- `follow` is bounded by explicit tick count and never injects intervention actions
- any request to force meetings/alter intent remains out of scope and must be refused

### Decision 3: follow workflows must stay bounded

If following a human requires stepping or windowed refresh, the request and response must make the bounds explicit.

### Decision 4: compare outputs should remain structured

Comparison results should expose comparable metrics/facts cleanly rather than only free-form narrative text.

## Deliverables

- guided command semantics for focus, compare, and follow
- backend orchestration support for guided workflows
- any required narrow read-model additions
- UI focus/follow behavior in the console
- validation notes for the guided path

## Definition of done

- focus, compare, and follow commands work through the backend orchestration layer
- UI effects can focus or track humans using stable ids
- follow behavior is explicitly bounded
- guided workflows remain distinct from interventions
- validation notes capture residual risks clearly

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/human/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/features/city/services/`
- `docs/sprints/sprint10/`

## Features and task breakdown

## Feature 1: Guided semantics lock

### Goal

Fix the command semantics so guided workflows are powerful enough to impress but still clearly non-interventionist.

### Acceptance criteria

- a contributor can distinguish guided commands from director commands without guessing
- bounded inputs and outputs are explicit

### Best owner

- You

## Feature 2: Backend guided workflow support

### Goal

Implement backend orchestration and read support for focus, compare, and follow commands.

### Acceptance criteria

- guided workflows are backend-owned and city-scoped
- compare/follow responses expose structured data plus UI effects

### Best owner

- Codex

## Feature 3: Console integration

### Goal

Teach the simulation console to focus, compare, and track humans intentionally.

### Acceptance criteria

- the UI can highlight or track a human based on backend effects
- compare/follow flows feel like part of the same console rather than a side feature

### Best owner

- Cursor chat

## Feature 4: Validation and closeout

### Goal

Prove the guided workflows behave coherently before director commands are attempted.

### Acceptance criteria

- the guided path is validated on the touched boundaries
- deferred intervention work remains explicit

### Best owner

- Codex

## Recommended implementation order

1. Lock guided-command semantics.
2. Add backend orchestration support.
3. Add any narrow read-model helpers required.
4. Integrate guided effects into the console.
5. Validate and close the sprint.

## Dependencies inside the sprint

- Sprint 9 console stability is required first.
- Feature 1 blocks the rest of the sprint.
- Feature 3 depends on stable backend ids/effects from Feature 2.

## Suggested delegation

### Best tasks for you

- approve the boundary between guided commands and interventions

### Best tasks for Cursor chat

- UI tracking/focus integration
- console polish for guided flows

### Best tasks for Codex

- backend orchestration logic
- narrow read-model helpers
- validation

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Guided semantics lock | You | Guided commands are explicit and non-interventionist. |
| Task 2a | Backend focus/compare/follow support | Codex | Guided workflows execute through backend orchestration. |
| Task 2b | Narrow read-model/effect helpers | Codex | Any missing compare/follow support is added without broad redesign. |
| Task 3a | Console integration for guided workflows | Cursor chat | The console can focus/track humans coherently. |
| Task 4a | Validation and closeout | Codex | Guided flows are validated and deferred intervention work is recorded. |

## Risks

- a follow workflow can silently become an intervention if bounds and semantics are vague
- compare outputs may become too narrative if structure is not enforced
- guided UI behavior can get noisy if focus/tracking effects are not intentional

## Handoff to next sprint

Sprint 11 should introduce explicit interventions only after guided observation is stable and clearly distinct.
