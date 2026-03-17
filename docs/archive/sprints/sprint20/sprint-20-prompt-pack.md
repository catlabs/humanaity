# Sprint 20 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 20 task only.

References for every task:

- `docs/roadmap.md` (Epic 15)
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `docs/sprints/sprint20/sprint-20-proximity-group-and-visual-markers.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 20:

- PROXIMITY_GROUP produces DIALOGUE_EXCHANGED or DISCOVERY_UNLOCKED (SOCIAL_PRACTICE); one per group per window
- visual markers are frontend-only derivation from existing event type and category

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: When multiple humans remain in proximity for K consecutive ticks, emit DIALOGUE_EXCHANGED or DISCOVERY_UNLOCKED with SOCIAL_PRACTICE (one per group per window).

Acceptance criteria:

- Define "group": humans within proximity threshold (e.g. same as or slightly larger than collision threshold). Track which humans are in the same connected component per tick.
- Sustain: group G exists for K consecutive ticks (e.g. K=2 or 3). Then emit one event: either DIALOGUE_EXCHANGED (e.g. pick one pair in group) or DISCOVERY_UNLOCKED with category SOCIAL_PRACTICE. Event key deterministic (e.g. PROX_GROUP:groupKey:tick).
- Throttle: one event per group per "window" (e.g. after emitting, do not emit again for same group for next M ticks). State must be deterministic (seed + tick + positions).
- buildProximityGroupDrafts or equivalent; wire into buildStepEventDrafts.

In scope: SimulationApplicationService, group detection (e.g. connected components by distance), sustained-group tracking, draft builder, throttling.

Out of scope: Frontend markers, new event types.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: Board shows visual markers per rule matrix: collision (line/pulse), dialogue (e.g. 💬), discovery category (scientific/cultural/philosophical), and director intervention badge.

Acceptance criteria:

- Collision: existing line or pulse between humans kept.
- DIALOGUE_EXCHANGED: visible distinction (e.g. 💬 or dialogue icon) in interaction layer or event marker.
- Discovery: when showing event or invention, use category to pick marker: TECHNIQUE → scientific (e.g. ⚙), SOCIAL_PRACTICE → cultural (e.g. 🏛), KNOWLEDGE → philosophical (e.g. 📜). Can be near human or on event marker.
- Director intervention: when intervention state is pending or executed, show clear badge or label (existing or enhanced styling).
- Reuse existing symbolic-board and simulation-detail components; extend only where needed (e.g. event marker by type/category, intervention badge).

In scope: apps/ui symbolic-board, simulation-board, simulation-detail; event type and category from API; intervention state from effects.

Out of scope: Backend changes, new API fields.
