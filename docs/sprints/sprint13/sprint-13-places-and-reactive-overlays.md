# Sprint 13: Places and Reactive Overlays

## Execution status

- Current phase: Sprint 13 complete
- Active chunk: none
- Next chunk: Sprint 14 `Task 1a` - chat-to-board effect contract lock
- Blocked items: depends on Sprint 12 board MVP completion
- Last completed chunk: `Task 4a` - validation and closeout (2026-03-14)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Locked place anchors as non-canonical frontend presentation, fixed interaction/event overlay lifetimes, and explicit rule that overlays cannot invent world meaning. |
| Task 2a | done | Audited board semantics against existing snapshot/timeline contracts; `humans[]`, `recentEvents[].{eventType,actorIds,tick,createdAt}`, and `recentInventions[]` already cover Sprint 13 overlays, so no backend API change is required. |
| Task 2b | done | No contract delta from Task 2a; generated API clients intentionally left unchanged. |
| Task 3a | done | Extended the symbolic board with fixed place anchors and bounded interaction line overlays derived from recent backend timeline events. |
| Task 3b | done | Added transient board event markers with bounded lifetime and synchronized board marker/event selection with existing context panel state. |
| Task 4a | done | UI type checks passed and overlay behavior remains backend-driven; `npm run build` terminated with exit `-1` in this environment and is tracked as residual delivery risk. |

## Sprint intent

Sprint 13 exists to make the symbolic board explainable at a glance by adding fixed places, temporary interaction cues, and short-lived event markers.

This sprint should deepen the board without making the frontend a second simulation engine.

## Why this sprint comes next

Sprint 12 gives the product a visible board. Sprint 13 should now make that board meaningful enough to support observation and storytelling.

If Sprint 13 is skipped:

- the board remains visually cleaner but still semantically thin
- users can see movement but not context
- later chat-driven board reactions would have too little expressive range

## Sprint outcome

At the end of Sprint 13, the board should show fixed symbolic places, temporary interaction overlays between humans, and temporary event markers near affected entities, with all behavior grounded in backend-owned state or clearly documented frontend derivation.

## Sprint scope

### In scope

- lock which board semantics belong to backend state versus frontend derivation
- add minimal read-model changes if current contracts are insufficient
- render fixed places with stable positions
- add simple SVG or HTML interaction overlays
- add transient event markers with bounded lifetime
- sync board selection with context panel and existing backend effects

### Out of scope

- complex pathfinding or navigation systems
- realistic geography or map generation
- dense event choreography or cinematic animation systems
- broad redesign of simulation history semantics

## Product and technical decisions for this sprint

### Decision 1: places are contextual anchors, not navigable terrain

Places exist to help users interpret interactions and movement. They are symbolic anchors, not a simulation of terrain.

### Decision 2: overlays must remain bounded and explainable

Interaction lines and event markers should appear briefly and predictably. The board must not accumulate noisy or long-lived visual state.

Sprint 13 overlay lifetime lock:

- interaction overlays auto-expire after a short bounded window (default 6s)
- transient event markers auto-expire after a short bounded window (default 5s)
- overlays are refreshed from backend-owned snapshot/timeline refresh cycles only; no autonomous UI timer-driven world mutations

### Decision 3: canonical meaning stays in backend-owned data

If a visualization changes product meaning or would need to be shared consistently across UI and MCP, prefer a narrow backend contract addition over frontend guessing.

## Deliverables

- locked place and overlay semantics
- any minimal read-model/API additions required
- fixed place rendering on the board
- temporary interaction and event overlay layers
- synchronized board/context selection behavior
- validation notes on determinism-safe visualization

## Definition of done

- the board shows a stable set of symbolic places
- recent interactions can produce visible temporary overlays
- recent events can produce visible temporary markers
- selection/focus stays coherent between board and context surfaces
- visualization behavior is traceable to backend state/history or explicitly documented derivation

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/event/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/`
- `apps/ui/src/app/features/city/components/`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/features/city/services/`
- `apps/ui/src/app/api/`
- `apps/mcp/src/generated/`
- `docs/sprints/sprint13/`

## Features and task breakdown

## Feature 1: Place and overlay semantics lock

### Goal

Make the board semantics explicit before visual richness increases.

### Acceptance criteria

- a contributor can tell which overlay behavior is canonical and which is presentation-only
- place semantics are stable enough for contract decisions

### Best owner

- You

## Feature 2: Narrow backend/read-model support

### Goal

Add only the minimum backend-owned semantics necessary to support stable places or overlays.

### Acceptance criteria

- any contract additions stay narrow and justified
- frontend rendering no longer needs to guess at critical meaning

### Best owner

- Codex

## Feature 3: Place rendering and reactive overlays

### Goal

Make the board visually explain movement and interaction without sacrificing simplicity.

### Acceptance criteria

- places are visible and stable
- interaction and event overlays are noticeable but bounded

### Best owner

- Cursor chat

## Feature 4: Validation and closeout

### Goal

Confirm that the richer board still respects deterministic state ownership and remains usable.

### Acceptance criteria

- overlay behavior can be explained in terms of existing or newly added backend state
- Sprint 14 can rely on the board as a stable visual feedback surface

### Best owner

- Codex

## Recommended implementation order

1. Lock place and overlay semantics.
2. Add minimal backend/read-model support if required.
3. Regenerate generated clients where contracts changed.
4. Render fixed places.
5. Add interaction overlays.
6. Add transient event markers and selection sync.
7. Validate determinism-safe behavior.

## Dependencies inside the sprint

- Feature 1 blocks the rest of the sprint.
- Feature 2 blocks Feature 3 if a contract change is required.
- Feature 3b depends on the place/overlay layer from Feature 3a.
- Feature 4 depends on both contract decisions and UI behavior being complete.

## Suggested delegation

### Best tasks for you

- approve place taxonomy and overlay meaning
- decide when a semantic gap justifies a backend change instead of frontend derivation

### Best tasks for Cursor chat

- place rendering
- overlay rendering
- board-to-context interaction polish

### Best tasks for Codex

- read-model/API changes
- generated client updates
- determinism-safe validation

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Place and overlay semantics lock | You | Board semantics are explicit enough for stable implementation. |
| Task 2a | Narrow board read-model support | Codex | Any missing canonical semantics are added without broad redesign. |
| Task 2b | Generated client regeneration | Codex | UI and MCP clients match the final board-related API contract. |
| Task 3a | Fixed places and interaction overlays | Cursor chat | The board shows places and temporary human interaction cues. |
| Task 3b | Event markers and selection synchronization | Cursor chat | Event markers appear cleanly and selection stays coherent across surfaces. |
| Task 4a | Validation and closeout | Codex | Visualization remains deterministic-safe and Sprint 14 handoff is clear. |

## Risks

- the sprint can drift into excessive visual effects if the “symbolic and bounded” rule is ignored
- frontend-only derivation may accidentally encode canonical meaning unless contract boundaries stay explicit
- too many concurrent overlays can reduce readability if lifetime and priority rules are not constrained

## Expected sprint outputs

- a semantically richer board with places and overlays
- any minimal board-related API/read-model additions
- synchronized board and context behavior
- clear handoff for chat-driven board reactions in Sprint 14

## Handoff to next sprint

Sprint 14 should wire chat commands into visible board reactions only after Sprint 13 makes the board expressive enough to show those reactions clearly.
