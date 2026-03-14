# Agent Chat Orchestration Spec (Epic 9 / Epic 10)

## Status

- Status: LOCKED for planning
- Scope anchor: `docs/sprints/sprint08/sprint-08-agent-chat-mvp.md`
- Follow-on anchors:
  - `docs/sprints/sprint09/sprint-09-agentic-simulation-console.md`
  - `docs/sprints/sprint10/sprint-10-guided-human-workflows.md`
  - `docs/sprints/sprint11/sprint-11-controlled-director-interventions.md`
- Applies to: backend-owned agent orchestration and chat-driven simulation UX in `apps/backend` and `apps/ui`

## Purpose

This document fixes the product and architecture rules for HUMANAIty's agent chat direction.

If implementation choices conflict with this spec during the new console and orchestration sprints, this spec is the source of truth.

## Primary Product Goal

HUMANAIty should converge on one flagship experience:

- a live simulation map
- a lightweight context/timeline surface
- an agent chat panel as the primary interaction loop

The user should be able to type natural-language requests such as:

- "Advance the city by 5 steps"
- "Summarize what is happening in the city"
- "Explain this event"
- "Show me the latest inventions"

and receive a clear reply plus visible UI updates grounded in deterministic simulation state.

## Product Framing Rule

The chat layer is not a separate toy interface beside the simulation.

It is the main command-and-explanation surface for the primary simulation page.

The product should feel like:

- an agentic simulation console
- an AI-native observation surface
- one coherent portfolio story instead of several loosely related pages

## Authority Rules

### Deterministic simulation remains canonical

Canonical world state still lives in backend simulation/history domains.

Canonical facts include:

- simulation run state
- humans and their positions
- events and inventions
- years, eras, ticks, and deterministic ordering
- any explicit intervention records once Epic 10 begins

AI does not become canonical world state.

### Agent orchestration is an interpreter, not a source of truth

The orchestration layer may:

- interpret user intent
- choose from allowed commands
- invoke deterministic actions or reads
- summarize and explain results
- return UI effects

It must not:

- silently invent canonical facts
- mutate simulation state outside allowed command boundaries
- hide whether an action was a normal simulation step versus an explicit intervention

### MCP is a tool layer, not the application agent

MCP remains valuable for:

- parity with backend capabilities
- external agent access
- smoke validation without the UI
- reusable tool semantics

But the preferred application runtime path is:

- Angular UI
- backend orchestration endpoint/service
- backend application services and/or MCP-aligned tool adapters
- UI refresh via backend-owned response contracts

Direct frontend-to-MCP orchestration is not the target architecture.

## Preferred Architecture

### Required flow

The preferred interaction flow is:

1. frontend sends a city-scoped chat request to backend orchestration
2. backend authenticates user ownership/access
3. orchestration classifies the command into an allowed command class
4. orchestration executes one or more backend service calls and/or MCP-aligned tool calls
5. backend returns:
   - user-facing message
   - executed action summary
   - referenced entities
   - UI effects and refresh instructions
6. frontend applies effects and refreshes the relevant simulation state

### Recommended backend placement

Add a new backend slice under a dedicated package such as:

- `eu.catlabs.humanaity.agent.api`
- `eu.catlabs.humanaity.agent.application`
- `eu.catlabs.humanaity.agent.domain`

Keep it separate from:

- raw simulation controller responsibilities
- UI-only composition logic
- MCP server runtime code

### Execution strategy rule

For core simulation commands owned by the backend domain, prefer direct backend application-service and read-model calls inside orchestration.

Use MCP-aligned adapters when they help preserve external-tool parity or keep command semantics shared, but do not force the backend to proxy through a separate MCP process for in-process simulation actions that already live in the same monorepo.

## Command Classes

## Phase 1: Safe MVP commands

These are the first commands the orchestration layer should support:

- advance simulation by `N` deterministic steps
- summarize recent city changes
- explain one event
- show recent inventions
- show latest city snapshot/state

Rules:

- commands stay city-scoped
- step counts are explicit and bounded
- all outputs must remain traceable to backend-owned state
- UI refresh is automatic after commands that mutate simulation progress

## Phase 2: Guided commands

These commands are allowed after the MVP loop is stable:

- focus on a human
- compare two humans
- follow one human for bounded ticks

Rules:

- these remain observation-first workflows
- orchestration may need extra read-model helpers and UI effects
- guided commands should not be framed as simulation interventions

## Phase 3: Director commands

These are explicit interventions, not normal read or control commands:

- make two humans meet
- trigger a guided interaction
- inject a controlled intervention into the simulation

Rules:

- treat them as explicit simulation interventions
- require extra policy checks and confirmation semantics
- record provenance and make intervention status visible in UI/history
- never blur them with autonomous world behavior

## Response Contract Rule

The orchestration endpoint must return a UI-friendly contract rather than raw tool traces.

Recommended response shape:

