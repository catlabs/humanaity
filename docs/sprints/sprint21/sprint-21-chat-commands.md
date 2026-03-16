# Sprint 21: Chat Commands

## Execution status

- Current phase: Sprint 21 complete
- Active chunk: none
- Next chunk: none
- Blocked items: none
- Last completed chunk: Task 3a (2026-03-16)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Added structured command types and deterministic matcher coverage for step, pause, focus, move-to-place, and meet-human parsing with fail-closed resolution tests. |
| Task 2a | done | Added LLM fallback for ambiguous move-style requests with schema validation, deterministic-first precedence, and refusal on invalid output. |
| Task 3a | done | Added response provenance fields, backend interpretation logs, and a compact provenance line in the simulation chat UI. |

## Sprint intent

Sprint 21 converts chat command handling from mostly agentic interpretation into an explicit hybrid pipeline: deterministic parsing first, LLM fallback only when needed, and visible provenance for token-using commands.

## Why this sprint comes next

The repo already has chat orchestration, move-to-place commands, and rule-based simulation triggers. The next architectural gap is command routing discipline: the system needs to stop treating every natural-language request as equivalent and expose when an LLM was required.

## Sprint outcome

At the end of Sprint 21, HUMANAIty accepts a bounded set of structured chat commands through deterministic matching, falls back to the LLM only for ambiguous but allowed requests, and reports interpretation provenance to users and logs.

## Sprint scope

### In scope

- Define structured backend command types for deterministic chat handling.
- Add deterministic matching for commands such as `step`, `pause`, `focus`, `move to place`, and `meet human`.
- Add an LLM fallback adapter that can propose a structured command only when deterministic matching cannot safely resolve intent.
- Validate fallback output against command schema and city policy before execution.
- Return interpretation provenance to backend logs and frontend feedback.

### Out of scope

- Goal execution changes inside the simulation step loop.
- New knowledge progression logic.
- Large UI redesign beyond provenance display.

## Product and technical decisions for this sprint

- **Decision 1:** Deterministic parsing wins whenever a command can be matched with sufficient confidence; the LLM is not allowed to override a valid deterministic parse.
- **Decision 2:** LLM fallback returns structured command candidates, never direct free-form mutations.
- **Decision 3:** Unsupported or ambiguous requests fail closed with a clear refusal message instead of guessing.

## Deliverables

- Structured command schema and deterministic matcher.
- LLM fallback interpreter with validation and refusal path.
- Provenance fields in orchestration response and visible UI messaging.

## Definition of done

- Common commands such as `step 10`, `pause simulation`, `focus on Anna`, and `tell Pierre to meet Lucas` resolve without LLM usage.
- Ambiguous but supported requests can use validated LLM fallback to return a structured command.
- API response and UI clearly indicate `DETERMINISTIC_MATCH` or `LLM_FALLBACK`.
- Logs make token-using interpretations auditable.

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/application/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/agent/api/`
- `docs/specs/agent-chat-orchestration-spec.md`
- `docs/specs/chat-goals-tech-tree-spec.md`
- `apps/ui/src/app/features/city/`

## Features and task breakdown

### Feature 1: Structured command contract and deterministic matcher

**Goal:** Parse the common safe command families without LLM usage.

**Acceptance criteria:** Structured command types defined; deterministic matcher handles common phrasing variants; unsupported commands fail closed.

**Best owner:** Codex.

### Feature 2: Validated LLM fallback

**Goal:** Use the LLM only when deterministic parsing cannot safely resolve an allowed command.

**Acceptance criteria:** Fallback produces structured command candidate; schema validation and policy checks pass before execution; ambiguous output is refused.

**Best owner:** Codex.

### Feature 3: Interpretation transparency

**Goal:** Show users and developers how a command was interpreted.

**Acceptance criteria:** Response includes provenance; logs record it; UI displays snack bar, log row, or equivalent feedback.

**Best owner:** Cursor or Codex.

## Recommended implementation order

1. Task 1a: Structured command types and deterministic matcher.
2. Task 2a: LLM fallback adapter, schema validation, refusal semantics.
3. Task 3a: Provenance in API, logs, and UI.

## Dependencies inside the sprint

- Task 2a depends on Task 1a.
- Task 3a depends on Tasks 1a and 2a.

## Suggested delegation

- **Codex:** Command schema, deterministic matcher, fallback validation, orchestration tests.
- **Cursor:** Frontend provenance display and light UX wiring.

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Structured command schema + deterministic matcher | Codex | Safe commands resolve without LLM; unsupported requests fail closed. |
| Task 2a | Validated LLM fallback | Codex | Ambiguous requests use fallback only when needed; output is schema-validated and policy-checked. |
| Task 3a | Provenance logging and UI feedback | Cursor / Codex | API, logs, and UI show deterministic vs fallback interpretation. |

## Risks

- Matcher breadth can sprawl quickly; keep Sprint 21 limited to high-value command families.
- If fallback validation is weak, the system will reintroduce hidden nondeterministic behavior through orchestration.

## Handoff to next sprint

Sprint 22 will add a real goal model so structured commands can assign durable intentions instead of directly teleporting or mutating humans in ad hoc ways.
