# Sprint 16 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 16 task only.

References for every task:

- `docs/roadmap.md` (Epic 15)
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `docs/specs/history-ledger-spec.md`
- `docs/sprints/sprint16/sprint-16-movement-and-dialogue-from-collision.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 16:

- only movement verification/fix and dialogue-from-collision logic
- do not add place model, chat commands, or discovery category refactor
- keep determinism: same seed + same steps => same events

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: Verify or fix 2D movement so humans do not all stay on one horizontal line.

Acceptance criteria:

- Confirm whether snapshot API returns distinct `y` values for humans after a few steps.
- If backend is correct, check frontend use of `topPct` (board view model and template).
- If a bug is found, apply minimal fix (backend or frontend); otherwise document findings.

In scope: Network/snapshot inspection, coordinate mapping audit, optional minimal fix in `SimulationApplicationService` or `BoardViewModelService` / symbolic-board.

Out of scope: New features, dialogue logic, place model.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: Add dialogue-from-collision: when two humans are within collision threshold and both available and have no recent DIALOGUE_EXCHANGED for that pair, emit DIALOGUE_EXCHANGED.

Acceptance criteria:

- "Recent discussion" is defined (e.g. last 1–3 ticks for that (humanA, humanB) pair) and implemented deterministically (e.g. query event repository by city, event type DIALOGUE_EXCHANGED, actor ids, tick range).
- `buildDialogueDrafts(tick, humans)` added in SimulationApplicationService; for each pair within collision threshold, if both !busy and no recent dialogue for pair, add one EventDraft(EventType.DIALOGUE_EXCHANGED, actorIds, payload, importance, eventKey).
- Event key shape: e.g. DIALOGUE_EXCHANGED:smallerId:largerId:tick. Ordered by id for stability.
- buildDialogueDrafts called from buildStepEventDrafts; drafts emitted via existing eventApplicationService.emitEventsAtTick.
- Ordering: dialogue drafts and collision drafts in same tick must have stable sequenceInTick (history ledger).

In scope: `SimulationApplicationService`, event repository/query for DIALOGUE_EXCHANGED by city and actor pair, `EventDraft`, `buildStepEventDrafts`.

Out of scope: Place model, chat, discovery category changes, PROXIMITY_GROUP.

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal: Confirm DIALOGUE_EXCHANGED events appear in timeline and on board and update sprint execution status.

Acceptance criteria:

- Timeline (history API or UI) shows DIALOGUE_EXCHANGED events when conditions are met.
- Board shows dialogue interaction (existing interaction layer for DIALOGUE_EXCHANGED).
- Sprint 16 execution status block in the sprint doc updated: Task 1a, 2a, 3a marked done; next chunk / last completed set.

In scope: Manual or automated validation, execution status update in `docs/sprints/sprint16/sprint-16-movement-and-dialogue-from-collision.md`.

Out of scope: New UI features, backend logic changes beyond what was done in Task 2a.
