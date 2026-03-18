# Main Simulation Board Spec

## Status

- Status: ACTIVE for M1 Simulation UI planning
- Scope anchor: `docs/archive/sprints/sprint15/sprint-15-main-simulation-board-simplification.md`
- Applies to: the authoritative `/cities/:id` simulation page in `apps/ui`
- Historical note: `docs/specs/frontend-simulation-experience-spec.md` remains a Sprint 4 historical artifact and does not govern the current authoritative main-page direction

## Purpose

This document fixes the page-boundary and rendering semantics for the authoritative main simulation page as M1 planning proceeds.

If implementation choices conflict with milestone planning or legacy sprint history, this spec is the source of truth for the current page contract.

## Primary Product Goal

The authoritative simulation page should deliver one visibly readable workflow that:

- loads real snapshot data from the backend
- renders humans clearly as markers on a dominant board
- keeps selected-human state and simulation status visible beside the board
- shows recent event context in the same page workspace
- reserves a bottom command-console area for deterministic control flows
- avoids heavy rendering-engine ownership on the authoritative main page

## Authoritative Page Rule

The city simulation route must have one primary board surface.

Rules:

- the main page must not keep PixiJS and the lightweight symbolic board in parallel on the same authoritative surface
- PixiJS references must be removed from the authoritative main-page rendering path for Sprint 15
- if legacy PixiJS code remains elsewhere in the repo, it is non-authoritative and out of the main-page rendering path
- the board must be the visually dominant element on the page

## Required Page Sections

The primary simulation page must include:

- a compact city/status header
- one dominant symbolic board surface
- a right-side inspector/feed rail for selected-human context and recent activity
- a bottom command-console area
- compact status details such as tick, year, run state, and human count

Panels may be visually reduced, but the board, right rail, and command-console zones must remain legible as one coherent page flow.

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

## Sprint 15 Task 2a Contract Audit Outcome

Audit result: no backend API change is required for Sprint 15.

Contract evidence:

- backend step updates clamp positions in-domain to `[0, 1]` (`SimulationApplicationService.updateHumanPosition`)
- snapshot output passes `human.x` and `human.y` through directly (`SimulationController.toSnapshotOutput`)
- generated UI contract keeps `x` and `y` optional numeric fields (`SimulationSnapshotHumanOutput`)

Frontend mapping decision for Sprint 15:

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
- places are fixed and non-canonical in Sprint 15
- places must not imply terrain simulation, pathfinding, or new world semantics

### Overlays

- interaction lines, event markers, or other reactive overlays are not required in Sprint 15
- if any existing overlay code remains in the codebase, it must not dominate the main page surface

## Control Rules

M1 keeps the authoritative page command-framed without pulling all of M2 forward.

Rules:

- refresh and deterministic step remain available on the authoritative page
- the bottom interaction area should read as the future command-console surface even if it still uses transitional controls
- command-related labels should reduce agent-first framing and clarify the deterministic action path
- visible state must refresh from backend data after control actions

## Layout Rules

- the page should read as board first, context second, commands third
- use a stable three-zone layout: compact top status/header, board-plus-right-rail workspace, bottom command-console area
- avoid multi-panel competition around the board
- preserve mobile viability, but do not sacrifice the board-first hierarchy to keep every previous panel visible
- the board container must fill remaining vertical space through a flex layout chain; do not rely on `height: 100%` percentage inheritance where parent height may be indefinite

## Inspector and Feed Rules

- selected-human state should remain visible without requiring drawer navigation
- the right rail should combine inspection and recent event context into one readable story
- timeline/history endpoints remain the source of truth for event feed content
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

## Done Signals for M1

M1 is aligned with this spec only if:

- the main simulation page shows one symbolic board as the dominant surface
- humans render from real backend snapshot coordinates
- selected-human state and core simulation status stay visible without extra navigation
- recent activity has a clear right-rail home on the main page
- the page exposes a recognizable bottom command-console area
- PixiJS is out of the main page surface and authoritative page references
