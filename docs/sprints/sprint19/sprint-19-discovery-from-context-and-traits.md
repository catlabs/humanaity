# Sprint 19: Discovery from Context and Traits

## Execution status

- Current phase: In progress
- Active chunk: Task 2a
- Next chunk: Task 3a
- Blocked items: none
- Last completed chunk: Task 1a (2026-03-15)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Added collision complementary-trait discovery path with deterministic category mapping and per-pair cooldown; random discovery now skips humans that already received context discovery this tick. |
| Task 2a | pending | Discovery category from context everywhere (refactor). |
| Task 3a | pending | STAYED_AT_PLACE (N ticks) discovery. |

## Sprint intent

Sprint 19 completes the discovery semantics from the rule matrix: (1) when two humans collide and have complementary knowledge traits, emit DISCOVERY_UNLOCKED with category from context; (2) refactor so all discovery emission assigns category from context (place, collision context, or proximity); (3) add STAYED_AT_PLACE: when a human remains at a place for N ticks, emit DISCOVERY_UNLOCKED with category from place.

## Why this sprint comes next

Sprint 18 added REACHED_PLACE and place-based category. Sprint 19 extends context-driven category to collision (traits) and adds the "stayed at place" trigger so discovery is fully rule- and context-driven instead of arbitrary topic index.

## Sprint outcome

At the end of Sprint 19, DISCOVERY_UNLOCKED category is always derived from simulation context (place type, collision traits, or later proximity). Collision can produce discovery when humans have complementary traits; staying at a place for N ticks can produce discovery. INVENTION_EMERGED continues to inherit category from source discoveries.

## Sprint scope

### In scope

- Define "complementary knowledge traits" (e.g. from existing Human fields or deterministic scores from id/seed). When two humans collide and traits are complementary, emit DISCOVERY_UNLOCKED with category derived from trait mix or context.
- Refactor discovery emission so category is never assigned by topicIndex % 3 alone; use (1) place type when REACHED_PLACE or STAYED_AT_PLACE, (2) collision context when collision + complementary traits, (3) keep existing random discovery path only where no context applies, or phase it out in favor of place/collision.
- STAYED_AT_PLACE: track ticks-at-current-place per human; when ticks at place ≥ N (e.g. 2 or 3), emit DISCOVERY_UNLOCKED with category from place. Throttle per human/place (e.g. one per N ticks per place).
- INVENTION_EMERGED category remains inherited from source discovery category (no change if already correct).

### Out of scope

- PROXIMITY_GROUP (Sprint 20).
- New event types or API contract changes beyond internal refactor.
- Chat or UI beyond existing invention/event display.

## Product and technical decisions for this sprint

- **Decision 1:** "Complementary" can be defined as different dimensions (e.g. creativity vs intellect) or opposite ends of a scale; implementation choice must be deterministic (seed + human ids).
- **Decision 2:** STAYED_AT_PLACE and REACHED_PLACE can both emit for the same place under different conditions (first tick at place vs. N ticks at place); avoid double emission in same tick by clear rules (e.g. REACHED_PLACE only on transition into place; STAYED_AT_PLACE only when ticks at place >= N and not already emitted this "stay window").
- **Decision 3:** Category from context: place → use place category; collision + traits → use derived category (e.g. mix of TECHNIQUE/SOCIAL_PRACTICE/KNOWLEDGE from trait dimensions).

## Deliverables

- Complementary-traits logic and collision→discovery with category.
- Refactored discovery category assignment (context-driven).
- STAYED_AT_PLACE draft builder and throttling; category from place.

## Definition of done

- Collision with complementary traits produces DISCOVERY_UNLOCKED with context-derived category.
- All discovery emission paths assign category from context (place or collision); no arbitrary topicIndex % 3 for new paths.
- Human at same place for N consecutive ticks can trigger one DISCOVERY_UNLOCKED (category from place); throttled per human/place.
- Determinism preserved.

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/human/domain/Human.java` (traits if needed)
- `docs/specs/event-discovery-rule-matrix-spec.md`

## Features and task breakdown

### Feature 1: Collision + complementary traits → discovery

**Goal:** When two humans collide and have complementary knowledge traits, emit DISCOVERY_UNLOCKED with category from context.

**Acceptance criteria:** Trait complementarity defined and deterministic; buildCollisionDiscoveryDrafts or extend buildStepEventDrafts; category derived from traits/context; event key and payload deterministic.

**Best owner:** Codex.

### Feature 2: Discovery category from context (refactor)

**Goal:** All discovery emission assigns category from place, collision context, or proximity (when added); remove or reduce reliance on topicIndex % 3.

**Acceptance criteria:** REACHED_PLACE and STAYED_AT_PLACE use place category; collision discovery uses trait-derived category; existing random discovery path updated or deprecated; INVENTION_EMERGED inherits from source.

**Best owner:** Codex.

### Feature 3: STAYED_AT_PLACE discovery

**Goal:** When human remains at a place for N ticks, emit DISCOVERY_UNLOCKED (category from place); throttle per human/place.

**Acceptance criteria:** Ticks-at-place tracked per human (and place); when ticks >= N, one draft emitted; category from place; throttling rule (e.g. one per stay window) deterministic.

**Best owner:** Codex.

## Recommended implementation order

1. Task 1a: Complementary traits and collision→discovery with category.
2. Task 2a: Refactor category assignment to context everywhere.
3. Task 3a: STAYED_AT_PLACE tracking and emission.

## Dependencies inside the sprint

- Task 2a may overlap with Task 1a and 3a (refactor as you add new paths).
- Task 3a reuses place model from Sprint 18; ticks-at-place state must be deterministic.

## Suggested delegation

- **Codex:** Trait definition, collision discovery, STAYED_AT_PLACE, category refactor.
- **You:** Approve trait complementarity definition and N for STAYED_AT_PLACE.

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Collision + complementary traits → DISCOVERY_UNLOCKED with category | Codex | When collision and traits complementary, discovery emitted with context category. |
| Task 2a | Discovery category from context everywhere | Codex | No discovery uses arbitrary topic index for category; place/collision context used. |
| Task 3a | STAYED_AT_PLACE (N ticks) discovery | Codex | After N ticks at place, DISCOVERY_UNLOCKED emitted; category from place; throttled. |

## Risks

- Trait complementarity too loose or too strict may flood or starve discovery; tune for demo.
- STAYED_AT_PLACE and REACHED_PLACE must not double-emit in same tick; define clear precedence or exclusion.

## Handoff to next sprint

Sprint 20 will add PROXIMITY_GROUP (sustained proximity → DIALOGUE or SOCIAL discovery) and visual markers per matrix (e.g. intervention badge). Discovery category from context is then complete for all triggers in the matrix.
