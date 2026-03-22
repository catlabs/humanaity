# Frontend Simulation Experience Spec (Sprint 4)

## Status

- Status: LOCKED for Sprint 4 planning
- Scope anchor: `docs/archive/sprints/sprint04/sprint-04-frontend-simulation-experience.md`
- Applies to: Sprint 4 historical baseline for the city simulation experience in `apps/ui`
- Current authoritative page note: superseded for the main `/cities/:id` surface by `docs/specs/main-simulation-board-spec.md` (Sprint 15)

## Purpose

This document fixes the frontend behavior and page-boundary semantics that Sprint 4 implementation must follow.

If implementation choices conflict with this spec during Sprint 4, this spec is the source of truth.

## Primary Product Goal

Sprint 4 delivers one credible city simulation page that:

- loads a real city
- shows a real world view
- lets the user control the simulation
- surfaces real history and inventions
- behaves coherently when data is absent, loading, or stale

## Authoritative Page Rule

Sprint 4 must have one primary city simulation surface.

The frontend must not keep two equal-status pages that both claim ownership of:

- simulation controls
- world rendering
- event/invention panels
- city simulation detail navigation

The surviving route must be the canonical destination for `/cities/:id`.

## Required UI Sections

The primary simulation page must include:

- city context header
- simulation controls
- world/canvas surface
- human list or inspector surface
- invention summary surface
- event/timeline summary surface

These sections may be laid out differently during implementation, but all are required in Sprint 4.

## Contract-to-UI Mapping

### Snapshot-driven UI

The Sprint 3 snapshot contract is the source of truth for:

- city name
- run status
- tick/year/era
- population and aggregate metrics
- current humans
- recent inventions
- summary history counters

Frontend code must not recompute product-facing alternatives for these fields.

### History-driven UI

History endpoints are the source of truth for:

- event feed content
- extended inventions listing when the snapshot window is insufficient
- timeline-oriented summaries shown in panels

## Required Control Behavior

Sprint 4 controls must support:

- start
- pause or stop aligned with the current backend contract
- deterministic step increment
- visible state refresh after any control action

Rules:

- disabled states should reflect in-flight requests or invalid actions
- visible state must refresh from backend data after control actions
- controls must not mutate page state optimistically in ways that hide backend truth

## World View Behavior

This section is Sprint 4 historical behavior and is not the current authoritative-page direction.

The world view is centered on the Pixi canvas.

Rules:

- Pixi must render current human positions from backend data
- the canvas must live on the authoritative city simulation page
- page lifecycle should initialize and tear down Pixi cleanly
- polling or refresh behavior should keep rendered humans reasonably current for MVP

## Panel Behavior

### Humans

- show current humans from backend snapshot or position stream
- support selection or inspection behavior if already present in the surviving page

### Inventions

- show real inventions from backend data
- do not expose fake local invention generation in Sprint 4

### Events and timeline

- show real history data
- summary-first presentation is acceptable for Sprint 4
- full chronology workstation behavior is deferred

## Empty-State Rules

### City exists, no run yet

The page must still render successfully and clearly communicate:

- no simulation run exists yet
- controls available to start or create progress
- world and history panels may be empty without looking broken

### No history yet

The page must:

- show empty event/invention/timeline states explicitly
- avoid fake seed data

### No humans

The page must:

- keep the canvas and surrounding layout stable
- show empty population/world messaging explicitly

## Loading and Error Rules

### Loading

The page must make visible when:

- snapshot is loading
- control actions are in flight
- history panels are loading

### Error

The page must present recoverable errors for:

- snapshot load failure
- history load failure
- control-action failure

Avoid silent failure with only console logging as the user-visible behavior.

## Deferred for Later Sprints

- full visual redesign of the simulation UI
- advanced timeline exploration UX
- AI-authored narrative cards
- real-time subscriptions beyond MVP polling/refresh
- multi-city dashboards
- deep human simulation inspector semantics beyond current surface needs

## Implementation Guardrails

- preserve existing design-system patterns unless a small page-local adjustment is necessary
- prefer generated API clients over direct fallback HTTP for Sprint 4-covered paths
- keep route ownership explicit and singular
- remove fake page-local data instead of layering real data beside it
- do not let Pixi integration remain stranded in a non-authoritative route

## Done Signals for Sprint 4

Sprint 4 implementation is aligned with this spec only if:

- one page clearly owns the city simulation experience
- the page uses real backend snapshot/history data
- the control loop is real
- the canvas is real
- empty/loading/error states are explicit
- fake content is removed from Sprint 4-covered surfaces
