# Sprint 21 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 21 task only.

References for every task:

- `docs/roadmap.md` (Epics 16-19)
- `docs/specs/agent-chat-orchestration-spec.md`
- `docs/specs/chat-goals-tech-tree-spec.md`
- `docs/sprints/sprint21/sprint-21-chat-commands.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 21:

- deterministic parsing first
- LLM fallback only for ambiguous but allowed commands
- provenance visible in API, logs, and UI

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: define structured command types and deterministic matching for the core safe command families.

Acceptance criteria:

- Introduce structured command types for at least `STEP_SIMULATION`, `PAUSE_SIMULATION`, `FOCUS_HUMAN`, `MOVE_TO_PLACE`, and `MEET_HUMAN`.
- Add deterministic matching for common phrasing variants such as `step 10`, `pause simulation`, `focus on Anna`, `tell Elsa to go to the forest`, and `tell Pierre to meet Lucas`.
- Matching must be city-scoped and fail closed when required entities cannot be resolved safely.
- Add targeted tests for matcher behavior and refusal cases.

In scope: backend orchestration parsing, command DTO/domain contract, tests.

Out of scope: LLM fallback, provenance UI.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: add an LLM fallback interpreter that produces validated structured commands only when deterministic matching cannot safely resolve the request.

Acceptance criteria:

- Fallback is invoked only after deterministic parsing returns ambiguous or unresolved for an allowed command family.
- Fallback output must map to the structured command schema and pass validation before execution.
- Unsupported, underspecified, or invalid fallback outputs return refusal responses without mutating state.
- Add tests that cover deterministic-first precedence, valid fallback, and refusal on invalid output.

In scope: backend fallback adapter, schema validation, policy checks, tests.

Out of scope: provenance UI, goal execution changes.

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal: expose interpretation provenance so the user can see whether a command was parsed deterministically or required LLM fallback.

Acceptance criteria:

- Orchestration response includes provenance enum such as `DETERMINISTIC_MATCH`, `LLM_FALLBACK`, `REFUSED_UNSUPPORTED`, or `REFUSED_AMBIGUOUS`.
- Backend logs capture provenance and resolved structured command summary.
- Frontend shows a compact notification, log row, or panel entry that reflects interpretation provenance.
- Keep the UX lightweight; do not redesign the page.

In scope: API contract, frontend display, backend logging.

Out of scope: new command families or new simulation logic.
