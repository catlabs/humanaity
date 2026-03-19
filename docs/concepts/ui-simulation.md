# UI simulation

## Scope
Primary simulation UI experience for the authoritative city simulation page.

## Covers
- board-first layout with the simulation board as the dominant surface
- a right-side command/context rail for commands, human state, recent activity, and discoveries
- snapshot- and timeline-driven rendering from backend-owned state
- full-height board container behavior so the world surface fills available vertical space on the authoritative page

## Active layout notes
- The authoritative `/cities/:id` page should read as a compact status/header above a two-column workspace: one dominant board surface and one right-side command/context rail.
- The board remains the dominant surface and should read as a pure map area without an internal title bar or competing scroll container.
- The right rail should keep the primary deterministic command console at the top, then flow into simulation status, selected-human context, recent activity, and discoveries in one readable scrollable column.
- The primary command console should submit exact deterministic commands through `POST /api/simulations/{cityId}/commands`.
- Deterministic command feedback should be rendered directly from backend command responses rather than inferred from AI narration or best-effort client interpretation.
- Recent activity entries and board event markers should stay visibly linked so selecting either surface makes the latest simulation delta easier to read.
- Event and discovery narration should remain supplemental to canonical facts, with explicit ready/fallback/unavailable states rendered directly from backend enrichment fields.
- The simulation board container should fill remaining page height via flex layout (`flex: 1` + `min-height: 0`) rather than relying on percentage-height inheritance chains.
- Empty/loading overlays should be layered within the same board container so they do not collapse or displace the world surface.
- Legacy agent-chat affordances may remain elsewhere in the repo for historical or secondary flows, but they are not the authoritative command path for this page.

## Source docs
- `docs/specs/main-simulation-board-spec.md`
- `docs/specs/simulation-read-model-spec.md`
- `docs/specs/deterministic-command-contract-spec.md`
- `docs/specs/frontend-simulation-experience-spec.md`
- `docs/archive/sprints/sprint15/sprint-15-main-simulation-board-simplification.md` (archived)
