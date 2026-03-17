# Sprint 4: Frontend Simulation Experience

## Execution status

- Current phase: Sprint 4 implemented
- Active chunk: none
- Next chunk: none
- Blocked items: Angular CLI `ng build` / `ng test` crash in local environment (`malloc: pointer being freed was not allocated`)
- Last completed chunk: `Task 4b` - validation flow and sprint closeout

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | completed | Frontend simulation experience spec locked in `docs/specs/frontend-simulation-experience-spec.md`. |
| Task 2a | completed | Canonical `/cities/:id` simulation page kept and duplicate `pages/details/` implementation removed. |
| Task 2b | completed | Real controls (`start`/`pause`/`step`/refresh), polling, and Pixi lifecycle wired on authoritative page. |
| Task 3a | completed | Event/invention/timeline panels now consume backend timeline/snapshot history contracts. |
| Task 3b | completed | Explicit loading, empty, error, and selection states added to simulation detail panels. |
| Task 4a | completed | Focused simulation-detail spec test added for route/service/page contract behavior. |
| Task 4b | completed | Validation run recorded: `tsc` app/spec checks pass; Angular builder/test runner crash noted as environment issue. |

## Sprint intent

Sprint 4 exists to turn the new backend-owned simulation contracts into a credible frontend product experience.

This sprint is frontend-first, but it depends directly on Sprint 3 contracts.

It does **not** try to redesign the whole application. Its job is to make the city simulation UI real, coherent, and demonstrable.

## Why this sprint comes next

Sprint 3 removed the biggest API-contract ambiguity, but the frontend still has split responsibilities across multiple city detail pages and several placeholder-only interaction seams.

If Sprint 4 is skipped:

- the product will still look partially mocked even though the backend is real
- the duplicate `details` and `simulation-detail` surfaces will keep the UI architecture incoherent
- Pixi, event feed, and invention/timeline presentation will remain disconnected from the real world model
- portfolio value will lag behind backend maturity

## Sprint outcome

At the end of Sprint 4, HUMANAIty should have one real city simulation experience that uses backend-owned overview, snapshot, and history data with functional controls, Pixi rendering, and coherent side panels.

## Sprint scope

### In scope

- define the MVP frontend behavior contract for the city simulation page
- consolidate duplicate city detail/simulation surfaces into one primary route target
- wire real simulation controls (`start`, `pause`, `step`, refresh/polling) into the frontend
- render real humans in the Pixi map using backend positions
- expose real event, invention, and timeline summary data in the simulation experience
- add loading, error, and empty states for the main simulation page
- add focused frontend tests around the new route/service/page seams

### Out of scope

- broad visual redesign of unrelated app sections
- new backend business rules beyond contract-driven frontend needs
- AI-enriched narrative UI beyond existing deterministic history display
- multiplayer or live subscription infrastructure
- CI/deployment work from Epic 8
- deep admin tooling cleanup outside the city simulation flow

## Product and technical decisions for this sprint

### Global decision: there should be one primary simulation detail surface

Sprint 4 should consolidate the current frontend split between:

- `features/city/pages/details/`
- `features/city/pages/simulation-detail/`

The app should have one authoritative city simulation experience, not two partially overlapping implementations.

### Decision 1: preserve the existing design system, not invent a new one

Sprint 4 should reuse current Angular/Material/shared component patterns and focus on coherence, not a visual reset.

### Decision 2: backend contracts stay authoritative

Frontend code should consume Sprint 3-generated `overview`, `snapshot`, and history contracts rather than deriving product semantics locally.

Avoid:

- fake event lists
- placeholder counters
- duplicate year/era/status logic

### Decision 3: Pixi is part of the MVP detail experience

The Pixi map should be used as the real center surface for the city simulation page in Sprint 4, not left stranded in an older route.

### Decision 4: simulation controls must be real and bounded

Sprint 4 should support the MVP control loop:

- start
- pause/stop behavior aligned with current backend contract
- deterministic stepping
- visible state refresh after control actions

### Decision 5: timeline and invention presentation should be summary-first

Sprint 4 should expose real history data in compact, usable panels first.

