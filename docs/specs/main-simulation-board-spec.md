# Main Simulation Board Spec

## Status

- Status: ACTIVE and implemented for the milestone-era authoritative simulation page
- Scope anchor: `docs/archive/sprints/sprint15/sprint-15-main-simulation-board-simplification.md`
- Applies to: the authoritative `/cities/:id` simulation page in `apps/ui`
- Historical note: `docs/specs/frontend-simulation-experience-spec.md` remains a Sprint 4 historical artifact and does not govern the current authoritative main-page direction

## Purpose

This document fixes the page-boundary and rendering semantics for the authoritative main simulation page.

If implementation choices conflict with milestone planning or legacy sprint history, this spec is the source of truth for the current page contract.

## Primary Product Goal

The authoritative simulation page should deliver one visibly readable workflow that:

- loads real snapshot data from the backend
- renders humans clearly as markers on a dominant board
- keeps selected-human state and simulation status visible beside the board
- shows recent event context in the same page workspace
- uses a right-rail command console as the primary deterministic control surface
- makes command outcome, state change, event history, and narration read as one coherent product flow
- avoids heavy rendering-engine ownership on the authoritative main page

## Authoritative Page Rule

The city simulation route must have one primary board surface.

Rules:

- the main page must not keep PixiJS and the lightweight symbolic board in parallel on the same authoritative surface
- PixiJS references must be removed from the authoritative main-page rendering path for the active milestone flow
- if legacy PixiJS code remains elsewhere in the repo, it is non-authoritative and out of the main-page rendering path
- the board must be the visually dominant element on the page

## Required Page Sections

The primary simulation page must include:

- a compact city/status header
- one dominant symbolic board surface
- a right-side command/context rail for commands, selected-human context, recent activity, and discoveries
- compact status details such as tick, year, run state, and human count

Panels may be visually reduced, but the board and right-side command/context rail must remain legible as one coherent page flow.

## Data Ownership Rules

### Backend-owned state

The backend snapshot is the source of truth for:

- city identity
- run status
- tick, year, and era
- humans and their coordinates
- human count and aggregate metrics already present in the snapshot

Frontend code must not invent alternative human positions or maintain a parallel simulation state.

### Allowed frontend derivation

Frontend code may apply a deterministic mapping layer only for:

- clamping raw coordinates into board-safe render bounds
- fallback placement when coordinates are absent or invalid
- static placement of symbolic non-canonical places such as forest, river, church, campfire, and house

This derivation must not change canonical simulation meaning.

## Contract Audit Outcome

Audit result: no backend snapshot API change is required for the authoritative board flow.

Contract evidence:

- backend step updates clamp positions in-domain to `[0, 1]` (`SimulationApplicationService.updateHumanPosition`)
- snapshot output passes `human.x` and `human.y` through directly (`SimulationController.toSnapshotOutput`)
- generated UI contract keeps `x` and `y` optional numeric fields (`SimulationSnapshotHumanOutput`)

Frontend mapping decision:

- keep deterministic `value * 100` percentage mapping with clamp-to-board bounds
- keep deterministic index-based fallback when coordinates are missing or non-finite
- do not introduce additional frontend-only state or inferred movement semantics

## Board Rendering Rules

### Humans

- humans render as simple absolutely positioned markers
- marker positions come from backend snapshot coordinates after deterministic mapping only
- marker movement must animate through CSS transitions when coordinates change after refresh or step
- human markers must remain readable without requiring a heavy graphics engine

### Places

- places are symbolic readability anchors only
- places are fixed and non-canonical on the main page
- places must not imply terrain simulation, pathfinding, or new world semantics

### Overlays

- lightweight overlays such as event markers or place highlights may appear when driven by backend or deterministic UI state
- overlays must remain secondary to the board and must not dominate the main page surface

## Control Rules

The authoritative page is command-first, with deterministic commands as the primary control path.

Rules:

- refresh and deterministic step remain available on the authoritative page
- the command console should live at the top of the right-side context rail as the primary deterministic control surface
- the authoritative page should submit exact commands through `POST /api/simulations/{cityId}/commands` rather than the legacy agent-chat endpoint
- command-related labels should reduce agent-first framing and clarify the deterministic action path
- visible state must refresh from backend data after control actions
- backend command feedback should be displayed explicitly rather than inferred from conversational agent responses
- the page may summarize the command -> simulation -> events -> narration flow explicitly when that helps demo readability

## Layout Rules

- the page should read as board first, context second, commands third
- use a stable header-plus-workspace layout: compact top status/header, dominant board surface, right-side command/context rail
- the board surface should not carry an internal title bar; era/year context belongs in the page header
- the right-side command/context rail should be the primary vertical scroll container on desktop
- avoid multi-panel competition around the board
- preserve mobile viability, but do not sacrifice the board-first hierarchy to keep every previous panel visible
- the board container must fill remaining vertical space through a flex layout chain; do not rely on `height: 100%` percentage inheritance where parent height may be indefinite

## Inspector and Feed Rules

- selected-human state should remain visible without requiring drawer navigation
- the right rail should combine commands, inspection, recent event context, and discoveries into one readable story
- timeline/history endpoints remain the source of truth for event feed content
- board event markers and feed entries should be visibly linked through shared selection/highlight behavior
- newly arrived timeline entries after a command-triggered refresh should be called out explicitly in the right rail
- narration shown in feed/discovery surfaces must come directly from backend enrichment fields and label ready/fallback/unavailable states explicitly
- canonical facts should stay readable even when narration is absent or fallback-generated
- one focused narrative summary area may surface the currently selected or freshest event/discovery, provided canonical facts remain visible
- the right rail should stay secondary to the board, but primary over legacy auxiliary cards

## Empty, Loading, and Error Rules

### No run yet

The page must still render a stable board area and clearly communicate that no run exists yet.

### No humans

The board area must remain stable and show an explicit empty state.

### Loading

Loading feedback must be visible without displacing the board.

### Error

Snapshot/control failures must be visible and recoverable.

## Explicitly Out of Scope

- broad product redesign
- new backend simulation features
- realistic map rendering
- parallel PixiJS and HTML board ownership on the main page
- new frontend-only simulation semantics

## Done Signals for the Main Board Flow

The authoritative page is aligned with this spec only if:

- the main simulation page shows one symbolic board as the dominant surface
- humans render from real backend snapshot coordinates
- selected-human state and core simulation status stay visible without extra navigation
- recent activity has a clear right-rail home on the main page
- the page exposes a recognizable right-rail command console at the top of the context column
- the command-console path uses deterministic backend command execution as the primary control surface
- PixiJS is out of the main page surface and authoritative page references
