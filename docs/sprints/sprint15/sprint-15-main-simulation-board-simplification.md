# Sprint 15: Main Simulation Board Simplification

## Execution status

- Current phase: In progress
- Active chunk: `Task 2a` - snapshot coordinate contract audit
- Next chunk: `Task 3a` - minimal simulation board component
- Blocked items: none
- Last completed chunk: `Task 1a` - authoritative-page simplification and board boundary lock (2026-03-15)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Locked one authoritative symbolic board surface for the main route, removed PixiJS provider wiring from simulation detail tests, and marked Sprint 4 Pixi spec as historical/superseded for authoritative-page ownership. |
| Task 2a | pending | Audit the current snapshot coordinate contract and confirm whether deterministic frontend clamping/fallback mapping is sufficient. |
| Task 3a | pending | Introduce or consolidate one lightweight board component and render visible human markers from real backend data. |
| Task 3b | pending | Simplify the page layout so the board dominates and only minimal controls/status remain visible. |
| Task 4a | pending | Validate that refresh and deterministic step actions visibly move at least some humans on the main board. |

## Sprint intent

Sprint 15 exists to stop adding visual and layout complexity to the main simulation page and deliver one minimal board that visibly works.

This sprint is corrective rather than expansive. It should remove ambiguity from the flagship page rather than add new simulation features.

## Why this sprint comes next

Sprints 12 through 14 established the symbolic board direction, places/overlays, and board-aware chat reactions. The current user feedback shows that the authoritative page still feels too crowded and does not yet communicate movement clearly enough.

If Sprint 15 is skipped:

- the flagship page continues to compete with itself visually
- the product keeps carrying Pixi-era expectations on the main route
- movement remains harder to perceive than it should be for a portfolio-critical demo

## Sprint outcome

At the end of Sprint 15, the main city simulation page should show one symbolic HTML/CSS board where humans render from real backend snapshot coordinates and visibly move after refresh or deterministic step actions, with the page simplified around that board.

## Sprint scope

### In scope

- remove or disable the PixiJS-based simulation surface from the authoritative main simulation page
- remove remaining PixiJS references that imply the authoritative main page is still Pixi-owned
- introduce or consolidate one lightweight Angular board component for that page
- render humans as clear positioned markers from backend snapshot coordinates
- use deterministic frontend mapping only for clamping/fallback when coordinates are absent or unsafe
- add a small fixed set of symbolic places for readability
- simplify the page so the board is the primary visible element
- keep only minimal controls and compact status/debug information
- validate visible movement after refresh or step

### Out of scope

- broad redesign of the whole product
- new simulation mechanics or backend features unless a real snapshot contract gap is discovered
- deeper chat workflow redesign
- heavy graphics engines or parallel rendering runtimes on the main page
- large new side panels, inspectors, or timeline workstations

## Product and technical decisions for this sprint

### Decision 1: one authoritative board surface

Sprint 15 hard boundary:

- the main city simulation page has one board surface
- PixiJS does not remain in parallel on that authoritative page
- PixiJS references do not remain in the authoritative main-page path
- if legacy PixiJS code remains in the repo, it is non-authoritative for this sprint

### Decision 2: backend coordinates remain authoritative

Human positions come from backend snapshot data. Frontend logic may only clamp or deterministically fall back when a coordinate is absent or invalid.

### Decision 3: simplicity outranks expressive overlays

Places may remain as fixed symbolic anchors, but overlays, reactive effects, and secondary panels must yield if they make human movement harder to read.

## Deliverables

- locked authoritative-page boundary for the main simulation board
- locked main-board spec for page ownership and data/render semantics
- one sprint doc for the corrective implementation slice
- one prompt pack for delegable execution

## Definition of done

- opening the main simulation page shows one symbolic board as the primary element
- humans render as visible markers from real backend snapshot data
- refresh or deterministic step actions visibly move at least some markers
- the PixiJS surface and authoritative PixiJS references are out of the main page rendering path
- the page keeps only minimal controls and compact status information
- any frontend coordinate mapping is documented and deterministic