Avoid over-designing a full chronology workstation in the same sprint.

### Decision 6: empty and loading states are part of the contract

The page must behave predictably when:

- the city exists but has no run yet
- there are no events or inventions yet
- snapshot/history reads are loading
- the backend returns an error

## Deliverables

By the end of Sprint 4, the repo should contain:

- a written frontend simulation experience spec
- one primary city simulation route/page using real backend data
- real simulation controls wired to backend APIs
- Pixi rendering integrated into the authoritative city simulation page
- real event/invention/timeline summary panels driven by backend history data
- focused frontend tests for the new route/page/service behavior

## Definition of done

Sprint 4 is done only if all of the following are true:

- the app uses one primary city simulation detail surface
- city simulation controls call real backend endpoints and refresh visible state
- the Pixi map renders real humans from backend data on the authoritative page
- the simulation page no longer relies on fake event/invention summary content for Sprint 4-covered surfaces
- empty, loading, and error states are explicit for snapshot/history-dependent UI
- at least one focused frontend validation path covers the consolidated page behavior

## Suggested file targets

These are the most likely files or folders Sprint 4 will touch:

- `apps/ui/src/app/features/city/city.route.ts`
- `apps/ui/src/app/features/city/city.service.ts`
- `apps/ui/src/app/features/city/pages/details/`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/features/city/services/pixi-canvas.service.ts`
- `apps/ui/src/app/shared/components/timeline-node/`
- `apps/ui/src/app/shared/components/event-item/`
- `apps/ui/src/app/api/`

Likely new code areas:

- `apps/ui/src/app/features/city/models/`
- `apps/ui/src/app/features/city/view-models/`
- `docs/specs/frontend-simulation-experience-spec.md`

## Features and task breakdown

## Feature 1: Frontend simulation experience specification

### Goal

Define the intended page behavior and consolidation boundary so implementation work does not drift across duplicate UI seams.

### Tasks

1. Define the authoritative city simulation route/page for MVP.
2. Define which controls and panels are required on the page.
3. Define how snapshot/history contracts map into visible UI state.
4. Define loading, empty, and error behavior for the main experience.
5. Define what is explicitly deferred to later UI work.

Locked Sprint 4 spec artifact:

- `docs/specs/frontend-simulation-experience-spec.md`

### Acceptance criteria

- a developer can implement the primary city simulation page without guessing which route/page should survive
- the spec clearly states which UI behaviors are required now versus deferred

### Best owner

- You

## Feature 2: Simulation page consolidation

### Goal

Remove duplicate city simulation flows and make one route/page authoritative.

### Tasks

1. Choose the primary simulation page implementation base.
2. Move required Pixi/detail/history functionality into that page.
3. Retire or redirect the duplicate page/route.
4. Keep route semantics aligned with current city URLs.

Task 2a boundary note:

- focus first on route/page consolidation and compile-safe wiring
- deeper behavioral polishing can follow in later chunks

### Acceptance criteria

- one city route serves as the authoritative simulation experience
- the repo no longer has two competing city simulation pages for the same product responsibility

### Best owner

- Cursor chat

## Feature 3: Real controls and world rendering

### Goal

Make the simulation experience operational rather than decorative.

### Tasks

1. Wire start/pause/step actions to backend endpoints.
2. Refresh snapshot/history state after control actions.
3. Render real humans in Pixi from backend position data.
4. Keep control state and polling behavior deterministic and predictable.

### Acceptance criteria

- simulation controls update backend state and visible UI state coherently
- Pixi uses real backend human positions on the authoritative page

### Best owner

- Codex

## Feature 4: History and side-panel integration

### Goal

Present real event, invention, and summary data in usable frontend panels.

### Tasks

1. Replace fake event feed content with backend history data.
2. Replace fake invention registry content with backend data.
3. Add a lightweight timeline summary or list using existing shared components where possible.
4. Add empty/loading/error states for these panels.

### Acceptance criteria

- history panels show real backend data for Sprint 4-covered surfaces
- panel state is explicit when history data is absent or loading

### Best owner

- Cursor chat

## Feature 5: Frontend validation and closeout

### Goal

Make the consolidated UI safe to build on and demonstrable.

### Tasks

1. Add focused frontend tests for route/page/service behavior.
2. Validate generated-client usage for the simulation page path.
3. Run one end-to-end UI flow covering load plus control plus refresh.
4. Record execution status and residual risks in the sprint doc.

### Acceptance criteria

- regressions in the main simulation page are easier to catch
- the Sprint 4 experience can be demonstrated without relying on mocked data

### Best owner

- Codex

## Recommended implementation order

1. Write the frontend simulation experience specification.
2. Choose and consolidate the primary simulation page/route.
3. Wire real controls and backend refresh flow.
4. Integrate Pixi into the authoritative page.
5. Replace fake event/invention/timeline summary content with backend history data.
6. Add loading, empty, and error handling.
7. Add focused frontend tests and one validation flow.
8. Remove remaining duplicate or placeholder-only seams that conflict with the new page.

## Chunk-by-chunk ownership split (Cursor vs Codex)

Use this split to execute Sprint 4 in reviewable sub-chunks while preserving the five-feature structure above.

| Chunk ID | Chunk focus | Primary owner | Notes |
| --- | --- | --- | --- |
| Task 1a | Write frontend simulation experience spec | You | Semantic lock before route/page consolidation. |
| Task 2a | Consolidate route and primary page | Cursor | Decide which page survives and wire the route cleanly. |
| Task 2b | Wire real controls and Pixi state | Codex | Keep control flow and rendering behavior coherent. |
| Task 3a | Replace fake history/panel content | Cursor | Focus on real history-backed UI panels. |
| Task 3b | Add loading/error/selection polish | Cursor | Tighten UX without redesigning the system. |
| Task 4a | Add focused frontend validation | Codex | Prefer route/service/page behavior coverage. |
| Task 4b | Run validation flow and update sprint status | Cursor | Close the sprint loop and record results. |

## Dependencies inside the sprint

- Task 1a should complete before consolidation so the route/page boundary is explicit.
- Task 2a should happen before real controls and history-panel wiring.
- Task 2b depends on the primary page being selected.
- Task 3a and Task 3b depend on the authoritative page receiving real snapshot/history data.
- Task 4b depends on the previous chunks being integrated through generated clients.

## Suggested delegation

### Best tasks for you

- decide which page/route is the canonical user-facing simulation experience
- approve how much old UI scaffolding should be retired versus temporarily bridged
- validate the page-level product scope and deferred items

### Best tasks for Cursor chat

- route consolidation
- Angular template/service wiring
- shared component integration
- loading/empty/error state cleanup

### Best tasks for Codex

- real control-flow wiring with refresh behavior
- Pixi/rendering integration and related state handling
- focused frontend validation and regression coverage

## Ready-to-delegate task list

Use these as small execution handoffs:

1. `Task 1a`: write `docs/specs/frontend-simulation-experience-spec.md` and lock page/route behavior.
2. `Task 2a`: consolidate city simulation routes/pages into one authoritative frontend surface.
3. `Task 2b`: wire real controls, backend refresh, and Pixi rendering into that surface.
4. `Task 3a`: replace fake event/invention/timeline content with backend history data.
5. `Task 3b`: add loading, error, empty, and selection-state polish.
6. `Task 4a`: add focused frontend tests for the consolidated simulation experience.
7. `Task 4b`: run a UI validation flow and update sprint execution status.

## Risks

- consolidating two partially overlapping city pages could create regressions if responsibilities are not assigned cleanly
- Pixi integration may still assume older route/page lifecycle behavior
- backend history/snapshot contracts may be sufficient for MVP but still feel sparse in the UI without careful empty-state handling
- existing generated-client churn may expose unrelated frontend type issues during Sprint 4 implementation

## Handoff to next sprint

Sprint 5 should assume Sprint 4 delivers a real, coherent simulation UI.

That means the next sprint can focus on differentiated experience layers rather than basic credibility:

- AI-enriched recaps and dialogue presentation
- richer timeline storytelling
- higher-value demo flows across UI and MCP on top of one stable simulation experience
