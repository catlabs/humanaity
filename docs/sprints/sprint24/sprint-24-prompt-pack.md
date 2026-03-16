# Sprint 24 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 24 task only.

References for every task:

- `docs/roadmap.md` (Epics 16-19)
- `docs/specs/chat-goals-tech-tree-spec.md`
- `docs/sprints/sprint24/sprint-24-human-actions.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 24:

- unlocked applications expand deterministic action possibilities
- turn pacing is enforced in the backend step loop
- no LLM calls inside autonomous stepping

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal: turn unlocked applications into a deterministic action catalog the simulation can use.

Acceptance criteria:

- Define action families such as `COOK_FOOD`, `TELL_STORIES`, `CREATE_ART`, `STORE_FOOD`, and `TRADE_GOODS`.
- Map applications to unlocked action families.
- Add deterministic action selection rules that consider active goals, available unlocks, and local context.
- Add tests that show the same seed and state produce the same selected actions.

In scope: backend action model, selection rules, tests.

Out of scope: tribe interactions, UI storytelling.

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal: slow the simulation down by introducing a bounded per-tick action budget and gradual progression rules.

Acceptance criteria:

- Each tick can execute only a small bounded number of outcomes.
- Discoveries, inventions, and unlocked-action effects no longer burst unrealistically over a few steps.
- Pacing is deterministic and enforced in the backend, not simulated by UI delay.
- Add regression coverage for pacing behavior.

In scope: simulation step budget, ordering rules, tests.

Out of scope: LLM narration, tribe feature rollout.

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal: preserve autonomous no-LLM simulation stepping while leaving clean extension points for future tribe support.

Acceptance criteria:

- No LLM calls occur inside the simulation step loop.
- Long-run stepping remains deterministic for many turns with unlocks and actions enabled.
- Add or document clean extension points for future `tribeId` support without implementing multi-tribe behavior now.
- Update planning/spec references if needed to keep tribe support explicitly deferred.

In scope: backend determinism checks, extension seams, documentation alignment.

Out of scope: trade, conflict, cultural exchange mechanics.
