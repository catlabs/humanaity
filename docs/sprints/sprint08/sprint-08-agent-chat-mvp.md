# Sprint 8: Agent Chat MVP

## Execution status

- Current phase: Sprint 8 planned
- Active chunk: `Task 3b` - UI effects and refresh loop
- Next chunk: `Task 4a` - validation and sprint closeout
- Blocked items: none
- Last completed chunk: `Task 3a` - simulation-page chat panel (2026-03-14)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Locked Sprint 8 safe-command/effect boundaries in the orchestration spec to prevent guided/director scope creep. |
| Task 2a | done | Added `/api/agent/cities/{cityId}/chat` endpoint, stable request/response DTOs, ownership enforcement, and skeleton contract test. |
| Task 2b | done | Implemented bounded safe command execution (step, snapshot, summary, explain event, recent inventions) with user-facing messages and backend-owned `uiEffects`. |
| Task 3a | done | Added a lightweight in-page Agent Chat panel wired to backend orchestration with conversation continuity and response rendering. |
| Task 3b | planned | Wire backend `uiEffects` and refresh behavior into the frontend simulation loop. |
| Task 4a | planned | Add focused validation, update sprint status, and record residual risks. |

## Sprint intent

Sprint 8 exists to prove the new product direction with one strong end-to-end loop: the user chats with the simulation on the main city page, the backend interprets the request, allowed actions or reads run, and the UI refreshes from backend-owned state.

This sprint is mixed backend and frontend work, but it stays small on purpose. It should not redesign the whole simulation page or attempt guided/director behaviors yet.

## Why this sprint comes first

The roadmap now treats the agentic simulation console as the next portfolio-facing milestone after Sprint 7.

If Sprint 8 is skipped:

- the product story remains split across controls, panels, and MCP demos
- chat remains an idea instead of a real application flow
- later console redesign work risks being purely visual without a real orchestration backbone
- guided and director commands would start before the safe MVP command loop is proven

## Sprint outcome

At the end of Sprint 8, HUMANAIty should support one city-scoped agent chat MVP on the main simulation page. A user can ask for safe commands such as stepping the city, reading the latest state, summarizing recent changes, explaining an event, and listing recent inventions, and the backend returns a user-facing reply plus refresh/focus effects the UI can apply.

## Sprint scope

### In scope

- lock the orchestration architecture and response contract for the new chat direction
- add a backend-owned agent chat endpoint/service for one city
- implement only safe MVP command classes
- reuse backend application services and read models where they already exist
- keep MCP as a parity/tool layer, not the primary app runtime backend
- add a lightweight chat panel to the existing authoritative simulation page
- refresh snapshot/history after allowed actions using backend-owned contracts
- add focused validation for backend orchestration and the chat loop

### Out of scope

- broad simulation-page redesign
- guided human commands
- director/intervention commands
- persistent conversation memory
- broad auth/platform changes
- replacing existing snapshot/history endpoints with a chat-only API

## Product and technical decisions for this sprint

### Global decision: prove one real chat loop before redesigning the console

Sprint 8 should deliver a real orchestration loop and a usable chat surface on the current simulation page. It should not wait for a full UI redesign.

### Decision 1: backend orchestration owns command policy

The frontend sends chat requests to the backend. The backend decides whether the command is allowed, which services/tools are used, and which UI effects should happen next.

### Decision 2: safe commands only

Sprint 8 covers only these commands:

- advance simulation by bounded `N` steps
- show latest city state/snapshot
- summarize recent changes
- explain a known event
- show recent inventions

### Decision 3: deterministic state stays canonical

Action commands must still route through deterministic backend-owned simulation logic. Chat does not become a second state model.

### Decision 4: prefer refresh effects over custom frontend inference

The orchestration response should tell the UI what to refresh or focus. The frontend should not guess which panels to reload after an action.

### Decision 5: use the current city simulation route as the first host surface

Sprint 8 should land on the existing authoritative simulation page for `/cities/:id`. A separate experimental chat page would dilute the product story.

## Deliverables

By the end of Sprint 8, the repo should contain:

- a locked orchestration spec and Sprint 8 task boundary
- backend DTOs/controller/service for city-scoped agent chat
- safe MVP command execution in the backend orchestration layer
- a lightweight simulation-page chat panel using the new endpoint
- UI effect handling for refresh/focus actions
- focused tests or validations for the new chat path

## Definition of done

Sprint 8 is done only if all of the following are true:

