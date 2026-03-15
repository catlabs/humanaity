# Sprint 18 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 18 task only.

References for every task:

- `docs/roadmap.md` (Epic 15)
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `docs/sprints/sprint18/sprint-18-place-model-and-reached-place-discovery.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 18:

- place model is backend-owned; no new DB entity required if using fixed registry
- REACHED_PLACE = human enters place radius (was not at place, now is) + eligibility
- do not add STAYED_AT_PLACE or PROXIMITY_GROUP in this sprint

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: Introduce backend place model: fixed set of places with id, normalized (x,y), radius, and discovery category (TECHNIQUE / SOCIAL_PRACTICE / KNOWLEDGE).

Acceptance criteria:

- Place registry available to simulation (e.g. constant list or config). Each place: id (String), x (double), y (double), radius (double), category (InventionCategory or equivalent).
- Place ids align with FE board places: e.g. forest, river, church, campfire, house (or FIRE, WORKSHOP, FARM, MARKET, CHURCH, LIBRARY if product uses different names). Coords normalized 0–1 to match snapshot space.
- Mapping: FIRE/campfire, WORKSHOP, FARM → TECHNIQUE; MARKET, CHURCH → SOCIAL_PRACTICE; LIBRARY → KNOWLEDGE (or match FE place list).

In scope: New class or config for place registry; category per place.

Out of scope: At-place detection, discovery emission, FE changes.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: Each tick compute "at place" per human; when human reaches a place (was not at it, now is) and satisfies discovery eligibility, emit DISCOVERY_UNLOCKED with category from place.

Acceptance criteria:

- For each human, determine which place (if any) contains the human (distance to place center ≤ radius). Need "previous" at-place state: either in-memory (per run) or derived from previous tick positions.
- REACHED_PLACE: human not at P at t-1, at P at t; and eligible (e.g. not busy; optional cooldown per human/place). Emit one EventDraft DISCOVERY_UNLOCKED with category = place.getCategory(); event key deterministic (e.g. REACHED:humanId:placeId:tick).
- buildReachedPlaceDrafts(tick, humans, previousAtPlace or positions) and call from buildStepEventDrafts; emit via eventApplicationService.emitEventsAtTick.
- Eligibility: deterministic (e.g. cooldown from seed + humanId + placeId + tick).

In scope: SimulationApplicationService, place registry, at-place detection, REACHED_PLACE draft builder, eligibility, buildStepEventDrafts wiring.

Out of scope: STAYED_AT_PLACE, trait-based discovery, PROXIMITY_GROUP.

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal: Align backend place list with FE board places so simulation and chat move-to-place use the same semantics.

Acceptance criteria:

- Backend place ids and (x,y) match FE boardPlaces (e.g. in simulation-detail.component.ts). Document mapping or derive FE list from backend if feasible.
- No conflicting definitions: one source of truth for place id and coords (backend); FE can hardcode same values or fetch from API if added later.

In scope: Documentation, optional sync of FE boardPlaces with backend registry (same ids and normalized coords).

Out of scope: New API endpoint for places unless product requires it.
