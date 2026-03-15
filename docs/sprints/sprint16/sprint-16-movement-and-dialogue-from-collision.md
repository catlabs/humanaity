# Sprint 16: Movement and Dialogue from Collision

## Execution status

- Current phase: In progress
- Active chunk: Task 2a
- Next chunk: Task 3a
- Blocked items: none
- Last completed chunk: Task 1a (2026-03-15)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Verified backend updates both x/y per tick and frontend board mapping uses y->topPct with deterministic clamping; no movement fix required in this chunk. |
| Task 2a | pending | Add recent-discussion state and buildDialogueDrafts. |
| Task 3a | pending | Confirm DIALOGUE_EXCHANGED in timeline and board. |

## Sprint intent

Sprint 16 starts the rule-based event and discovery system (Epic 15). It ensures visible 2D movement is correct and adds the first new domain event from the rule matrix: DIALOGUE_EXCHANGED when two humans collide and both are available with no recent discussion between the same pair.

## Why this sprint comes next

Sprint 15 delivered the simplified symbolic board. The product baseline in `docs/specs/event-discovery-rule-matrix-spec.md` requires dialogue to emerge from simulation rules (collision + conditions), not from chat. Implementing this first unblocks later place-aware and chat-command work.

## Sprint outcome

At the end of Sprint 16, the simulation produces DIALOGUE_EXCHANGED events when collision and conditions are met; 2D movement is verified or fixed; and the timeline and board show dialogue events correctly.

## Sprint scope

### In scope

- Verify or fix 2D movement (snapshot y values and/or frontend layout) so humans do not all stay on one horizontal line.
- Add "recent discussion" state: per (humanA, humanB) last DIALOGUE_EXCHANGED tick (from event history or in-memory).
- Add `buildDialogueDrafts` in SimulationApplicationService: for pairs within collision threshold, if both available and no recent DIALOGUE_EXCHANGED for that pair, emit DIALOGUE_EXCHANGED with deterministic event key and actor ids.
- Call `buildDialogueDrafts` from `buildStepEventDrafts` and emit drafts via existing event service.
- Confirm DIALOGUE_EXCHANGED events appear in timeline and board (existing interaction/dialogue visualization).

### Out of scope

- Place model or REACHED_PLACE.
- Chat commands (go to place, show events).
- Discovery category refactor or complementary-traits logic.
- PROXIMITY_GROUP or STAYED_AT_PLACE.

## Product and technical decisions for this sprint

- **Decision 1:** Collision is the trigger; DIALOGUE_EXCHANGED is the domain event. Conditions: both humans available (not busy), no recent discussion for that pair (e.g. within last 1–3 ticks or from persisted events).
- **Decision 2:** Recent-discussion check may query event repository by city and event type DIALOGUE_EXCHANGED, filtering by actor pair and tick window; or maintain a bounded in-memory state per run. Must remain deterministic (seed + tick + human ids).
- **Decision 3:** Event key shape for dialogue: e.g. `DIALOGUE_EXCHANGED:humanAId:humanBId:tick` (ordered by id) to match history ledger and avoid duplicates.

## Deliverables

- 2D movement verified or fixed (diagnosis + minimal fix).
- buildDialogueDrafts implemented and wired into step.
- DIALOGUE_EXCHANGED events visible in API timeline and on board.

## Definition of done

- Snapshot API returns distinct y values for humans after steps (or root cause of horizontal-line issue documented and fixed).
- When two humans are within collision threshold and both available and have no recent dialogue, a DIALOGUE_EXCHANGED event is emitted at that tick.
- Timeline and board show dialogue events (existing UI for DIALOGUE_EXCHANGED).
- Same seed + same steps produce same dialogue events (determinism).

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/event/` (repository/query for recent dialogue by pair)
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `docs/specs/history-ledger-spec.md` (event key, ordering)
- `apps/ui/` (only if 2D movement fix is frontend)

## Features and task breakdown

### Feature 1: 2D movement verification or fix

**Goal:** Ensure humans move in both dimensions on the board.

**Acceptance criteria:** Snapshot response has distinct y for humans after a few steps; or frontend correctly uses topPct from y; any bug fixed with minimal change.

**Best owner:** Codex or Cursor (diagnosis first).

### Feature 2: Dialogue from collision (backend)

**Goal:** Emit DIALOGUE_EXCHANGED when collision + both available + no recent discussion for that pair.

**Acceptance criteria:** buildDialogueDrafts added; recent-discussion check deterministic; event key and actor ids correct; drafts emitted in same tick as collision drafts, ordering consistent with history ledger.

**Best owner:** Codex.

### Feature 3: Validation and visibility

**Goal:** Confirm dialogue events in timeline and on board.

**Acceptance criteria:** Integration or manual check shows DIALOGUE_EXCHANGED in timeline; board shows dialogue interaction (existing line/overlay); determinism test if applicable.

**Best owner:** Cursor or You.

## Recommended implementation order

1. Task 1a: Verify 2D movement (API + FE); fix if needed.
2. Task 2a: Implement recent-discussion check and buildDialogueDrafts; wire into buildStepEventDrafts and emit.
3. Task 3a: Validate dialogue events in timeline and board; update execution status.

## Dependencies inside the sprint

- Task 1a is independent.
- Task 2a depends on event repository/query for DIALOGUE_EXCHANGED by city and actor pair (or in-memory state design).
- Task 3a depends on Task 2a.

## Suggested delegation

- **You:** Accept design for recent-discussion window (ticks or count).
- **Cursor:** 2D movement frontend check/fix; validation of timeline/board.
- **Codex:** buildDialogueDrafts, recent-discussion logic, event key, integration with step.

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | 2D movement verification or fix | Codex / Cursor | Snapshot has distinct y and/or FE uses it correctly; horizontal-line issue resolved or documented. |
| Task 2a | buildDialogueDrafts and recent-discussion check | Codex | DIALOGUE_EXCHANGED drafts emitted when conditions met; deterministic; event key and ordering correct. |
| Task 3a | Confirm DIALOGUE_EXCHANGED in timeline and board | Cursor / You | Timeline and board show dialogue events; execution status updated. |

## Risks

- Recent-discussion window too large or too small may flood or starve dialogue events; start conservative (e.g. 1–3 ticks).
- Event ordering: DIALOGUE_EXCHANGED must have stable sequenceInTick relative to HUMANS_COLLIDED when both fire same tick.

## Handoff to next sprint

Sprint 17 will add chat commands (MOVE_HUMAN_TO_PLACE, SHOW_EVENTS_BY_TYPE) and the events drawer. Dialogue events from Sprint 16 will be visible in the "show events by type" flow once that is implemented.
