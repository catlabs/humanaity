# Sprint 18: Place Model and REACHED_PLACE Discovery

## Execution status

- Current phase: In progress
- Active chunk: Task 2a
- Next chunk: Task 3a
- Blocked items: none
- Last completed chunk: Task 1a (2026-03-15)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Added backend place registry with normalized ids/coords/radius and context categories aligned to board places (forest, river, church, campfire, house). |
| Task 2a | pending | At-place detection per tick; REACHED_PLACE discovery emission. |
| Task 3a | pending | Align backend places with FE; category from place. |

## Sprint intent

Sprint 18 introduces the backend place model and REACHED_PLACE trigger from the rule matrix. When a human reaches a place (enters its radius) and satisfies discovery eligibility, the simulation emits DISCOVERY_UNLOCKED with category derived from the place (FIRE/WORKSHOP/FARM→TECHNIQUE, MARKET/CHURCH→SOCIAL_PRACTICE, LIBRARY→KNOWLEDGE).

## Why this sprint comes next

Sprint 17 added chat move-to-place and events drawer. Now the simulation itself must use places so that discoveries are place-aware and category comes from context (rule matrix spec). This is the first step toward context-driven discovery category.

## Sprint outcome

At the end of Sprint 18, the backend has a fixed set of places with coordinates and optional radius; each tick the simulation determines "at place" per human; when a human reaches a place (was not at it, now is) and is eligible, DISCOVERY_UNLOCKED is emitted with category from place. Backend place set aligns with FE board places.

## Sprint scope

### In scope

- Backend place model: fixed set of places (e.g. FIRE, WORKSHOP, FARM, MARKET, CHURCH, LIBRARY or align with FE: forest, river, church, campfire, house) with id, normalized (x,y), and optional radius. No new entity required if using a registry/config.
- Each tick: for each human, compute which place (if any) the human is at (distance to place center ≤ radius).
- REACHED_PLACE trigger: when human was not at place P at previous tick and is at P this tick, and "discovery eligibility" (e.g. not busy, optional cooldown per human/place), emit one DISCOVERY_UNLOCKED with category from place (FIRE/WORKSHOP/FARM→TECHNIQUE; MARKET/CHURCH→SOCIAL_PRACTICE; LIBRARY→KNOWLEDGE). Event key and payload deterministic.
- Align backend place ids and coords with FE board places so board and simulation share the same semantics.
- Refactor or add discovery draft builder for REACHED_PLACE; call from buildStepEventDrafts.

### Out of scope

- STAYED_AT_PLACE (N ticks at place) — Sprint 19.
- Complementary-traits collision discovery — Sprint 19.
- PROXIMITY_GROUP — Sprint 20.
- Chat or drawer changes.

## Product and technical decisions for this sprint

- **Decision 1:** Place set can be a constant list or config in simulation layer; no new DB table required for MVP if places are fixed per product.
- **Decision 2:** "At place" requires storing or computing previous tick position or "last place" per human. Option: compute "at place" from current (x,y) only; "reached" = first tick at which human is in radius (may require tracking "was at place P last tick" in memory or on Human entity).
- **Decision 3:** Discovery eligibility: e.g. human not busy; optional per-place or per-human cooldown (e.g. same place once per 5 ticks) to avoid spam. Deterministic.

## Deliverables

- Place model (registry) with id, x, y, radius, category.
- At-place detection and REACHED_PLACE discovery emission in simulation step.
- Discovery category from place in emitted DISCOVERY_UNLOCKED.
- Backend places aligned with FE.

## Definition of done

- When a human moves into a place radius and is eligible, DISCOVERY_UNLOCKED is emitted with correct category (TECHNIQUE / SOCIAL_PRACTICE / KNOWLEDGE) from place.
- Same seed + same steps => same REACHED_PLACE discoveries (determinism).
- Backend place list matches FE board places (ids and coords) for consistency.

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/` (place registry, at-place logic, new draft builder)
- `apps/backend/src/main/java/eu/catlabs/humanaity/human/domain/Human.java` (optional: lastPlaceId or similar if persisted)
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `apps/ui/src/app/features/city/pages/simulation-detail/simulation-detail.component.ts` (boardPlaces — align ids with backend)

## Features and task breakdown

### Feature 1: Place model (backend)

**Goal:** Define fixed set of places with id, (x,y), radius, and discovery category.

**Acceptance criteria:** Place registry available to simulation; each place has id, x, y, radius, category (TECHNIQUE/SOCIAL_PRACTICE/KNOWLEDGE); ids match FE (e.g. forest, church, campfire).

**Best owner:** Codex.

### Feature 2: At-place detection and REACHED_PLACE discovery

**Goal:** Each tick compute "at place" per human; when human reaches a place (enters radius) and is eligible, emit DISCOVERY_UNLOCKED with category from place.

**Acceptance criteria:** buildReachedPlaceDrafts or equivalent; previous/current at-place state tracked deterministically; eligibility rule (e.g. not busy + cooldown); event key and payload deterministic; wired into buildStepEventDrafts.

**Best owner:** Codex.

### Feature 3: Align backend and FE places

**Goal:** Backend place ids and coords match FE board places so move-to-place and simulation use same semantics.

**Acceptance criteria:** Document or enforce alignment (e.g. backend registry mirrors FE boardPlaces ids and normalized coords); no conflicting definitions.

**Best owner:** Codex / You.

## Recommended implementation order

1. Task 1a: Place registry with id, x, y, radius, category.
2. Task 2a: At-place detection; REACHED_PLACE draft builder; wire into step; eligibility.
3. Task 3a: Align with FE; document or sync place list.

## Dependencies inside the sprint

- Task 2a depends on Task 1a.
- Task 3a can be done in parallel or after Task 1a.

## Suggested delegation

- **Codex:** Place model, at-place logic, REACHED_PLACE drafts, category mapping.
- **You:** Confirm place list (ids and categories) to match FE and product intent.

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Place model (registry) with id, coords, radius, category | Codex | Simulation can resolve place by id and get coords + category. |
| Task 2a | At-place detection + REACHED_PLACE discovery emission | Codex | DISCOVERY_UNLOCKED emitted when human reaches place and eligible; category from place. |
| Task 3a | Align backend places with FE | Codex / You | Place ids and coords consistent between backend and FE. |

## Risks

- Radius too large may make "at place" too easy; too small may rarely trigger. Tune for demo.
- Tracking "previous at place" may require in-memory state per run or optional field on Human; keep deterministic.

## Handoff to next sprint

Sprint 19 will add discovery from collision (complementary traits), discovery category from context everywhere, and STAYED_AT_PLACE (N ticks at place). The place model from Sprint 18 is reused.
