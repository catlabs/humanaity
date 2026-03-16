# Sprint 22 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 22 task only.

References for every task:

- `docs/roadmap.md` (Epics 16-19)
- `docs/specs/chat-goals-tech-tree-spec.md`
- `docs/sprints/sprint22/sprint-22-human-goals.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 22:

- goals are canonical backend state
- simulation executes goals deterministically
- chat assigns goals but does not execute them via LLM inside the turn loop

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: introduce a goal model that can support deterministic execution across simulation ticks.

Acceptance criteria:

- Add canonical goal representation with at least `goalId`, `humanId`, `goalType`, `status`, `assignedTick`, and target payload fields.
- Support the first goal types: `MOVE_TO_PLACE`, `MEET_HUMAN`, and `FOLLOW_HUMAN`.
- Define provenance for goal assignment such as `CHAT_COMMAND`, `AUTONOMOUS`, or `DIRECTOR_INTERVENTION`.
- Clarify persistence ownership and add targeted tests for creation/update semantics.

In scope: backend domain model, persistence wiring, tests.

Out of scope: movement execution, knowledge system.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: integrate goals into deterministic movement and completion rules.

Acceptance criteria:

- The step loop checks active goals before idle wandering.
- `MOVE_TO_PLACE` advances a human toward the target place; `MEET_HUMAN` and `FOLLOW_HUMAN` resolve target positions deterministically.
- Goal completion conditions are explicit and recorded.
- Boundary handling prevents edge-sticking or invalid coordinates.
- Add deterministic regression tests for goal execution.

In scope: simulation application service, movement helpers, tests.

Out of scope: chat wiring, new knowledge progression.

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal: route movement-oriented structured commands to goal assignment rather than direct ad hoc state mutation.

Acceptance criteria:

- Relevant commands such as `MOVE_TO_PLACE`, `MEET_HUMAN`, and `FOLLOW_HUMAN` create or update a goal for the target human.
- Orchestration response clearly summarizes the assigned goal.
- Existing refresh/focus flows still work after goal assignment.
- Commands that should remain immediate reads or explicit interventions are not converted silently into goals.

In scope: backend orchestration integration, response messaging, tests.

Out of scope: UI redesign, knowledge unlocks.
