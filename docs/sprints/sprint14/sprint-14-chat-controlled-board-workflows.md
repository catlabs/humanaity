# Sprint 14: Chat-Controlled Board Workflows

## Execution status

- Current phase: Sprint 14 planned
- Active chunk: `Task 3a` - safe and guided board reactions
- Next chunk: `Task 3b` - intervention board visualization
- Blocked items: depends on Sprint 13 board overlays; intervention-related work also depends on Sprint 11 completion
- Last completed chunk: `Task 2b` - frontend board effect resolver alignment (2026-03-14)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Locked board reaction expectations by command class: SAFE (refresh + optional highlight), GUIDED (focus/track markers), DIRECTOR (explicit intervention styling and confirmation-gated reaction). |
| Task 2a | done | Added minimal backend board-oriented effect hints (`TRACK_HUMAN`, `MARK_EVENT`, `BOARD_INTERVENTION_PENDING`, `BOARD_INTERVENTION_EXECUTED`) with updated contract tests. |
| Task 2b | done | Extended `AgentChatEffectsService` and simulation effect handling for board-aware tracking, event marking, and intervention pending/executed effect states. |
| Task 3a | planned | Make safe commands and guided workflows visibly react on the board. |
| Task 3b | planned | Integrate intervention visualization if Sprint 11 has landed; otherwise keep this slice blocked and explicit. |
| Task 4a | planned | Validate the end-to-end “chat drives board” product story and document residual gaps. |

## Sprint intent

Sprint 14 exists to make the board the primary visual feedback surface for the simulation console, so chat commands no longer feel detached from the world state they affect or describe.

This sprint should close the loop between orchestration, deterministic backend state, and reactive visualization.

## Why this sprint comes next

Sprint 12 provides the board foundation and Sprint 13 makes it expressive. Sprint 14 should now use that surface to show the result of safe commands, guided workflows, and, where available, explicit interventions.

If Sprint 14 is skipped:

- the board remains a passive viewer rather than the core observatory surface
- chat responses still depend too heavily on text alone
- the flagship “AI-driven civilization simulation console” story remains incomplete

## Sprint outcome

At the end of Sprint 14, safe chat commands and guided workflows should produce visible board reactions through backend-led UI effects, and explicit interventions should integrate into that same flow only if the intervention contract from Sprint 11 is available.

## Sprint scope

### In scope

- lock the chat-to-board effect contract
- add minimal backend `uiEffect` extensions if current effects are insufficient
- centralize board effect handling in the existing frontend effect path
- make safe commands and guided workflows visibly update the board
- integrate intervention visualization only if explicit intervention semantics are already complete
- validate the end-to-end console story

### Out of scope

- autonomous board behavior not triggered by backend state or effects
- broad new command classes beyond existing safe, guided, and explicit intervention boundaries
- hidden intervention behavior that bypasses confirmation or provenance rules

## Product and technical decisions for this sprint

### Decision 1: backend remains the effect authority

The board should react to backend-owned effects and state refreshes, not parse free text or invent its own command outcomes.

### Decision 2: safe and guided commands ship first-class board feedback

Stepping, focus, compare, follow, and summary flows should have visible board consequences where that improves comprehension.

Sprint 14 command-to-board contract lock:

- SAFE commands: refresh board state and optionally highlight event/invention context markers
- GUIDED commands: focus or track explicit humans on-board with bounded visual emphasis
- DIRECTOR commands: show explicit intervention-pending/executed states with distinct styling; never render as ordinary safe updates

### Decision 3: intervention reactions remain explicitly gated

If a command such as “make Alice meet Lucas” is supported, the board must reflect its explicit intervention status and confirmation semantics rather than presenting it as an ordinary safe command.

## Deliverables

- locked chat-to-board effect contract
- minimal backend/frontend effect-path updates
- visible board reactions for safe and guided commands
- optional intervention visualization integrated behind the existing intervention boundary
- validation notes and a repeatable demo flow

## Definition of done

