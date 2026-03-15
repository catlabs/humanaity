# Sprint 17: Chat Go-to-Place and Show Events Drawer

## Execution status

- Current phase: In progress
- Active chunk: Task 4a
- Next chunk: none
- Blocked items: none
- Last completed chunk: Task 3a (2026-03-15)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Added MOVE_HUMAN_TO_PLACE intent classification, backend place registry aligned with board coordinates, name-based human resolution, and deterministic position updates with refresh/focus effects. |
| Task 2a | done | Added SHOW_EVENTS_BY_TYPE intent resolution, event-type querying through EventApplicationService, OPEN_EVENTS_DRAWER uiEffect payload (eventType + eventIds), and AgentUiEffectOutput/API model extension for drawer/place-aware effects. |
| Task 3a | done | Added OPEN_EVENTS_DRAWER effect handling and a simulation-detail events drawer filtered by eventType/eventIds with click-to-select event behavior. |
| Task 4a | pending | Frontend: Place highlight + focus when chat moves human. |

## Sprint intent

Sprint 17 adds two chat-driven capabilities from the rule matrix: (1) "Tell Pierre to go to the forest" — move a human to a place and reflect on the board; (2) "Show me all events of type collision" — open a drawer listing those events. Chat does not create domain events; it only mutates state (position) or returns query results and uiEffects.

## Why this sprint comes next

Sprint 16 delivered dialogue from collision. Users need to direct humans to places and query events by type with visible UI feedback (drawer, place highlight). This sprint is backend-owned state/query + frontend reaction only.

## Sprint outcome

At the end of Sprint 17, the user can ask the agent to move a named human to a named place (e.g. forest) and see the board update with focus and optional place highlight; and the user can ask to see all events of a given type and get an events drawer opened with the filtered list.

## Sprint scope

### In scope

- Backend: Intent MOVE_HUMAN_TO_PLACE (or directive_go_place). Parse human name and place name; resolve human by name; resolve place to backend place id and (x,y). Set human position to place coords; return REFRESH_SNAPSHOT + FOCUS_HUMAN; optional HIGHLIGHT_PLACE with placeId.
- Backend: Place registry (fixed set of places with id and normalized x,y) aligned with FE board places (e.g. forest, river, church, campfire, house).
- Backend: Intent SHOW_EVENTS_BY_TYPE (or list_events_by_type). Parse event type (e.g. collision → HUMANS_COLLIDED, discussion → DIALOGUE_EXCHANGED). Call EventApplicationService.listCityEventsByType (or equivalent); return eventIds in referencedEntities or structuredData; add uiEffect OPEN_EVENTS_DRAWER with eventType and/or eventIds.
- API: Extend AgentUiEffectOutput (and OpenAPI) with OPEN_EVENTS_DRAWER and optional payload (eventType, eventIds). Regenerate Angular client.
- Frontend: When chat response contains OPEN_EVENTS_DRAWER, open drawer with event list filtered by eventType/eventIds; show type, tick, year, narrative; click → HIGHLIGHT_EVENT.
- Frontend: When chat moves human to place, apply FOCUS_HUMAN and optional HIGHLIGHT_PLACE; highlight target place and focus human marker.

### Out of scope

- Domain event creation from chat (no DISCOVERY_UNLOCKED or DIALOGUE_EXCHANGED from chat).
- Place model for simulation triggers (REACHED_PLACE / STAYED_AT_PLACE) — that is Sprint 18.
- Discovery category refactor or proximity group.

## Product and technical decisions for this sprint

- **Decision 1:** Backend owns the place registry (id → x,y). FE boardPlaces can remain hardcoded with same ids so board and move-target stay aligned.
- **Decision 2:** Chat handlers must never call eventApplicationService.emitEventsAtTick or create domain events; only Human position update and query + uiEffects.
- **Decision 3:** OPEN_EVENTS_DRAWER carries eventType and/or eventIds so the drawer can filter or fetch; FE may use existing timeline/events source filtered by ids or type.

## Deliverables

- MOVE_HUMAN_TO_PLACE intent and move-human action; place map in backend.
- SHOW_EVENTS_BY_TYPE intent and OPEN_EVENTS_DRAWER uiEffect; API extended; client regenerated.
- Events drawer component; place highlight and focus on move.

