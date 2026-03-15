# Event and Discovery Rule Matrix Spec

## Status

- **Status:** LOCKED for Rule-based event and discovery system (Sprints 16–20)
- **Scope anchor:** `docs/roadmap.md` Epic 15, `docs/sprints/sprint16/` through `docs/sprints/sprint20/`
- **Applies to:** Backend simulation triggers, domain event emission, discovery category assignment, and chat-orchestration boundaries

## Purpose

This spec fixes the product baseline for how low-level triggers, world/place conditions, and chat commands map to domain events and UI behavior. Implementation for Sprints 16–20 must align with this matrix and the design principles below.

## Design Principles

1. **Triggers vs domain events**  
   Low-level triggers (e.g. HUMAN_COLLISION, REACHED_PLACE) are not the same as domain events. Collision is a trigger; DIALOGUE_EXCHANGED and DISCOVERY_UNLOCKED are domain events produced by simulation rules when conditions are met.

2. **Chat never creates final domain events**  
   Chat may move a human, set a target, focus the UI, request filtered reads, or request an explicit intervention. Final domain events (DISCOVERY_UNLOCKED, DIALOGUE_EXCHANGED) must emerge only from simulation rules, not from chat handlers.

3. **Discovery category from context**  
   Discovery category (TECHNIQUE / SOCIAL_PRACTICE / KNOWLEDGE) must come from simulation context (place type, interaction type, traits) rather than arbitrary topic assignment. Target mapping:
   - FIRE / WORKSHOP / FARM → TECHNIQUE (scientific)
   - MARKET / CHURCH / group interaction → SOCIAL_PRACTICE (cultural)
   - LIBRARY / reflection places → KNOWLEDGE (philosophical)

4. **Board visualizes backend truth**  
   The symbolic board shows fixed places, human positions, temporary interactions, and event markers. Focus/highlight come from backend uiEffects. The frontend derives only presentation; canonical meaning stays backend-owned.

5. **Determinism**  
   Same seed and same step sequence must produce the same event and discovery outcomes. Chat commands that mutate world state (e.g. move human to place) must not break reproducibility of subsequent simulation steps.

## Rule Matrix (Product Baseline)

| Trigger | Required conditions | Domain event produced | Discovery category | UI label | Backend-owned |
|--------|---------------------|------------------------|--------------------|----------|----------------|
| HUMAN_COLLISION | Two humans within collision/proximity threshold | HUMANS_COLLIDED | none | none | Yes |
| HUMAN_COLLISION | Collision + both available + no recent discussion same pair | DIALOGUE_EXCHANGED | none | discussion | Yes |
| HUMAN_COLLISION | Collision + complementary knowledge traits | DISCOVERY_UNLOCKED | from context | scientific / cultural / philosophical | Yes |
| REACHED_PLACE | Human reaches place + discovery eligibility | DISCOVERY_UNLOCKED | by place (FIRE→TECHNIQUE, etc.) | scientific / cultural / philosophical | Yes |
| STAYED_AT_PLACE | Human at semantic place for N ticks | DISCOVERY_UNLOCKED | by place | scientific / cultural / philosophical | Yes |
| PROXIMITY_GROUP | Multiple humans close for bounded time | DIALOGUE_EXCHANGED or DISCOVERY_UNLOCKED | SOCIAL_PRACTICE | cultural | Yes |
| EVENT_HISTORY_DERIVATION | Sufficient persisted discoveries/event chains | INVENTION_EMERGED | inherited from source | scientific / cultural / philosophical | Yes |
| CHAT: MOVE_HUMAN_TO_PLACE | User asks e.g. "Tell Pierre to go to the forest" | no domain event | none | none | State mutation only; uiEffects for focus/place highlight |
| CHAT: FOCUS / SHOW_EVENTS_BY_TYPE / FOLLOW | User asks focus, show events, or follow | no domain event | none | none | Query + uiEffects only |
| DIRECTOR_INTERVENTION | Explicit confirmed intervention | intervention state; later ticks may produce normal events | depends on resulting event | intervention | Yes, with provenance |

## Place → Category Mapping (Backend)

- **FIRE, WORKSHOP, FARM** → TECHNIQUE (scientific)
- **MARKET, CHURCH** (social/reflection) → SOCIAL_PRACTICE (cultural) or DIALOGUE_EXCHANGED
- **LIBRARY / KNOWLEDGE_PLACE** → KNOWLEDGE (philosophical)

## UI Category Labels (Frontend)

- TECHNIQUE → scientific
- SOCIAL_PRACTICE → cultural
- KNOWLEDGE → philosophical

## Out of Scope for This Spec

- Exact threshold values (e.g. collision distance, place radius, N ticks) are implementation choices; they must remain deterministic and backend-owned.
- Visual marker details (e.g. icons, colors) are frontend presentation; this spec does not mandate specific glyphs.
