# Testing strategy

## Purpose

Testing in HumanAIty is meant to protect the parts of the project that carry the most product risk:

- core flows that must keep working: auth, city management, simulation, history, agent-driven control
- deterministic simulation behavior that should not drift silently
- backend contracts consumed by the UI and MCP
- ownership, authorization, and other user-facing safety boundaries

This is a personal project. The goal is not maximum coverage. The goal is fast feedback on the failures that would make the project feel broken or misleading.

## Testing principles

- test the flows that matter more than the code that is easy to count
- prefer a few strong tests over many weak ones
- put most test depth where the product logic really lives
- keep tests readable enough to help future changes, not fight them

## Current test layers

### Backend

The backend currently carries most of the test depth.

It already includes tests around:

- simulation determinism and reproducibility
- simulation history and read-model API contracts
- city authorization rules
- agent chat API behavior and safe command handling
- selected simulation services and catalog loaders
- application startup

This matches the repository shape well: the backend owns most of the important domain rules.

### Frontend

The frontend currently has a small, focused set of specs.

Today those tests mostly cover:

- simulation detail page behavior
- selected city feature services and view-model logic
- app bootstrap-level behavior

This means the UI is tested selectively, not exhaustively. That is reasonable for the project right now.

### MCP

MCP is treated as a real consumption surface, not just a helper script.

Current MCP coverage is mainly:

- contract drift checking against backend OpenAPI
- TypeScript build validation
- one end-to-end smoke path through auth, city access, simulation stepping, and history timeline

This gives confidence that backend features remain reachable through MCP without requiring the UI.

### CI

CI currently runs three jobs:

- backend: Maven tests
- frontend: install, production build, browser tests
- MCP: backend startup, OpenAPI drift check, MCP build, MCP smoke

CI is the project gate for baseline quality. It is not trying to prove everything. It is trying to catch obvious regressions early and cheaply.

## Failures that matter most

The most important failures in this project are:

- auth or ownership bugs that expose the wrong city or allow the wrong action
- simulation regressions that break determinism or corrupt history outputs
- backend contract changes that silently break the UI or MCP
- agent chat regressions that break safe command handling or structured UI-facing responses
- MCP regressions that make important backend flows unreachable outside the UI

These deserve the strongest test attention.

## Test deeply

The project should keep investing most of its test effort in:

- backend domain rules and state transitions
- deterministic simulation behavior
- API contracts that drive the frontend and MCP
- authorization and ownership boundaries
- agent command parsing, safe execution, and structured responses
- MCP smoke paths for critical user journeys

When these areas change, adding or updating tests should be the default.

## Test selectively

The project should test these areas selectively rather than broadly:

- frontend component rendering
- frontend service orchestration
- secondary MCP tools that are not part of the main smoke path
- non-critical edge cases that duplicate coverage already present in backend tests

The rule here is simple: test enough to protect important user-visible behavior, but do not build a heavy UI test suite just for coverage numbers.

## Not prioritized yet

These areas can wait unless they become a source of repeated regressions:

- large browser end-to-end suites
- visual snapshot testing
- exhaustive component-level frontend tests
- live OpenAI-backed CI tests
- coverage thresholds and coverage-driven work
- broad performance or load testing

For now, these would add more maintenance cost than product value.

## How testing supports project goals

HumanAIty is trying to prove a few things well:

- the simulation stays coherent and reproducible
- the backend remains the source of truth
- the same core capabilities can be exercised from UI and MCP
- agent-driven workflows stay safe and predictable enough for iteration

The testing approach should stay aligned with those goals. If a test does not protect one of those goals, it is probably low priority.

## How CI fits

CI should stay simple and readable.

Its job is to answer a few practical questions on every push and pull request:

- does the backend still work?
- does the frontend still build and pass its focused tests?
- does MCP still match backend contracts and complete a critical smoke path?

That is enough for the current stage of the project. CI should grow only when new product risk appears, not because a larger pipeline sounds more complete.

## When tests should change

Tests should change when the behavior, contract, or risk level changes.

In practice, that usually means:

- add or update tests when a core flow changes
- update tests when a backend contract or MCP behavior changes
- add a regression test when a bug is fixed and could easily come back
- remove or simplify tests when they are brittle and no longer protect an important risk

## How this should evolve

As the project grows, testing should grow where real risk grows.

Good reasons to add more testing:

- a core flow starts regressing often
- a backend contract changes frequently and breaks consumers
- MCP becomes a more important public or demo surface
- the frontend starts carrying more business logic
- AI-assisted features move from optional/demo behavior to core behavior

When that happens, prefer adding focused tests near the risky area before adding a new generic testing layer.