## Definition of done

- User can say "Tell Pierre to go to the forest" (or similar); Pierre's position updates to forest coords; board refreshes and focuses Pierre; optional place highlight.
- User can say "Show me all events of type collision" (or similar); backend returns event ids and OPEN_EVENTS_DRAWER effect; drawer opens with filtered events; click highlights event on board.
- Chat never creates DISCOVERY_UNLOCKED or DIALOGUE_EXCHANGED.

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/application/AgentChatOrchestrationService.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/api/dto/AgentUiEffectOutput.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/event/application/EventApplicationService.java` (listCityEventsByType)
- OpenAPI spec and generated Angular client
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/features/city/services/agent-chat-effects.service.ts`

## Features and task breakdown

### Feature 1: MOVE_HUMAN_TO_PLACE (backend)

**Goal:** Parse "go to place" / "tell X to go to Y"; resolve human and place; set position; return refresh + focus (+ optional HIGHLIGHT_PLACE).

**Acceptance criteria:** Intent classified; human resolved by name; place resolved to coords; human position updated and saved; response has REFRESH_SNAPSHOT, FOCUS_HUMAN; optional HIGHLIGHT_PLACE(placeId).

**Best owner:** Codex.

### Feature 2: SHOW_EVENTS_BY_TYPE and OPEN_EVENTS_DRAWER (backend + API)

**Goal:** Intent list_events_by_type; query events by type; return eventIds and OPEN_EVENTS_DRAWER effect; extend AgentUiEffectOutput.

**Acceptance criteria:** Intent parses event type; listCityEventsByType used; eventIds in response; OPEN_EVENTS_DRAWER with eventType/eventIds; OpenAPI and client updated.

**Best owner:** Codex.

### Feature 3: Events drawer (frontend)

**Goal:** When OPEN_EVENTS_DRAWER in response, open drawer with event list; click → highlight event.

**Acceptance criteria:** AgentChatEffectsService handles OPEN_EVENTS_DRAWER; simulation-detail opens drawer with events; list shows type, tick, year, narrative; click sets selectedEventId / HIGHLIGHT_EVENT.

**Best owner:** Cursor.

### Feature 4: Place highlight and focus (frontend)

**Goal:** When chat moves human to place, show place highlight and focus human.

**Acceptance criteria:** FOCUS_HUMAN applied; optional HIGHLIGHT_PLACE handled (e.g. CSS class or ring on place); board shows focused human and highlighted place.

**Best owner:** Cursor.

## Recommended implementation order

1. Task 1a: Backend place map + MOVE_HUMAN_TO_PLACE intent and action.
2. Task 2a: SHOW_EVENTS_BY_TYPE intent + OPEN_EVENTS_DRAWER; extend API; regenerate client.
3. Task 3a: Frontend events drawer.
4. Task 4a: Frontend place highlight and focus.

## Dependencies inside the sprint

- Task 2a must complete before Task 3a (FE needs effect type and payload).
- Task 1a and Task 2a can be parallel; Task 3a and 4a can be parallel after 2a.

## Suggested delegation

- **Codex:** Backend intents, place registry, event list by type, API extension.
- **Cursor:** Drawer component, effect handling, place highlight, focus.

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | MOVE_HUMAN_TO_PLACE intent + place map + move human | Codex | User message moves named human to named place; backend updates position; returns refresh + focus. |
| Task 2a | SHOW_EVENTS_BY_TYPE + OPEN_EVENTS_DRAWER effect + API | Codex | Intent returns event ids and OPEN_EVENTS_DRAWER; AgentUiEffectOutput extended; client regenerated. |
| Task 3a | Events drawer for OPEN_EVENTS_DRAWER | Cursor | Drawer opens with filtered events; click highlights event. |
| Task 4a | Place highlight + focus when chat moves human | Cursor | Focus and optional place highlight visible on board. |

## Risks

- Place name normalization (e.g. "the forest" vs "forest") must match between chat and place registry.
- Drawer and place highlight should not block or override existing focus/track behavior incorrectly.

## Handoff to next sprint

Sprint 18 will add the backend place model and REACHED_PLACE discovery so that simulation (not just chat) uses places for discovery category. The place registry from Sprint 17 can be reused or extended for that.
