# Sprint 19 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 19 task only.

References for every task:

- `docs/roadmap.md` (Epic 15)
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `docs/sprints/sprint19/sprint-19-discovery-from-context-and-traits.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 19:

- discovery category must come from context (place, collision traits, or stay-at-place)
- do not add PROXIMITY_GROUP (Sprint 20)
- keep determinism for all new logic

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: When two humans collide and have complementary knowledge traits, emit DISCOVERY_UNLOCKED with category derived from context (trait mix).

Acceptance criteria:

- Define "complementary traits" using existing Human fields (e.g. creativity, intellect, sociability, practicality) or deterministic scores from id/seed. Rule must be deterministic (e.g. different dimensions high/low, or opposite ends of scale).
- In buildStepEventDrafts (or new helper): for each pair already in collision, if traits are complementary, add one DISCOVERY_UNLOCKED EventDraft with category derived from traits (e.g. map trait dimensions to TECHNIQUE/SOCIAL_PRACTICE/KNOWLEDGE). Event key deterministic (e.g. COLLISION_DISC:humanA:humanB:tick).
- Do not double-emit with existing random discovery for same human/tick; decide precedence (e.g. collision discovery over random, or mutually exclusive).

In scope: SimulationApplicationService, Human trait access, complementarity check, discovery draft with category from context.

Out of scope: STAYED_AT_PLACE, PROXIMITY_GROUP, UI.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: Refactor discovery emission so category is always assigned from context (place, collision, or stay-at-place); remove or reduce topicIndex % 3 for category.

Acceptance criteria:

- REACHED_PLACE and STAYED_AT_PLACE discoveries use place.getCategory().
- Collision discovery (Task 1a) uses trait-derived category.
- Any remaining "random" discovery path (e.g. buildDiscoveryDrafts) either uses context when available or is phased out / limited to fallback; no arbitrary topicIndex % 3 as sole source of category for new paths.
- INVENTION_EMERGED category remains inherited from source discovery (verify existing behavior).

In scope: All discovery draft builders in SimulationApplicationService; category assignment logic.

Out of scope: New triggers, API changes.

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal: When a human remains at the same place for N consecutive ticks, emit DISCOVERY_UNLOCKED with category from place; throttle per human/place.

Acceptance criteria:

- Track "ticks at current place" per human (and which place). Reset when human leaves place. When ticks at place >= N (e.g. 2 or 3), emit one DISCOVERY_UNLOCKED with category = place.getCategory(). Throttle: e.g. one discovery per "stay window" per human per place (so not every tick after N).
- buildStayedAtPlaceDrafts or equivalent; deterministic event key (e.g. STAYED:humanId:placeId:tick); call from buildStepEventDrafts.
- State for "ticks at place" must be deterministic (derived from positions and place registry, or stored in run-scoped state).

In scope: SimulationApplicationService, place model from Sprint 18, ticks-at-place tracking, STAYED_AT_PLACE draft builder, throttling.

Out of scope: PROXIMITY_GROUP, frontend.
