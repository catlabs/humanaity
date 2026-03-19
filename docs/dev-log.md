# Dev log

Use this file as the rolling dated log for milestone execution notes.

## 2026-03-18

Decision:

- keep the existing concept-first docs structure instead of reviving sprint planning as the active workflow
- make deterministic commands and AI narration-only behavior the forward product direction
- keep legacy agent orchestration material only as historical or transitional context

Implemented:

- added active concept docs for `product-architecture` and `ai-narration`
- rewrote the active command, simulation-engine, and UI-simulation concept docs around milestone-oriented delivery
- added `docs/milestones.md` and the deterministic command contract spec
- updated docs navigation so `docs/roadmap.md` stays navigation-only and `docs/dev-log.md` becomes the rolling execution log
- updated `docs/agent-context.md` with active-work read order, docs update cadence, and a Codex handoff template
- added `.cursor/skills/docs/milestone-execution/SKILL.md` for milestone slicing, docs update timing, and Codex handoff packets
- updated `.cursor/rules/*` and `.cursor/skills/docs/*` guidance so agents know when to read and update `docs/milestones.md`, `docs/dev-log.md`, concept docs, and specs

Trade-offs:

- legacy agent-chat code and its spec remain in the repo for traceability and incremental migration
- this slice resets the documentation and planning surface only; the runtime migration to `POST /api/simulations/{cityId}/commands` is still follow-up implementation work
- existing rule filenames still use some sprint-era names, but their content now points to milestone-oriented workflow

## 2026-03-18

Decision:

- make board-area sizing deterministic in CSS by using flex growth instead of percentage height chaining

Implemented:

- updated `SimulationDetailComponent` styles so `.content` is a flex container and `.world-body` uses `flex: 1 1 0%` with `min-height: 0`
- removed `.world-body { height: 100%; }` which could resolve to zero in the current page layout chain
- synced active docs:
  - `docs/milestones.md` with this M1 slice update
  - `docs/concepts/ui-simulation.md` with layout-fill behavior guidance
  - `docs/specs/main-simulation-board-spec.md` with board-container sizing rule

Trade-offs:

- this slice fixes the board container collapse path only; it does not redesign panel composition or command/feed behavior

## 2026-03-18

Decision:

- turn M1 into an explicit four-slice execution plan instead of keeping it as a three-line milestone summary
- align the active UI concept/spec docs with the current board-left, right-rail, command-bottom page direction

Implemented:

- expanded `docs/milestones.md` M1 into execution slices with exit criteria, ordered tasks, and explicit M2/M3/M4 boundaries
- updated `docs/concepts/ui-simulation.md` so the active concept framing matches the three-zone authoritative page layout
- revised `docs/specs/main-simulation-board-spec.md` from the older Sprint 15 simplification framing to the active M1 page contract

Trade-offs:

- this slice clarifies planning and page semantics only; it does not change runtime UI implementation yet
- `docs/specs/frontend-simulation-experience-spec.md` remains historical reference material rather than the active main-page contract

## 2026-03-19

Decision:

- complete M1 by making the authoritative simulation page itself carry the board-first demo story instead of hiding status, selection, and activity behind overlays or drawer-first workflows
- keep the current interaction path for the bottom console in M1, but relabel and structure it as a command-first surface ahead of the M2 backend rewrite

Implemented:

- rewrote `SimulationDetailComponent` into a three-zone layout with a dominant board panel, persistent right-rail inspector/feed, and bottom command console
- promoted simulation status, selected-human inspection, recent event feed, and recent discoveries into the primary page workspace on `/cities/:id`
- tightened no-run and loading overlays so they keep the board stable while clarifying what the user should do next
- synced `docs/milestones.md`, `docs/concepts/ui-simulation.md`, and `docs/specs/main-simulation-board-spec.md` to match the implemented M1 surface

Trade-offs:

- the command console still uses the existing interaction endpoint, so the deterministic command contract remains M2 work
- the event drawer still exists as a secondary deep-history surface, but the primary readable feed is now in the right rail

## 2026-03-19

Decision:

- complete M2 by making the main command console use the deterministic backend command contract instead of the legacy agent-chat path
- keep the legacy agent orchestration endpoint available for secondary/historical flows, but remove it from the primary `/cities/:id` control loop

Implemented:

- added `POST /api/simulations/{cityId}/commands` with fail-closed handling for `advance <count>`, `focus <human>`, and `move <human> <place>`
- wired the simulation-detail command console and step button to the new deterministic endpoint and applied UI effects from the structured command response
- added backend API contract coverage for auth, ownership, advance/focus/move success cases, and fail-closed rejection of legacy phrasing
- synced milestones, concepts, and specs so M2 is recorded as complete and the deterministic command endpoint is the active command contract

Trade-offs:

- the legacy agent-chat code still exists for non-primary flows and historical traceability, but it is no longer the main simulation command path
- exact human matching is intentionally strict in M2; broader guided/natural-language behavior remains outside the deterministic contract

## 2026-03-19

Decision:

- reconcile the active docs so the authoritative simulation-page concept, milestone tracker, and board spec all describe the same post-M2 command-first behavior

Implemented:

- resolved semantic drift across `docs/concepts/ui-simulation.md`, `docs/milestones.md`, and `docs/specs/main-simulation-board-spec.md`
- clarified that the `/cities/:id` page now treats `POST /api/simulations/{cityId}/commands` as the primary deterministic control path
- kept legacy agent-chat references documented as historical or secondary rather than primary-page behavior

Trade-offs:

- this slice is documentation-only; it does not change the already-implemented UI or backend command behavior

## 2026-03-19

Implemented:

- fixed `SimulationCommandsApiContractTest` bearer helper to use `JwtService.generateAccessToken(user.getEmail())` so `spring-boot:run` test-compile matches other API contract tests and local backend startup succeeds
