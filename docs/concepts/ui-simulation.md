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
- The authoritative `/cities/:id` page should read as three zones: compact status/header, board-plus-right-rail workspace, and bottom command console.
- The board remains the dominant surface; the right rail should keep simulation status, selected-human context, and the recent activity feed in one readable column without competing with the board for primary attention.
- The bottom command console is the primary control surface on the page and should submit exact deterministic commands through `POST /api/simulations/{cityId}/commands`.
- Deterministic command feedback should be rendered directly from backend command responses rather than inferred from AI narration or best-effort client interpretation.
- The simulation board container should fill remaining page height via flex layout (`flex: 1` + `min-height: 0`) rather than relying on percentage-height inheritance chains.
- Empty/loading overlays should be layered within the same board container so they do not collapse or displace the world surface.
- Legacy agent-chat affordances may remain elsewhere in the repo for historical or secondary flows, but they are not the authoritative command path for this page.

## Source docs
- `docs/specs/main-simulation-board-spec.md`
- `docs/specs/simulation-read-model-spec.md`
- `docs/specs/deterministic-command-contract-spec.md`
- `docs/specs/frontend-simulation-experience-spec.md`
- `docs/archive/sprints/sprint15/sprint-15-main-simulation-board-simplification.md` (archived)
