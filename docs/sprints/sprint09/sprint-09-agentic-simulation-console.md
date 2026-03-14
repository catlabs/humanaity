# Sprint 9: Agentic Simulation Console

## Execution status

- Current phase: Sprint 9 planned
- Active chunk: `Task 4a` - validation and closeout
- Next chunk: none
- Blocked items: depends on Sprint 8 orchestration loop
- Last completed chunk: `Task 3a` - supporting-panel simplification (2026-03-14)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Locked a console-first boundary: map + chat are primary, supporting panels are secondary, and no new command classes are introduced in Sprint 9. |
| Task 2a | done | Consolidated the simulation shell around a stronger central console hierarchy (map + chat prominence, reduced side-panel weight). |
| Task 2b | done | Centralized `uiEffects` interpretation in a dedicated frontend service so refresh/focus/highlight behavior is applied consistently. |
| Task 3a | done | Simplified supporting context surfaces by reducing panel density and showing bounded recent history/invention lists. |
| Task 4a | planned | Validate the consolidated console flow and close the sprint. |

## Sprint intent

Sprint 9 exists to turn the successful Sprint 8 chat loop into the product's primary simulation console rather than one more control surface inside a busy page.

This sprint is frontend-led, with thin backend coordination only where the UI effect contract needs small adjustments.

## Why this sprint comes next

Sprint 8 proves the orchestration loop. Sprint 9 should capitalize on that immediately by simplifying the UI around the strongest new behavior.

If Sprint 9 is skipped:

- the chat workflow remains bolted onto an older page structure
- the product still reads like several parallel demos instead of one coherent console
- guided commands in Sprint 10 would land on top of a layout that has not been simplified first

## Sprint outcome

At the end of Sprint 9, the city simulation page should read as one agentic console: the map is central, the chat panel is the dominant interaction surface, and only lightweight context/history panels remain beside it.

## Sprint scope

### In scope

- consolidate the authoritative simulation page around map + chat + lightweight context
- keep route ownership on `/cities/:id`
- promote backend-owned `uiEffects` into stable focus/highlight/select behavior
- simplify timeline/info surfaces so they support the console flow
- preserve existing generated-client/backend contract usage

### Out of scope

- new command classes beyond Sprint 8 safe commands
- guided follow/focus semantics that require new backend capabilities
- director/intervention commands
- broad design-system rewrite

## Product and technical decisions for this sprint

### Decision 1: one flagship page

Sprint 9 should make the city simulation page clearly feel like the main product, not one route among several equal-status prototypes.

### Decision 2: chat becomes the dominant interaction surface

The UI should bias user action toward the chat panel for commands and explanations, while keeping direct controls only where they remain obviously useful.

Console-first definition for Sprint 9:

- map remains the primary visual surface
- chat remains the primary command-and-explanation surface
- history/inspector surfaces are intentionally lighter than map/chat
- no new command classes, no guided/director semantics, no backend orchestration expansion

### Decision 3: context panels should support, not compete

History, metrics, and entity context should become lighter and more focused so the map + chat story stays primary.

### Decision 4: backend effects stay authoritative

If the backend says to refresh, focus, or highlight something, the page should respond through one stable effect-handling path rather than scattered one-off UI logic.

## Deliverables

- a consolidated simulation-console layout on the primary city page
- stable focus/highlight handling from orchestration effects
- simplified supporting panels for history and context
- validation notes proving the new console flow remains usable

## Definition of done

- the city simulation page is clearly organized around map + chat
- supporting panels are secondary and lightweight
- Sprint 8 commands still work in the new layout
- focus/highlight effects are visible and stable
- the sprint documents validation plus residual UX risks

## Suggested file targets

- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/features/city/services/`
- `apps/ui/src/app/shared/components/`
- `docs/sprints/sprint09/`

## Features and task breakdown

## Feature 1: Console boundary lock

### Goal

Define how far Sprint 9 goes in UI simplification so it does not collapse into either a cosmetic-only pass or a full redesign program.

### Acceptance criteria

- a contributor can tell what "console-first" means in this repo
- Sprint 9 clearly excludes new command-class expansion

### Best owner

- You

## Feature 2: Page shell consolidation

### Goal

Refactor the authoritative simulation page so map, chat, and lightweight context have clear visual hierarchy.

### Acceptance criteria

- the page reads as one product surface
- the map and chat surfaces are visually primary

### Best owner

- Cursor chat

## Feature 3: Effect handling and supporting-panel simplification

### Goal

Make selection/highlight behavior stable and reduce panel clutter around the new console loop.

### Acceptance criteria

- backend effects feel intentional and visible in the UI
- supporting panels remain useful without dominating the page

### Best owner

- Cursor chat

## Feature 4: Validation and closeout

### Goal

Confirm the consolidated console remains functional and document any remaining UX debt before guided commands begin.

### Acceptance criteria

- Sprint 8 chat flows still work on the new page
- residual console/layout risks are documented clearly

### Best owner

- Codex and Cursor chat

## Recommended implementation order

1. Lock the Sprint 9 UI boundary.
2. Consolidate the simulation page shell.
3. Stabilize effect handling.
4. Simplify supporting panels.
5. Validate the new console flow.

## Dependencies inside the sprint

- Sprint 8 backend orchestration loop must exist first.
- Effect handling depends on the Sprint 8 response contract staying stable.

## Suggested delegation

### Best tasks for you

- approve the product hierarchy and scope boundary

### Best tasks for Cursor chat

- page layout consolidation
- panel simplification
- frontend effect handling

### Best tasks for Codex

- validation
- any narrow effect-contract or refactor support

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Console-first boundary lock | You | Sprint 9 scope is clearly limited to UI consolidation. |
| Task 2a | Simulation page shell consolidation | Cursor chat | The page hierarchy is map + chat first. |
| Task 2b | UI effect stabilization | Cursor chat | Refresh/focus/highlight behavior is consistent. |
| Task 3a | Supporting-panel simplification | Cursor chat | History/info surfaces support the console instead of competing with it. |
| Task 4a | Validation and closeout | Codex and Cursor chat | The console flow is validated and residual risks are recorded. |

## Risks

- Sprint 9 can drift into aesthetic churn if hierarchy and outcome are not kept concrete
- reducing panel weight without losing useful information will need discipline
- if Sprint 8 contracts are unstable, Sprint 9 can accidentally absorb backend work it should not own

## Handoff to next sprint

Sprint 10 should add guided human workflows only after Sprint 9 makes the console coherent and stable.
