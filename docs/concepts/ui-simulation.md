# UI simulation

## Scope
Primary simulation UI experience for the authoritative city simulation page.

## Covers
- board-first layout with the simulation board as the dominant surface
- a right-side inspector and event feed for human state and recent activity
- a bottom command console for deterministic commands
- snapshot- and timeline-driven rendering from backend-owned state
- full-height board container behavior so the world surface fills available vertical space on the authoritative page

## Active layout notes
- The simulation board container should fill remaining page height via flex layout (`flex: 1` + `min-height: 0`) rather than relying on percentage-height inheritance chains.
- Empty/loading overlays should be layered within the same board container so they do not collapse or displace the world surface.

## Source docs
- `docs/specs/main-simulation-board-spec.md`
- `docs/specs/simulation-read-model-spec.md`
- `docs/specs/deterministic-command-contract-spec.md`
- `docs/specs/frontend-simulation-experience-spec.md`
- `docs/archive/sprints/sprint15/sprint-15-main-simulation-board-simplification.md` (archived)
