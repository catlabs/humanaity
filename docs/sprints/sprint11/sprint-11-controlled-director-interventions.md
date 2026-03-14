# Sprint 11: Controlled Director Interventions

## Execution status

- Current phase: Sprint 11 planned
- Active chunk: `Task 3a` - console confirmation and intervention labeling
- Next chunk: `Task 4a` - validation and closeout
- Blocked items: depends on Sprint 10 guided workflow completion
- Last completed chunk: `Task 2b` - first director command implementation (2026-03-14)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Locked first-command boundary (`DIRECTOR_MEET_HUMANS`), two-step confirmation semantics, and explicit provenance/labeling requirements. |
| Task 2a | done | Added persistent director intervention model, Flyway schema, and token-based confirmation orchestration path with explicit DIRECTOR action labels. |
| Task 2b | done | Implemented `DIRECTOR_MEET_HUMANS` execution after valid token confirmation, with policy checks, persisted provenance status updates, and explicit intervention action labels. |
| Task 3a | planned | Add console confirmation and intervention-labeling behavior. |
| Task 4a | planned | Validate the first intervention flow and close the sprint. |

## Sprint intent

Sprint 11 exists to introduce the first director-style command without damaging HUMANAIty's deterministic simulation philosophy.

This sprint is backend-first and policy-heavy. It should add one explicit intervention path, not a broad "agent can do anything" layer.

## Why this sprint comes next

Directed interventions are the most powerful and most dangerous agent feature in the roadmap. They should only start after safe commands, console consolidation, and guided observation are already working.

If Sprint 11 is skipped:

- the product remains observation-oriented, which is acceptable short term
- but the roadmap still lacks a disciplined answer to "can the user intentionally steer the world?"

If Sprint 11 is rushed:

- intervention commands can blur canonical history and user-authored actions
- deterministic credibility can erode
- the product can look like an unconstrained toy instead of a controlled simulation platform

## Sprint outcome

At the end of Sprint 11, HUMANAIty should support one narrow, explicit director command through the agent console, with visible confirmation, backend policy checks, and intervention provenance that distinguishes user-authored steering from normal autonomous world behavior.

## Sprint scope

### In scope

- lock the intervention policy and user-facing labeling rules
- add an explicit backend intervention model and provenance handling
- implement one first director command only
- require confirmation semantics before executing the intervention
- expose intervention-aware responses and UI labeling in the console
- validate the first intervention path end to end

### Out of scope

- multiple director powers in one sprint
- open-ended world editing
- hidden or implicit interventions
- broad scenario scripting
- broad simulation-rule redesign unrelated to the first intervention path

## Product and technical decisions for this sprint

### Decision 1: interventions are explicit simulation inputs

Sprint 11 must treat director commands as explicit user-authored simulation inputs, not as ordinary reads or normal autonomous world behavior.

### Decision 2: confirmation is mandatory

The first intervention flow should require explicit confirmation semantics before execution.

Sprint 11 confirmation contract:

- first request returns a `DIRECTOR_CONFIRMATION_REQUIRED` response with a generated `confirmationToken` and a short expiry window
- execution is allowed only when the follow-up request includes an explicit confirmation phrase and the matching token
- expired/missing tokens must fail closed with a refusal response and no simulation mutation

### Decision 3: provenance must be visible

The system should be able to say:

- this happened autonomously
- this happened because the user issued an intervention

Sprint 11 provenance contract:

- every executed intervention must persist `cityId`, `tick`, `commandType`, actor ids, initiating `userId`, and `confirmationToken` reference
- responses for executed interventions must include explicit `commandClass=DIRECTOR`, `INTERVENTION_EXECUTED` action labeling, and stable intervention id for traceability

### Decision 4: one narrow first command

Choose one first director command, such as "make these two humans meet", and implement it well.

Do not bundle several intervention types into Sprint 11.

Sprint 11 locked first command:

- command id: `DIRECTOR_MEET_HUMANS`
- intent shape: make two specific human ids meet in the next deterministic tick window
- policy bounds: same city only, distinct ids required, max two actors, no open-ended scripting

## Deliverables

- locked intervention policy and confirmation model
- backend intervention entity/model or equivalent provenance mechanism
- one first director command in the orchestration layer
- UI confirmation and intervention labeling behavior
- validation notes proving the first intervention path works and stays explicit

## Definition of done

- the first director command is visibly labeled as an intervention
- confirmation happens before execution
- provenance is persisted or otherwise queryable from backend-owned state
- normal simulation behavior and intervention behavior remain distinguishable
- the sprint documents what is still deferred after the first intervention lands

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/event/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `docs/sprints/sprint11/`

## Features and task breakdown

## Feature 1: Intervention policy lock

### Goal

Make the policy and product meaning of interventions explicit before code is written.

### Acceptance criteria

- a contributor can tell what makes a command an intervention
- confirmation, provenance, and visible labeling rules are unambiguous

### Best owner

- You

## Feature 2: Backend intervention model and first command

### Goal

Implement the smallest backend structure that supports one real intervention with provenance and policy checks.

### Acceptance criteria

- the backend can execute one intervention through an explicit path
- provenance is preserved and queryable

### Best owner

- Codex

## Feature 3: Console confirmation and labeling

### Goal

Expose the first intervention in the UI without making it look like an ordinary chat command.

### Acceptance criteria

- the user must confirm the intervention before execution
- the console clearly labels the action as a directed intervention

### Best owner

- Cursor chat

## Feature 4: Validation and closeout

### Goal

Verify the first intervention path end to end and record what remains intentionally out of scope.

### Acceptance criteria

- the first intervention path is validated on the touched boundaries
- later intervention ideas remain explicitly deferred

### Best owner

- Codex

## Recommended implementation order

1. Lock intervention policy and confirmation semantics.
2. Add backend intervention model/provenance handling.
3. Implement one first director command.
4. Add UI confirmation and intervention labeling.
5. Validate and close the sprint.

## Dependencies inside the sprint

- Sprint 10 should land first.
- Feature 1 blocks the rest of the sprint.
- Feature 3 depends on the backend command and provenance contract being stable.

## Suggested delegation

### Best tasks for you

- approve the intervention boundary and first-command choice

### Best tasks for Cursor chat

- confirmation UX
- intervention labeling in the console

### Best tasks for Codex

- backend intervention model
- orchestration and provenance logic
- validation

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Intervention policy and contract lock | You | Confirmation, provenance, and non-goals are explicit. |
| Task 2a | Backend intervention model and orchestration path | Codex | One explicit intervention path exists in backend-owned logic. |
| Task 2b | First director command | Codex | One narrow intervention command works with policy checks. |
| Task 3a | Console confirmation and labeling | Cursor chat | The UI treats interventions differently from ordinary commands. |
| Task 4a | Validation and closeout | Codex | The first intervention flow is validated and later ideas are deferred explicitly. |

## Risks

- intervention work can undermine deterministic credibility if provenance is weak
- confirmation can become perfunctory if the UI does not make the stakes explicit
- one first command can still sprawl into a broad control surface without discipline

## Handoff to next sprint

After Sprint 11, any further director work should be incremental and policy-first rather than expanding powers quickly.
