# Milestones

This file is the active delivery tracker for the next development days.

Execution rhythm:

- update this file after each completed task or milestone slice
- append one dated entry to `docs/dev-log.md` after each meaningful execution slice
- do not create new sprint docs for active work

## M1 — Simulation UI

- Status: COMPLETE
- Goal: Make the simulation visually understandable in under 30 seconds.
- Current baseline: `GET /api/simulations/{cityId}/snapshot`, the city simulation page, the board component, human selection, and timeline loading already exist.
- Visible UI outcome: board-first page with the board on the left, a right-side inspector/feed, and a bottom command console that reads clearly at a glance.
- Backend/API work:
  - keep `GET /api/simulations/{cityId}/snapshot` as the canonical read model
  - keep timeline/history endpoints as the source for feed content
  - add `locations[]` to the snapshot only if board readability truly needs canonical location DTOs
- Frontend work:
  - simplify `SimulationDetailComponent` around board, inspector/feed, and command console
  - keep `SimulationBoardComponent` and `BoardViewModelService` as the primary rendering path
  - reduce agent-specific framing on labels and panels
- Execution plan:
  - Slice 1 — Layout shell and information hierarchy
    - move the authoritative page to a stable three-zone layout: compact header, board + right rail, bottom command console
    - keep the board as the dominant surface and preserve the flex-fill board container work already landed
    - exit criteria: the page reads correctly before any interaction and the board remains visually primary on common laptop widths
  - Slice 2 — Inspector and simulation-status readability
    - keep selected-human details, run status, tick/year, and human-count context visible without opening secondary drawers
    - merge the right rail around one clear inspector/feed story instead of several competing cards
    - exit criteria: after selecting a human, the user can identify who is selected and what state the simulation is in without extra navigation
  - Slice 3 — Feed and command-console framing
    - promote recent history into the right rail and relabel the bottom interaction area as a command-first console, even if M2 owns the backend command rewrite
    - keep current controls usable while making the page read as `board -> context -> action`
    - exit criteria: a demo viewer can see where events appear and where commands will be entered next
  - Slice 4 — Demo-state polish and validation
    - tighten no-run, loading, empty, and error states so the page stays readable when backend data is absent or in flight
    - run a final demo pass focused on the 30-second comprehension goal and capture any deferred follow-ups for M2/M3
    - exit criteria: no primary page state looks broken, collapsed, or ambiguous during the demo path
- Ordered tasks:
  1. Reframe the page layout to board-left, inspector/feed-right, command-bottom.
  2. Keep selected-human state and basic simulation status visible without extra navigation.
  3. Promote a clear right-rail event feed and a command-first bottom console without pulling M2 backend work forward.
  4. Tighten no-run, loading, empty, and error states for demo readability.
  5. Run a final M1 demo pass and capture explicit follow-ups for M2 and M3.
- Deferred follow-up boundary:
  - M1 does not introduce the new deterministic command endpoint; that remains M2 work
  - M1 does not add AI narration cards; that remains M4 work
  - M1 may reuse current history/timeline data, but deeper event semantics stay in M3
- Recent slice updates:
  - 2026-03-18: fixed `SimulationDetailComponent` board container sizing so `.world-body` reliably fills available vertical space (flex-fill layout chain, no percentage-height collapse).
  - 2026-03-18: rewrote M1 as a four-slice execution plan with explicit exit criteria and M2/M3/M4 boundaries.
  - 2026-03-19: implemented the authoritative three-zone simulation page with a dominant board, right-rail inspector/feed, and bottom command console on `/cities/:id`.
- Done signal: a new user can identify the board, select a human, understand the current simulation state, and see where commands and recent events live in under 30 seconds.

## M2 — Command System

- Status: COMPLETE
- Goal: Replace the primary agent-chat control loop with explicit deterministic commands.
- Current baseline: the repo now has an M1 board-first page and a legacy agent orchestration endpoint, but the primary console path still needed to move to the deterministic command contract.
- Visible UI outcome: the bottom input reads as a command console and accepts only explicit supported commands with clear feedback.
- Backend/API work:
  - add `POST /api/simulations/{cityId}/commands` as the primary command surface
  - support `advance <count>`, `focus <human>`, and `move <human> <place>`
  - keep parsing fail-closed and remove AI from the primary command path
