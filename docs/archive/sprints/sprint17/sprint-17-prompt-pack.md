# Sprint 17 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 17 task only.

References for every task:

- `docs/roadmap.md` (Epic 15)
- `docs/specs/event-discovery-rule-matrix-spec.md`
- `docs/sprints/sprint17/sprint-17-chat-go-to-place-and-show-events-drawer.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 17:

- chat may only mutate human position (move to place) or return query results + uiEffects
- chat must NOT create DISCOVERY_UNLOCKED or DIALOGUE_EXCHANGED
- place registry in backend must align with FE board places (same ids)

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: Add MOVE_HUMAN_TO_PLACE (or directive_go_place) intent: parse human name and place name, resolve human by name, resolve place to (x,y), set human position, return REFRESH_SNAPSHOT + FOCUS_HUMAN and optional HIGHLIGHT_PLACE(placeId).

Acceptance criteria:

- classifyIntent recognizes "go to", "tell X to go to", "send X to" + place name.
- Backend place registry: fixed map placeId → (x, y) normalized 0–1 (e.g. forest → (0.14, 0.18) to match FE).
- Resolve human by name from city humans (e.g. case-insensitive or fuzzy match).
- Update human x,y to place coords; save; return response with REFRESH_SNAPSHOT, FOCUS_HUMAN(humanId), and optionally HIGHLIGHT_PLACE(placeId) if effect type exists.
- Message text confirms move (e.g. "Moved Pierre to the forest.").

In scope: AgentChatOrchestrationService, place registry (config or constant map), HumanRepository, human position update.

Out of scope: SHOW_EVENTS_BY_TYPE, drawer UI, domain event creation.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: Add SHOW_EVENTS_BY_TYPE intent; query events by type; return eventIds and OPEN_EVENTS_DRAWER uiEffect; extend AgentUiEffectOutput and OpenAPI.

Acceptance criteria:

- classifyIntent recognizes "show (me) (all) events (of type) X", "list collisions", "events of type discussion"; map collision → HUMANS_COLLIDED, discussion → DIALOGUE_EXCHANGED, etc.
- Call EventApplicationService.listCityEventsByType(cityId, eventType); limit to sensible cap (e.g. 50).
- Response: referencedEntities.eventIds or structuredData with event ids; uiEffects include OPEN_EVENTS_DRAWER with payload eventType and/or eventIds.
- AgentUiEffectOutput (and OpenAPI spec) extended to support type OPEN_EVENTS_DRAWER and optional eventType, eventIds. Regenerate Angular client.

In scope: AgentChatOrchestrationService, EventApplicationService, AgentUiEffectOutput, OpenAPI, generated client.

Out of scope: Frontend drawer implementation, MOVE_HUMAN_TO_PLACE.

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal: When chat response contains OPEN_EVENTS_DRAWER, open a drawer with event list filtered by eventType or eventIds; show type, tick, year, narrative; click → HIGHLIGHT_EVENT.

Acceptance criteria:

- AgentChatEffectsService resolves OPEN_EVENTS_DRAWER and sets state (e.g. drawerOpen + eventIds or eventType) for simulation-detail to consume.
- Simulation-detail opens drawer (Material or slide-out) when effect present; content = events from timeline/events source filtered by eventIds or eventType.
- List items show event type, tick, year, enriched snippet if present; click sets selectedEventId and optionally closes drawer or keeps it with detail.
- Existing HIGHLIGHT_EVENT / selectedEventId behavior used for board highlight.

In scope: agent-chat-effects.service.ts, simulation-detail component, new drawer component or inline drawer, event list from existing events signal/source.

Out of scope: Backend changes, place highlight (Task 4a).

## Task 4a Prompt

Implement only `Task 4a`; do not expand to other sprint tasks.

Goal: When chat moves human to place, apply FOCUS_HUMAN and optional HIGHLIGHT_PLACE so the board shows focused human and highlighted target place.

Acceptance criteria:

- Existing FOCUS_HUMAN effect already focuses human marker; ensure it is applied from move-human response.
- If backend sends HIGHLIGHT_PLACE(placeId), frontend highlights that place on the board (e.g. CSS class, ring, or pulse on the place anchor for that placeId).
- Board places already exist in simulation-detail; add visual state for "highlighted place" when effect present.

In scope: simulation-detail, symbolic-board or simulation-board, place highlight styling, effect resolution for HIGHLIGHT_PLACE.

Out of scope: Backend place registry, events drawer.
