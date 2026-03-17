# Sprint 24: Human Actions

## Execution status

- Current phase: Completed
- Active chunk: none
- Next chunk: Sprint 25 Task 1a
- Blocked items: none
- Last completed chunk: Task 3a

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | done | Added application-mapped action catalog, deterministic action selector, and integrated action event drafts. |
| Task 2a | done | Added deterministic per-tick action/event budgets with priority-based pacing and regression coverage capping outcomes per tick. |
| Task 3a | done | Enforced non-AI simulation stepping path, added long-run no-LLM determinism coverage, and introduced optional `tribeId` seams for future multi-tribe support. |

## Sprint intent

Sprint 24 makes unlocked applications matter in the simulation while slowing the world down to a believable cadence and keeping the engine fully autonomous without LLM usage.

## Why this sprint comes next

Sprint 23 can unlock applications, but unlocked knowledge has no simulation consequence yet. The next milestone is to turn applications into new action possibilities and enforce turn pacing so the civilization evolves gradually instead of exploding with changes over a few seconds.

## Sprint outcome

At the end of Sprint 24, unlocked applications deterministically expand the action space, each tick executes only a small bounded action budget, and the simulation remains fully autonomous without LLM calls. The new domain design also stays compatible with later `tribeId` support.

## Sprint scope

### In scope

- Define an action catalog unlocked by applications such as `COOK_FOOD`, `TELL_STORIES`, `CREATE_ART`, `STORE_FOOD`, and `TRADE_GOODS`.
- Add deterministic action selection rules that use current goals, unlocked applications, and local context.
- Introduce a bounded per-tick action budget so one step produces only a small number of visible outcomes.
- Ensure unlocks, actions, and events can run for thousands of turns without LLM dependence.
- Add tribe-compatible extension points such as optional `tribeId` fields or abstractions where needed, without implementing full tribe behavior.

### Out of scope

- Full multi-tribe gameplay such as trade, conflict, or diplomacy.
- LLM-authored per-tick decisions.
- Large economy or resource-chain systems.

## Product and technical decisions for this sprint

- **Decision 1:** Action unlocks are deterministic consequences of applications, not free-form AI behaviors.
- **Decision 2:** Turn pacing is a hard simulation rule, not just a frontend delay.
- **Decision 3:** Tribe compatibility is a boundary decision in Sprint 24, not a product feature rollout.

## Deliverables

- Application-unlocked action catalog and deterministic action selection.
- Per-tick pacing budget and gradual progression rules.
- No-LLM autonomous stepping with tribe-ready extension points documented in code and planning.

## Definition of done

- Unlocked applications add new possible actions for humans.
- Each tick produces a small bounded set of actions or events.
- Same seed and same step sequence still produce the same action and progression outcomes.
- The step loop runs autonomously for long simulations without LLM calls.
- Domain changes do not assume a single permanent tribe-less world model.

## Suggested file targets

- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/SimulationApplicationService.java`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/domain/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/human/domain/`
- `docs/specs/chat-goals-tech-tree-spec.md`

## Features and task breakdown

### Feature 1: Application-unlocked actions

**Goal:** Turn unlocked applications into concrete deterministic action possibilities.

**Acceptance criteria:** Action catalog defined; applications map to action families; selection logic deterministic and testable.

**Best owner:** Codex.

### Feature 2: Turn pacing

**Goal:** Slow simulation evolution by bounding what can happen in one tick.

**Acceptance criteria:** Per-tick budget implemented; discoveries and inventions emerge gradually; no bursty multi-system spam in one step.

**Best owner:** Codex.

### Feature 3: Autonomous stepping and tribe-ready boundaries

**Goal:** Keep the engine autonomous and prepare future tribe support without delivering it now.

**Acceptance criteria:** No LLM calls in the step loop; extension points for future `tribeId` support are explicit; tests cover long-run determinism.

**Best owner:** Codex.

## Recommended implementation order

1. Task 1a: Action catalog and deterministic selection rules.
2. Task 2a: Per-tick pacing budget and gradual progression constraints.
3. Task 3a: Long-run autonomy checks and tribe-ready boundary work.

## Dependencies inside the sprint

- Task 1a depends on Sprint 23 application unlock state.
- Task 2a depends on Task 1a because pacing must apply to the new action set.
- Task 3a depends on Tasks 1a and 2a.

## Suggested delegation

- **Codex:** Backend action model, pacing rules, determinism tests.
- **You:** Approve action families and whether `tribeId` lands as a field now or as a documented abstraction seam.

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Application-unlocked action catalog | Codex | Applications unlock deterministic action families that the step loop can evaluate. |
| Task 2a | Turn pacing budget | Codex | Each tick has a bounded action budget and progression slows to a believable cadence. |
| Task 3a | Autonomous long-run stepping + tribe-ready seams | Codex | Long simulations run without LLM; future tribe support has explicit extension points. |

## Risks

- If pacing is implemented as arbitrary throttling instead of rule budget, the simulation will feel inconsistent.
- Adding `tribeId` too aggressively could drag Sprint 24 into a premature social-systems rewrite.

## Handoff to next sprint

The next milestone after Sprint 24 can branch into tribe interactions, richer economy, or UI storytelling, but only after command routing, goals, knowledge progression, and pacing are stable.