- Frontend work:
  - relabel the current chat strip as a command console
  - send explicit commands and render backend feedback
  - refresh snapshot and timeline after mutation commands
- Ordered tasks:
  1. Implement the request and response DTOs from `docs/specs/deterministic-command-contract-spec.md`.
  2. Reuse deterministic backend command pieces where possible, but remove LLM fallback from the primary path.
  3. Wire the UI to the new endpoint and keep command errors explicit.
- Recent slice updates:
  - 2026-03-19: implemented `POST /api/simulations/{cityId}/commands`, wired the main command console to it, and removed agent-chat parsing from the primary control loop.
  - 2026-03-19: reconciled milestone/spec doc wording so the authoritative page docs consistently describe the deterministic command console as the primary control path.
- Done signal: valid commands execute deterministically, invalid commands fail clearly, and mutation commands visibly refresh the board and feed.

## M3 — Event System

- Status: READY
- Goal: Make simulation changes readable as a coherent event flow.
- Current baseline: event history endpoints, timeline loading, event drawer behavior, and board event markers already exist.
- Visible UI outcome: the right-side feed makes recent movement, interaction, and discovery activity readable without opening secondary tools.
- Backend/API work:
  - reuse `GET /api/simulations/{cityId}/history/events` and `GET /api/simulations/{cityId}/history/timeline`
  - ensure location-entry and discovery events remain deterministic and UI-readable
  - preserve ordered event semantics by tick and sequence
- Frontend work:
  - promote a clear chronological feed on the main page
  - synchronize feed focus with board markers and selected entities
  - surface recent event outcomes immediately after step or move commands
- Ordered tasks:
  1. Turn the current event drawer behavior into a clearer primary feed.
  2. Keep recent board activity and feed entries visually linked.
  3. Ensure stepping or moving produces an obvious event delta in the UI.
- Done signal: a step or move action produces visible board and feed updates that can be understood without explanation.

## M4 — AI Narrative Layer

- Status: READY
- Goal: Add AI value as narration only.
- Current baseline: backend event enrichment already exists and `EventOutput` already exposes enrichment fields.
- Visible UI outcome: events can show natural-language narration beside deterministic facts, with clear fallback behavior.
- Backend/API work:
  - keep narration attached to deterministic event or invention outputs as the primary path
  - add a separate narration endpoint only if event DTOs prove insufficient
  - keep explicit `ready`, `fallback`, and absent semantics
- Frontend work:
  - render narrative text beside canonical event facts
  - show fallback and empty states explicitly
  - keep AI out of command parsing and state mutation flows
- Ordered tasks:
  1. Reuse existing enrichment fields as the first-class narrative contract.
  2. Add narrative presentation to the main event feed.
  3. Verify the UI remains useful when narration is missing or fallback-generated.
- Done signal: narration improves readability without becoming required for command execution or simulation understanding.

## M5 — Product Coherence

- Status: READY
- Goal: Make the full system demo understandable in under two minutes.
- Current baseline: the repo already contains the main pieces, but the UI and docs still reflect an older agentic framing.
- Visible UI outcome: the page reads as one coherent product flow from command to state change to event to narration.
- Backend/API work:
  - keep the deterministic command surface and snapshot/timeline contracts primary
  - remove forward-demo dependence on the legacy agent orchestration path
- Frontend work:
  - tighten labels, panel hierarchy, and information density
  - keep empty, loading, and error states calm and readable
  - preserve the board as the dominant visual surface
- Ordered tasks:
  1. Remove lingering agent-first copy from the main page.
  2. Tighten panel hierarchy so board, feed, and command console read as one flow.
  3. Run an end-to-end demo pass and fix the most visible confusion points.
- Done signal: the system can be demonstrated as `Command -> Simulation -> Events -> Narration -> UI` in under two minutes.