- the backend exposes one city-scoped orchestration endpoint with a stable UI-facing response contract
- safe MVP commands work without bypassing backend-owned simulation/history truth
- the primary simulation page has a usable chat surface
- action commands trigger visible backend-owned state refresh
- the chat response can point the UI at relevant entities through stable ids/effects
- guided and director commands remain explicitly out of scope

## Suggested file targets

These are the most likely files or folders Sprint 8 will touch:

- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/features/city/city.service.ts`
- `apps/ui/src/app/api/`
- `docs/specs/agent-chat-orchestration-spec.md`
- `docs/sprints/sprint08/`

Likely new code areas:

- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/api/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/application/`
- `apps/ui/src/app/features/agent-chat/` or equivalent page-local chat component path

## Features and task breakdown

## Feature 1: Spec and command boundary lock

### Goal

Turn the new agent chat direction into an executable Sprint 8 boundary with explicit safe commands and clear non-goals.

### Acceptance criteria

- a contributor can tell which commands belong in Sprint 8 and which are deferred
- the backend/frontend contract direction is fixed before implementation

### Best owner

- You

## Feature 2: Backend orchestration endpoint and safe command execution

### Goal

Create the backend-owned orchestration slice that accepts chat input and executes allowed safe commands against deterministic state and read models.

### Acceptance criteria

- the backend can interpret one city-scoped chat request and return a UI-facing response payload
- safe MVP commands execute through backend-owned services or read models rather than UI-only logic

### Best owner

- Codex

## Feature 3: Chat panel and refresh loop integration

### Goal

Expose the orchestration endpoint on the authoritative simulation page and wire the response into page refresh/focus behavior.

### Acceptance criteria

- a user can submit a chat request from the main simulation page
- the page refreshes relevant state after allowed actions without guessing from free text

### Best owner

- Cursor chat

## Feature 4: Validation and closeout

### Goal

Prove the new chat loop works at the touched boundary and record anything intentionally deferred.

### Acceptance criteria

- regressions in the new orchestration path are reasonably visible
- the sprint doc records actual validation results and residual risks

### Best owner

- Codex

## Recommended implementation order

1. Lock the orchestration spec and Sprint 8 command boundary.
2. Add backend agent chat DTOs, controller, and orchestration service skeleton.
3. Implement the safe MVP command executors.
4. Add the simulation-page chat panel.
5. Wire refresh/focus effects into the UI.
6. Validate the new path and close the sprint.

## Dependencies inside the sprint

- Feature 1 blocks the rest of the sprint.
- Feature 2 blocks Feature 3.
- Feature 3 depends on existing simulation snapshot/history consumers staying authoritative.
- Feature 4 depends on both backend orchestration and UI integration being in place.

## Suggested delegation

### Best tasks for you

- keep the command boundary disciplined
- decide user-facing wording and non-goals
- confirm which response/effect semantics are acceptable

### Best tasks for Cursor chat

- chat panel integration
- generated client wiring
- page refresh/effect plumbing

### Best tasks for Codex

- backend orchestration slice
- safe command execution logic
- contract-focused tests and validation

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Orchestration spec and Sprint 8 boundary lock | You | Sprint 8 commands, response contract, and non-goals are unambiguous. |
| Task 2a | Backend agent chat API skeleton | Codex | One city-scoped backend endpoint/service exists for agent chat requests. |
| Task 2b | Safe MVP command executors | Codex | Step/snapshot/summary/explain/inventions commands execute through backend-owned logic. |
| Task 3a | Simulation-page chat panel | Cursor chat | The authoritative simulation page can submit chat requests and render replies. |
| Task 3b | UI effects and refresh loop | Cursor chat | Action replies cause the right snapshot/history refresh and focus behavior. |
| Task 4a | Validation and sprint closeout | Codex | The new chat loop is validated and residual risks are recorded. |

## Risks

- the orchestration layer could sprawl into a generic agent platform if command classes are not kept narrow
- UI integration could become a redesign sprint unless the first pass stays lightweight
- response contracts can get muddy if raw tool internals leak into frontend-visible payloads
- safe command parsing should remain explicit and bounded rather than pretending to support arbitrary free-form control

## Handoff to next sprint

Sprint 9 should build on Sprint 8 by consolidating the UI around the successful chat loop rather than adding lots of new commands immediately.

What Sprint 8 should leave behind:

- one credible end-to-end agent chat demo
- a backend-owned orchestration contract
- a clear separation between safe commands now and everything deferred