- the board reacts visibly to safe and guided chat commands
- board reactions are driven by backend-owned effects and refreshed state
- intervention-related board behavior is either integrated explicitly or documented as blocked by Sprint 11
- the simulation console reads as one coherent chat-driven observatory experience

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/agent/`
- `apps/ui/src/app/features/city/services/`
- `apps/ui/src/app/features/city/components/`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `apps/ui/src/app/api/model/`
- `docs/sprints/sprint14/`

## Features and task breakdown

## Feature 1: Chat-to-board effect contract lock

### Goal

Define exactly how command classes should map to board reactions so later implementation does not sprawl.

### Acceptance criteria

- each major command class has explicit board-behavior expectations
- intervention behavior is clearly distinguished from safe and guided flows

### Best owner

- You

## Feature 2: Backend/frontend effect path alignment

### Goal

Extend and align the existing effect system so the board can react intentionally without duplicating orchestration logic in the UI.

### Acceptance criteria

- any new effect semantics are minimal and stable
- the frontend has one consistent board-aware effect handling path

### Best owner

- Codex

## Feature 3: Visible board reactions for chat workflows

### Goal

Make the simulation board the main visual confirmation surface for chat-driven workflows.

### Acceptance criteria

- safe commands visibly affect the board
- guided focus/follow behavior is apparent on the board
- intervention visualization, if enabled, is explicit and policy-aligned

### Best owner

- Cursor chat for UI work, Codex for contract support

## Feature 4: Validation and closeout

### Goal

Prove the “chat drives board” loop end to end and record any remaining blocked or deferred work.

### Acceptance criteria

- the console story can be demonstrated cleanly from chat command to board update
- blocked intervention slices, if any, are documented explicitly

### Best owner

- Codex

## Recommended implementation order

1. Lock the board-aware effect contract.
2. Add any minimal backend effect extensions.
3. Align the frontend effect resolver and board handlers.
4. Wire safe commands into visible board reactions.
5. Add guided workflow board reactions.
6. Integrate intervention visualization only if Sprint 11 has landed.
7. Validate the end-to-end experience.

## Dependencies inside the sprint

- Feature 1 blocks the rest of the sprint.
- Feature 2 blocks Feature 3.
- Intervention work in Feature 3b depends on Sprint 11 completion.
- Feature 4 depends on the final command/effect path being stable.

## Suggested delegation

### Best tasks for you

- approve the board-behavior contract for each command class
- decide whether the first intervention visualization is in scope based on Sprint 11 status

### Best tasks for Cursor chat

- board reaction polish
- command-result visual emphasis
- final console integration work

### Best tasks for Codex

- backend `uiEffect` shaping
- frontend effect service alignment
- validation and end-to-end reasoning checks

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Chat-to-board effect contract lock | You | Command classes have explicit board-reaction rules. |
| Task 2a | Backend board-effect support | Codex | Any missing board-oriented `uiEffects` are added cleanly. |
| Task 2b | Frontend effect-path alignment | Codex | The existing effect resolver consistently drives board behavior. |
| Task 3a | Safe and guided command board reactions | Cursor chat | Safe and guided chat flows visibly update the board. |
| Task 3b | Intervention board visualization | Codex and Cursor chat | Intervention reactions are explicit, confirmed, and policy-aligned, or clearly blocked. |
| Task 4a | Validation and closeout | Codex | The end-to-end chat-driven board loop is validated and residual gaps are recorded. |

## Risks

- too many new effect types can overcomplicate a contract that is currently clean and bounded
- frontend board behavior may diverge from backend meaning if free-text interpretation leaks back into the UI
- intervention visualization can undermine policy clarity if confirmation/provenance status is not visually distinct

## Expected sprint outputs

- board-aware effect contract
- visible board feedback for chat-driven workflows
- optional first intervention visualization path
- demo-ready “AI-driven civilization simulation console” flow

## Handoff to next sprint

Any later board work should extend depth or polish incrementally, not reopen the ownership boundary between deterministic backend state and frontend visualization.
