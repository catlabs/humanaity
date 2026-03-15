# Sprint 20: Proximity Group and Visual Markers

## Execution status

- Current phase: Completed
- Active chunk: none
- Next chunk: none
- Blocked items: none
- Last completed chunk: Task 2a (2026-03-15)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Added sustained proximity-group detection with deterministic connected components and per-group cooldown; emits DIALOGUE for pairs and SOCIAL_PRACTICE discovery for groups of 3+. |
| Task 2a | done | Added board markers for collision/dialogue/discovery categories (✦/💬/⚙🏛📜) plus explicit intervention status badges in board header. |

## Sprint intent

Sprint 20 completes the rule matrix implementation with (1) PROXIMITY_GROUP trigger: when multiple humans remain close for a bounded time (e.g. 2–3 ticks), emit DIALOGUE_EXCHANGED or DISCOVERY_UNLOCKED with SOCIAL_PRACTICE; (2) visual markers on the board per matrix (e.g. dialogue icon, discovery category icons, intervention badge) so the board clearly reflects event types and intervention state.

## Why this sprint comes next

Sprints 16–19 delivered dialogue from collision, chat commands, place model, and context-driven discovery. The last trigger in the matrix is PROXIMITY_GROUP; and the UI should make event meaning and intervention provenance visible at a glance (matrix suggested markers).

## Sprint outcome

At the end of Sprint 20, when a group of humans stays in proximity for K consecutive ticks, the simulation emits DIALOGUE_EXCHANGED or DISCOVERY_UNLOCKED (SOCIAL_PRACTICE). The board shows visual markers that distinguish collision, dialogue, discovery category (scientific/cultural/philosophical), and director intervention state.

## Sprint scope

### In scope

- Backend: Track groups of humans within proximity (e.g. same threshold as collision or slightly larger) over consecutive ticks. When a group of size ≥ 2 has been in proximity for K ticks (e.g. 2–3), emit one DIALOGUE_EXCHANGED or one DISCOVERY_UNLOCKED with SOCIAL_PRACTICE per group (or per pair within group, with throttling). Deterministic; one event per group per "window" to avoid spam.
- Frontend: Visual markers per rule matrix — temporary line/pulse for collision (existing); dialogue marker (e.g. 💬) for DIALOGUE_EXCHANGED; discovery category markers (e.g. ⚙ scientific, 🏛 cultural, 📜 philosophical) near humans or events; intervention badge when director intervention is pending or executed. Reuse existing interaction-layer and event-marker concepts; extend with category or type where needed.

### Out of scope

- New domain event types.
- Backend API contract changes beyond existing snapshot/timeline/effects.
- Broad board redesign; only marker/icon and intervention styling.

## Product and technical decisions for this sprint

- **Decision 1:** PROXIMITY_GROUP can emit either DIALOGUE_EXCHANGED or DISCOVERY_UNLOCKED(SOCIAL_PRACTICE); product may prefer one or the other, or both under different conditions (e.g. 2 humans → dialogue, 3+ → discovery). Document choice.
- **Decision 2:** Group tracking: maintain per-tick set of "groups" (connected components within proximity); when a group has existed for K ticks, emit. State must be deterministic (seed + tick + human positions).
- **Decision 3:** Visual markers are frontend presentation only; backend already sends event type and category. FE derives icon/label from eventType and invention category (TECHNIQUE/SOCIAL_PRACTICE/KNOWLEDGE) or event payload.

## Deliverables

- PROXIMITY_GROUP detection and DIALOGUE or DISCOVERY_UNLOCKED emission (SOCIAL_PRACTICE).
- Board visual markers: dialogue, discovery category, intervention badge.

## Definition of done

- A group of humans in proximity for K ticks produces at least one domain event (DIALOGUE_EXCHANGED or DISCOVERY_UNLOCKED with SOCIAL_PRACTICE); throttled and deterministic.
- Board shows distinct visuals for collision, dialogue, and discovery category (scientific/cultural/philosophical); director intervention state has a clear badge or label.

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java`
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `apps/ui/src/app/features/city/components/symbolic-board/`
- `apps/ui/src/app/features/city/pages/simulation-detail/` (intervention state styling)

## Features and task breakdown

### Feature 1: PROXIMITY_GROUP trigger (backend)

**Goal:** When multiple humans remain in proximity for K consecutive ticks, emit DIALOGUE_EXCHANGED or DISCOVERY_UNLOCKED (SOCIAL_PRACTICE).

**Acceptance criteria:** Group detection deterministic; sustained for K ticks; one event per group per window (or per pair with throttle); category SOCIAL_PRACTICE for discovery; event key and payload deterministic.

**Best owner:** Codex.

### Feature 2: Visual markers (frontend)

**Goal:** Board reflects event types and intervention state with clear markers per matrix.

**Acceptance criteria:** Collision: line/pulse (existing). Dialogue: e.g. 💬 or equivalent. Discovery: marker by category (⚙/🏛/📜 or icons). Director intervention: badge or label for pending/executed. Reuse existing layers; extend only where needed.

**Best owner:** Cursor.

## Recommended implementation order

1. Task 1a: PROXIMITY_GROUP logic and emission.
2. Task 2a: Frontend markers and intervention badge.

## Dependencies inside the sprint

- Task 2a can start in parallel; it uses existing event type and category from API.

## Suggested delegation

- **Codex:** Proximity group tracking, draft builder, throttling, determinism.
- **Cursor:** Board markers by event type and category, intervention badge styling.

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | PROXIMITY_GROUP → DIALOGUE or DISCOVERY (SOCIAL) | Codex | Sustained proximity produces event; SOCIAL_PRACTICE when discovery; deterministic. |
| Task 2a | Visual markers and intervention badge | Cursor | Board shows dialogue, discovery category, and intervention state clearly. |

## Risks

- Group definition (who is in the same "group") and K ticks may need tuning; avoid event spam.
- Multiple groups overlapping may require clear rule (e.g. disjoint groups, or largest group wins).

## Handoff to next sprint

Epic 15 (Rule-based event and discovery system) is complete after Sprint 20. Subsequent work can focus on hardening, polish, or new product features that build on the rule matrix.