## Suggested file targets

- `docs/roadmap.md`
- `docs/specs/main-simulation-board-spec.md`
- `docs/sprints/sprint15/`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/features/city/components/`
- `apps/ui/src/app/features/city/services/`

## Features and task breakdown

## Feature 1: Authoritative page boundary lock

### Goal

Stop the main simulation page from carrying multiple competing rendering directions.

### Acceptance criteria

- the sprint makes it explicit that the main route owns one board surface only
- PixiJS is clearly out of the authoritative page path for this iteration
- authoritative references no longer describe the page as Pixi-owned

### Best owner

- You

## Feature 2: Snapshot coordinate contract audit

### Goal

Confirm whether existing backend snapshot coordinates already support the simplified board without new API work.

### Acceptance criteria

- coordinate ownership is explicit
- any deterministic frontend mapping is narrow and documented
- any true contract gap is identified before UI work begins

### Best owner

- Codex

## Feature 3: Minimal board and page simplification

### Goal

Make one readable main board where movement is obvious and the rest of the page stops competing with it.

### Acceptance criteria

- humans are easy to see on the board
- movement is visible after step or refresh
- symbolic places improve readability without implying deeper simulation meaning
- the page is cleaner than before

### Best owner

- Cursor chat

## Feature 4: Validation and closeout

### Goal

Prove that the simplified board actually works with real backend data and record any remaining blockers cleanly.

### Acceptance criteria

- visible movement is confirmed with real snapshot updates
- any coordinate-mapping caveat is documented explicitly
- residual layout or data risks are called out without expanding scope

### Best owner

- Codex

## Recommended implementation order

1. Lock the authoritative-page simplification boundary.
2. Lock the main-board spec and coordinate ownership rules.
3. Audit the existing snapshot contract and coordinate mapping needs.
4. Build or consolidate the lightweight board component.
5. Simplify the simulation page layout around that board.
6. Validate visible movement with refresh and deterministic step actions.

## Dependencies inside the sprint

- Feature 1 blocks the rest of the sprint.
- Feature 2 should finish before any board implementation starts.
- Feature 3 depends on the contract and page-boundary decisions from Features 1 and 2.
- Feature 4 depends on both board rendering and page simplification being complete.

## Suggested delegation

### Best tasks for you

- approve the hard boundary that PixiJS leaves the authoritative main page and its authoritative references
- decide whether any secondary panel must remain visible despite the simplification

### Best tasks for Cursor chat

- lightweight board component structure
- page simplification and visual hierarchy
- CSS transitions for marker movement

### Best tasks for Codex

- snapshot contract audit
- deterministic coordinate mapping review
- validation and closeout notes

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Authoritative-page boundary lock | You | The main route is explicitly limited to one symbolic board surface for Sprint 15 and no authoritative PixiJS ownership references remain. |
| Task 2a | Snapshot coordinate contract audit | Codex | Coordinate ownership and any narrow mapping rules are explicit before UI work starts. |
| Task 3a | Minimal simulation board component | Cursor chat | The page renders visible human markers from real backend snapshot data. |
| Task 3b | Simulation page simplification | Cursor chat | The board becomes the dominant page element with only minimal controls/status. |
| Task 4a | Validation and closeout | Codex | Visible movement after refresh/step is confirmed and any remaining risks are documented. |

## Risks

- the sprint can drift into another redesign cycle instead of a corrective simplification pass
- existing overlay or chat-related UI may reintroduce clutter if the board-first rule is not enforced
- stale PixiJS references in docs or page-level code can keep follow-up implementation ambiguous even after the board is simplified
- coordinate issues may be masked by ad hoc frontend behavior unless deterministic mapping rules stay explicit

## Handoff to next sprint

Once Sprint 15 lands, any later board polish should start from the simplified authoritative page rather than reintroducing multiple competing surfaces or rendering runtimes.
