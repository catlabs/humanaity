# Sprint 22: Human Goals

## Execution status

- Current phase: Planned
- Active chunk: none
- Next chunk: Task 1a
- Blocked items: none
- Last completed chunk: none

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | todo | Introduce goal domain model and persistence ownership. |
| Task 2a | todo | Integrate goals into deterministic movement and completion rules. |
| Task 3a | todo | Route chat commands to goal assignment instead of ad hoc position mutation where applicable. |

## Sprint intent

Sprint 22 replaces random or chat-immediate movement with a deterministic goal model so humans can pursue places, people, and bounded intentions across multiple ticks.

## Why this sprint comes next

Sprint 21 gives the backend a clean structured command path. The next missing layer is durable simulation intent: commands should assign goals, and the simulation should execute those goals deterministically over time.

## Sprint outcome

At the end of Sprint 22, humans can hold active goals, move toward place or human targets, wander when idle, complete goals when conditions are met, and avoid edge-sticking behavior.

## Sprint scope

### In scope

- Add a goal model with types, status, target fields, and assignment provenance.
- Teach the simulation step loop to prioritize goal-driven movement over idle wandering.
- Support deterministic goal completion for `MOVE_TO_PLACE`, `MEET_HUMAN`, and `FOLLOW_HUMAN`.
- Emit goal-assigned and goal-completed events or equivalent history signals.
- Update command execution so movement-oriented chat commands assign goals instead of directly teleporting humans where that would break simulation semantics.

### Out of scope

- Full structure-building implementation for `BUILD_STRUCTURE`.
- Knowledge progression or application unlocks.
- Tribe-level coordination behavior.

## Product and technical decisions for this sprint

- **Decision 1:** Goals are canonical backend state and may be assigned by chat, director flows, or autonomous rules.
- **Decision 2:** Place and human ids are the primary movement targets; free-form coordinates are implementation detail, not chat-level semantics.
- **Decision 3:** Idle wandering remains available, but its movement budget should be smaller and lower priority than active goals.

## Deliverables

- Goal domain model and persistence approach.
- Deterministic goal-driven movement with completion logic.
- Chat command integration for goal assignment.

## Definition of done

- A human can receive `MOVE_TO_PLACE`, `MEET_HUMAN`, or `FOLLOW_HUMAN` and progress toward that target over multiple ticks.
- Idle humans still move slowly when no goal is active.
- Goal completion is deterministic and recorded.
- Humans do not get stuck at map edges due to movement clamping or boundary logic.

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/domain/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/human/domain/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/application/`
- `docs/specs/chat-goals-tech-tree-spec.md`

## Features and task breakdown

### Feature 1: Goal model

**Goal:** Represent durable simulation intentions as canonical backend state.

**Acceptance criteria:** Goal types, status, target fields, and assignment provenance defined; ownership and persistence clear.

**Best owner:** Codex.

### Feature 2: Goal-driven movement and completion

**Goal:** Move humans toward targets deterministically and complete goals when satisfied.

**Acceptance criteria:** Goal-driven movement integrated into step loop; idle wandering preserved; completion deterministic; edge handling safe.

**Best owner:** Codex.

### Feature 3: Goal assignment from commands

**Goal:** Structured chat commands assign goals rather than bypassing the simulation model.

**Acceptance criteria:** Relevant commands create or update goals; response explains assigned goal; UI refresh still works.

**Best owner:** Codex.

## Recommended implementation order

1. Task 1a: Goal model and persistence ownership.
2. Task 2a: Deterministic movement, progress, and completion rules.
3. Task 3a: Goal assignment wiring from chat commands.

## Dependencies inside the sprint

- Task 2a depends on Task 1a.
- Task 3a depends on Task 1a and should follow Task 2a for final semantics.

## Suggested delegation

- **Codex:** Goal model, movement rules, integration tests, command wiring.
- **You:** Approve which commands should assign goals versus remain immediate reads or interventions.

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Goal domain model | Codex | Canonical goal state exists with type, status, target, and provenance. |
| Task 2a | Goal-driven movement + completion | Codex | Humans pursue goals over ticks, wander when idle, and complete goals deterministically. |
| Task 3a | Command-to-goal assignment | Codex | Relevant chat commands assign goals instead of direct ad hoc state mutation. |

## Risks

- If goal persistence is underspecified, later knowledge and tribe systems will not compose cleanly.
- Replacing direct movement too aggressively could break existing demo flows; keep compatibility explicit.

## Handoff to next sprint

Sprint 23 will add the deterministic knowledge graph so discoveries and inventions can unlock stable applications rather than remaining flat event outputs.
