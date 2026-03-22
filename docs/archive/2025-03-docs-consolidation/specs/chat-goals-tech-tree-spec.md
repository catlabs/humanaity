# Chat, Goals, and Tech Tree Spec

## Status

- Status: LOCKED for future planning
- Scope anchor: `docs/archive/roadmap-legacy.md` Epics 16-19
- Applies to: backend command interpretation, simulation goal lifecycle, knowledge progression, and autonomous turn pacing

## Purpose

This spec fixes the architectural rules for the next simulation-depth milestone after the rule-based event system.

It exists to prevent follow-up implementation from mixing:

- chat interpretation with simulation execution
- LLM narration with deterministic world mutation
- fast event spam with slow turn-based civilization progression

## Core Rules

1. Deterministic simulation remains canonical.
2. The simulation turn loop must run without LLM calls.
3. Chat interpretation must use deterministic parsing first, then LLM fallback only when deterministic parsing cannot safely resolve intent.
4. Successful chat interpretation may assign goals, but goals are executed later by deterministic simulation rules.
5. Discoveries, inventions, applications, and unlocked actions must come from deterministic rule evaluation and config data.

## Command Interpretation Pipeline

Required flow:

1. User message enters backend orchestration.
2. Deterministic matcher attempts to resolve a structured command.
3. If confidence is sufficient, execute that command directly.
4. If ambiguous but allowed, invoke LLM fallback to produce a structured command candidate.
5. Validate the candidate against deterministic command schema and city-scoped policy before execution.
6. Return interpretation provenance to the UI and log it.

## Allowed Interpretation Provenance

- `DETERMINISTIC_MATCH`
- `LLM_FALLBACK`
- `REFUSED_UNSUPPORTED`
- `REFUSED_AMBIGUOUS`

The response contract should expose provenance so the UI can tell the user whether tokens were used.

## Goal Model

Goals are deterministic backend state attached to a human.

Minimum goal types:

- `MOVE_TO_PLACE`
- `MEET_HUMAN`
- `BUILD_STRUCTURE`
- `EXPLORE_AREA`
- `FOLLOW_HUMAN`

Minimum goal fields:

- `goalId`
- `humanId`
- `goalType`
- `status`
- `targetPlaceId` or `targetHumanId` or structured target payload
- `assignedTick`
- `sourceType` (`AUTONOMOUS`, `CHAT_COMMAND`, `DIRECTOR_INTERVENTION`)

## Goal Execution Rules

- A goal is assigned outside the turn loop or by deterministic in-loop rules.
- The step engine reads current goals and updates movement/action choices deterministically.
- Goal progress and completion must emit deterministic events.
- Humans without active goals must enter deterministic autonomous reassignment rather than perpetual random drift.
- After goal completion, humans may remain stationary for a bounded deterministic dwell window before the next assignment.
- Movement must clamp to world bounds and avoid edge-sticking behavior.

## Place-Aware Movement Rule

The place model from Sprint 18 remains the world anchor.

- New movement targets should prefer place ids or human ids, not raw free-form coordinates.
- Chat commands like "go to the forest" should resolve to a place id before goal assignment.
- Future world expansion must preserve stable place identity even if rendering changes.

## Knowledge Progression Model

Knowledge progression is:

`events or situations -> discoveries -> inventions -> applications`

The progression graph must live in a versioned config file such as `tech-tree.json`.

Each node must declare:

- stable id
- type (`DISCOVERY`, `INVENTION`, `APPLICATION`)
- prerequisite ids
- optional triggering contexts
- unlocked actions for applications

## Application Unlock Rule

- Applications do not directly call the LLM.
- Unlocking an application adds deterministic action possibilities to the simulation.
- Example unlocked action families: `COOK_FOOD`, `TELL_STORIES`, `CREATE_ART`, `STORE_FOOD`, `TRADE_GOODS`.

## Turn Pacing Rules

- One step means one small world update budget, not a burst of many unrelated actions.
- Each tick should execute only a small bounded set of outcomes.
- Knowledge progression must emerge gradually across many ticks.
- Action scheduling and unlock evaluation must remain deterministic for a fixed seed and step sequence.

## Tribe Compatibility Rule

Multi-tribe simulation is not in first implementation scope, but new domain design must stay compatible with it.

Required compatibility points:

- humans may later carry `tribeId`
- goals, events, and unlocked actions must not assume one global tribe
- place, discovery, and application logic should remain city-scoped and tribe-compatible

Current seam:

- `Human.tribeId` is optional and nullable; no tribe behavior is executed yet

## Explicitly Out of Scope

- Using the LLM inside simulation stepping
- Generative autonomous agents deciding per-tick actions
- Full economy, warfare, or diplomacy systems
- Final content authoring for the tech tree
