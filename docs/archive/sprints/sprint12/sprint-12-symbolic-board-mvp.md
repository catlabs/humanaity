# Sprint 12: Symbolic Board MVP

## Execution status

- Current phase: Sprint 12 complete
- Active chunk: none
- Next chunk: Sprint 13 `Task 1a` - place and overlay semantics lock
- Blocked items: depends on Sprint 11 completion for full sequencing, but most of the sprint can proceed on top of Sprint 10 plus Sprint 9 console work
- Last completed chunk: `Task 4a` - validation and closeout (2026-03-14)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Locked Sprint 12 as a board-observatory MVP: normalized human markers only, CSS movement transitions only, and no place/overlay/game-engine semantics. |
| Task 2a | done | Audited `SimulationSnapshotOutput`/`SimulationSnapshotHumanOutput`; existing `humans[].{id,name,x,y,busy}` plus run/timeline metrics are sufficient for board MVP, so no backend contract change is required in Sprint 12. |
| Task 2b | done | Added `BoardViewModelService` with deterministic normalized/clamped position mapping and fallback rules, covered by focused unit tests. |
| Task 3a | done | Added reusable `SymbolicBoardComponent` and integrated it into simulation detail as the primary board surface with CSS marker movement transitions. |
| Task 4a | done | UI type checks passed for app/spec configs and board integration; `npm run build` terminated with exit `-1` in this environment and is tracked as residual delivery risk. |

## Sprint intent

Sprint 12 exists to put a real symbolic simulation board on the main city console quickly, without waiting for deeper visualization semantics.

This sprint is intentionally frontend-led. It should deliver visible progress fast by reusing the existing snapshot contracts wherever possible.

## Why this sprint comes next

Sprint 9 established the console-first shell, Sprint 10 added guided human workflows, and Sprint 11 defines the intervention boundary. The next product-facing step is to make the main simulation surface visibly stronger.

If Sprint 12 is skipped:

- the product still relies on a lower-signal simulation presentation
- the console remains more architecturally interesting than visually legible
- later board overlays and chat-driven board reactions would land on an unstable foundation

## Sprint outcome

At the end of Sprint 12, the simulation detail page should show a symbolic board built with Angular, HTML, and CSS, where humans render as positioned markers using normalized coordinates and animate between snapshots with CSS transitions.

## Sprint scope

### In scope

- lock the symbolic board boundary as an observatory UI, not a game engine
- audit the existing snapshot contract against board needs
- add a frontend view-model layer for board rendering
- render humans as symbolic markers on a reusable board component
- keep the existing chat and context surfaces working around the new board
- validate refresh and movement behavior

### Out of scope

- fixed places such as forest, church, or river
- interaction lines or speech overlays
- transient event markers
- new heavy rendering engines
- broad backend redesign if the current snapshot already supports the first board milestone

## Product and technical decisions for this sprint

### Decision 1: symbolic board first

Sprint 12 should prioritize a clean symbolic board over realism. The UI must read like an observatory console, not a map editor or game scene.

Sprint 12 hard boundary:

- humans are rendered as symbolic markers only
- movement is shown only through position interpolation/transition
- no fixed place semantics, no interaction lines, no event markers in this sprint
- no second rendering runtime beside the Angular board surface

### Decision 2: frontend derivation is acceptable when semantics are purely visual

If a board concept can be derived safely from existing snapshot data and does not change canonical meaning, keep it frontend-local for this sprint.

### Decision 3: contract changes must stay minimal

Do not add backend work unless the board genuinely cannot ship with the current `SimulationSnapshotOutput` and `SimulationSnapshotHumanOutput` contracts.

## Deliverables

- a locked board-first Sprint 12 boundary
- a board view-model mapping plan
- a reusable Angular board component for symbolic human rendering
- CSS-based human movement animation between refreshes
- simulation detail page integration notes and validation results

## Definition of done

- the city simulation page has a visible symbolic board
- humans render from backend-owned coordinates rather than mock layout state
- movement between snapshots is animated with CSS transitions
- the chat panel and context surfaces remain usable on the same page
- any missing contract semantics are documented explicitly instead of guessed in the UI

## Suggested file targets

- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/features/city/components/`
- `apps/ui/src/app/features/city/services/`
- `apps/ui/src/app/api/model/`
- `docs/sprints/sprint12/`

## Features and task breakdown

## Feature 1: Board semantics and scope lock

### Goal

Fix the product boundary so Sprint 12 lands one meaningful board milestone rather than a broad visualization rewrite.

### Acceptance criteria

- a contributor can tell what the first board version must show
- game-engine and realism expectations are explicitly excluded

### Best owner

- You

## Feature 2: Contract audit and board view-model plan

### Goal

Determine whether current snapshot contracts are already sufficient and define the mapping layer that will keep rendering logic clean.

### Acceptance criteria

- backend contract gaps, if any, are narrow and explicit
- board state derivation rules are documented before component work starts

### Best owner

- Codex

## Feature 3: Board component and page integration

### Goal

Replace the current low-signal world presentation with a reusable symbolic board component inside the main console.

### Acceptance criteria

- humans render as symbolic markers with normalized positioning
- the main simulation page clearly reads as board + chat + context

### Best owner

- Cursor chat

## Feature 4: Validation and closeout

### Goal

Verify that the board refresh loop works and record any remaining semantic gaps before places and overlays are added.

### Acceptance criteria

- snapshot refresh still drives the visible board correctly
- residual risks are documented cleanly for Sprint 13

### Best owner

- Codex

## Recommended implementation order

1. Lock the board boundary and non-goals.
2. Audit the current snapshot contract.
3. Define the board view model and mapping rules.
4. Build the reusable board component.
5. Integrate the board into the simulation detail page.
6. Validate movement animation and refresh behavior.

## Dependencies inside the sprint

- Feature 1 blocks the rest of the sprint.
- Feature 2 should finish before any board component implementation starts.
- Feature 3 depends on stable mapping rules from Feature 2.
- Feature 4 depends on both component integration and refresh wiring being in place.

## Suggested delegation

### Best tasks for you

- approve the symbolic board scope boundary
- decide whether any missing human state semantics are worth adding now or deferring

### Best tasks for Cursor chat

- Angular board component structure
- page layout adjustments
- CSS transitions and visual hierarchy

### Best tasks for Codex

- contract audit
- view-model mapping design
- validation and narrow contract support if required

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Board semantics and scope lock | You | Sprint 12 is explicitly limited to the symbolic board MVP. |
| Task 2a | Snapshot contract audit | Codex | The board data boundary is explicit and any true contract gaps are identified. |
| Task 2b | Board view-model mapping plan | Codex | Board entities and derivation rules are clear enough for component work. |
| Task 3a | Symbolic board component and page integration | Cursor chat | The city page shows a real board with animated human markers. |
| Task 4a | Validation and closeout | Codex | The board refresh path is validated and Sprint 13 handoff is recorded. |

## Risks

- Sprint 12 can drift into premature place/overlay work if the board boundary is not held firmly
- unclear human-state semantics may tempt the UI to invent meaning instead of documenting the gap
- layout work can become cosmetic churn if the board component contract is not fixed early

## Expected sprint outputs

- visible symbolic board on the main simulation page
- board view-model contract for future sprints
- documented contract decisions about what is backend-owned versus frontend-derived
- handoff notes for places and overlays in Sprint 13

## Handoff to next sprint

Sprint 13 should add places, interactions, and event overlays only after Sprint 12 proves that the symbolic board itself is stable and legible.