```json
{
  "conversationId": "string",
  "message": "string",
  "commandClass": "SAFE_MVP | GUIDED | DIRECTOR",
  "executedActions": [
    {
      "type": "STEP_SIMULATION",
      "status": "COMPLETED",
      "summary": "Advanced city by 5 steps"
    }
  ],
  "referencedEntities": {
    "cityId": 1,
    "humanIds": [12],
    "eventIds": [55],
    "inventionIds": [9]
  },
  "uiEffects": [
    { "type": "REFRESH_SNAPSHOT" },
    { "type": "REFRESH_TIMELINE", "fromTick": 120 },
    { "type": "FOCUS_HUMAN", "humanId": 12 },
    { "type": "HIGHLIGHT_EVENT", "eventId": 55 }
  ],
  "snapshot": null
}
```

## Required response semantics

### `message`

- always user-facing
- summarizes what happened or why the command was refused
- should be readable without exposing tool internals

### `executedActions`

- lists what the orchestration layer actually did
- must distinguish read actions from simulation mutations
- director actions must be labeled explicitly as interventions

### `referencedEntities`

- gives the frontend stable ids to bind focus/highlight behavior
- avoids requiring the UI to parse free text for identifiers

### `uiEffects`

The backend may suggest effects such as:

- `REFRESH_SNAPSHOT`
- `REFRESH_TIMELINE`
- `FOCUS_HUMAN`
- `HIGHLIGHT_EVENT`
- `HIGHLIGHT_INVENTION`
- `SELECT_PANEL`

The frontend owns visual rendering of these effects, but the backend owns when they are suggested.

### `snapshot`

Optional.

Recommended rule:

- include an inline snapshot only when the command already fetched or mutated simulation state and returning it cheaply improves UX
- otherwise return refresh effects and let the frontend refetch through existing generated clients

This keeps the contract pragmatic without forcing every chat reply to embed large payloads.

## Request Contract Rule

The request must remain simple and city-scoped.

Recommended minimum fields:

```json
{
  "message": "Advance the city by 5 steps",
  "conversationId": "optional-string",
  "selectedHumanId": 12,
  "selectedEventId": 55
}
```

Rules:

- `message` is required
- city scope should come from the route/path rather than free-form message parsing
- selected ids are optional context, not authority
- the backend must not trust UI context alone for authorization or command eligibility

## UI Rules

### Primary page rule

The chat panel belongs on the primary city simulation page, not a separate experimental route.

The page should converge toward:

- map as the dominant visual surface
- chat as the dominant interaction surface
- lightweight context/timeline support rather than many equal-priority panels

### Refresh rule

After any allowed action that changes simulation state:

- the backend should return refresh effects
- the UI should refresh snapshot/history from backend-owned endpoints
- optimistic local state must not replace backend truth

### Selection/focus rule

When the backend references humans, events, or inventions:

- the UI may focus/highlight/select them
- the effect must be reversible and non-destructive
- the highlighted entity must still come from backend-owned ids

## Auth and security rules

- orchestration endpoints must require the same authenticated city ownership model as the rest of the product
- allowed commands must be whitelisted by command class
- step counts and time windows must be bounded
- invalid or disallowed commands must fail clearly with a safe user-facing message
- director commands require stricter checks and explicit confirmation semantics once implemented

Deferred but recommended later:

- rate limiting
- conversation/session persistence
- richer audit logging
- delegated/service access patterns aligned with later OIDC work

## Architecture options considered

### Option A: direct frontend-to-MCP orchestration

Rejected as the preferred target because:

- it makes command policy and auth harder to control
- it leaks tool-shaping concerns into the UI runtime
- it makes UI effects and response contracts less stable

### Option B: backend orchestration endpoint with direct backend service execution

Preferred for core simulation reads/actions because:

- it matches existing backend ownership of auth and simulation state
- it can reuse application services and read models directly
- it can shape a UI-specific contract cleanly

### Option C: backend orchestration endpoint that can also use MCP-aligned adapters

Recommended as a secondary pattern when:

- parity with external-agent tooling matters
- a workflow already exists naturally as a tool composition
- we want to keep backend and MCP capabilities conceptually aligned

## Deferred for later sprints

- free-form autonomous agent loops
- multi-city chat coordination
- intervention scripting
- broad conversation memory productization
- standards-based external-agent auth rollout
- replacing the deterministic simulation UI with chat-only interaction

## Implementation guardrails

- keep deterministic simulation and history canonical
- keep orchestration backend-owned
- keep MCP as a tool layer, not the product's only runtime backend
- start with one strong end-to-end city demo
- prefer narrow command classes over open-ended "do anything" agent behavior
- make interventions explicit and auditable once they exist

## Done signals for the new direction

This direction is implemented coherently only if:

- one primary simulation page owns the map + chat experience
- safe MVP commands work end to end through a backend-owned orchestration path
- backend responses contain user-facing messages plus UI effects, not raw tool traces
- simulation state refresh stays grounded in backend-owned endpoints
- guided commands remain distinct from interventions
- director commands, when added, are visibly labeled as explicit interventions
