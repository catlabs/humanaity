# Sprint 7: MCP Agent Workflows

## Execution status

- Current phase: Sprint 7 planned
- Active chunk: `Task 1a` - scope and workflow lock
- Next chunk: `Task 2a` - read-tool normalization
- Blocked items: none
- Last completed chunk: none

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | planned | Lock Sprint 7 to agent-facing MCP workflow improvements on top of existing backend read models. |
| Task 2a | planned | Normalize touched simulation-tool outputs and shared response helpers so read flows are consistently machine-readable. |
| Task 2b | planned | Add agent-oriented explanation and city-change summary tools using existing snapshot/history contracts where possible. |
| Task 3a | planned | Document the polished MCP demo flow, update README/examples, and capture any narrow contract gaps discovered during implementation. |
| Task 4a | planned | Run MCP build plus a focused smoke flow, then sync sprint status and residual risks. |

## Sprint intent

Sprint 7 exists to turn HUMANAIty's already-functional MCP server into a polished, agent-native product surface.

The repo already exposes the core backend read models through MCP: snapshot, timeline, events, inventions, and overview. This sprint should not re-do that work. Its purpose is to make those capabilities easier for agents to consume in one pass by adding higher-level read workflows, consistent JSON responses, and one polished demo path that can summarize what changed in a city over a bounded tick window.

This sprint is MCP-first and read-oriented. It should avoid dragging in broad backend, frontend, or auth implementation unless a very small contract gap makes the MCP milestone impossible.

## Why this sprint comes next

Sprint 6 tightened platform seams around ownership, config, migration, and future auth direction. That makes this a good point to improve the portfolio-differentiating surface the roadmap already calls out: agent workflows over the same world model exposed to the UI.

If Sprint 7 is skipped:

- the MCP server remains technically useful but less polished than the frontend-facing product story
- agents still need to stitch together several low-level reads to answer simple questions like "what changed in this city recently?"
- read-tool responses stay uneven across tools, which makes automation and demos more brittle
- Epic 7 remains only partially delivered even though most underlying backend contracts already exist

## Sprint outcome

At the end of Sprint 7, HUMANAIty should expose a clean MCP workflow for simulation exploration: agents can inspect a city snapshot, query bounded history, explain a specific event from backend-owned facts, and ask for a concise summary of city changes over the last N ticks without inventing their own joins across raw tool outputs.

## Sprint scope

### In scope

- lock the Epic 7 boundary against the current repo and existing MCP surface
- keep the sprint focused on MCP read workflows rather than broad platform work
- normalize touched MCP tool responses so machine-readable JSON is reliable in both `content` and `structuredContent`
- add one event-explanation tool using existing history payloads and enrichment fields where available
- add one bounded city-changes summary tool built from existing snapshot/timeline/history contracts
- keep the MCP workflow mostly read-oriented, with only the minimum auth/city setup needed for smoke validation
- document one polished demo flow for "summarize city changes over the last N ticks"
- validate the touched MCP path with a real build and focused smoke pass

### Out of scope

- broad backend read-model redesign
- new frontend work
- full OIDC/OAuth2 implementation for MCP
- background agents, orchestration loops, or long-running autonomous behavior
- replacing deterministic history semantics with AI-authored canonical facts
- broad tool-suite rewrite outside the touched simulation/agent workflow path

## Product and technical decisions for this sprint

### Global decision: build agent workflows on top of existing backend-owned reads

Sprint 7 should treat backend snapshot, overview, events, inventions, and timeline contracts as canonical.

The MCP layer may compose, summarize, and explain those contracts for agent usability, but it should not recreate a second product semantic model in TypeScript.

### Decision 1: stay read-first

Epic 7 is about exploration and explanation, not control-plane expansion.

This sprint should avoid adding new mutation-heavy MCP tools unless a tiny setup helper is required for validation. The main value is helping agents understand simulation state quickly and safely.

### Decision 2: explanation must stay traceable to deterministic facts

An event-explanation tool should explain what an event means from the backend-owned payload, actor ids, type/category, year/era, and enrichment fields already returned by history endpoints.

Do not introduce a new free-form LLM dependency in the MCP layer for this sprint. If enriched text already exists from Sprint 5, it can be incorporated as supporting context, not as a replacement for deterministic event facts.

### Decision 3: summary tools must be bounded and explicit

The "city changes over the last N ticks" workflow should use explicit windows such as `fromTick`, `toTick`, or `lastTicks`.

Avoid unbounded history reads or vague summarization behavior. Agents should be able to predict what source data a summary represents.

### Decision 4: touched MCP tools must emit machine-readable JSON consistently

For every Sprint 7-touched MCP tool, `content[0].text` should carry JSON that mirrors the primary structured payload shape closely enough for clients that only surface text.

This sprint does not need to refactor every existing tool in the server, but the simulation/agent workflow path touched here should end in a consistent response pattern.

### Decision 5: prefer MCP-local composition before adding backend endpoints

Most of Epic 7 can be delivered by composing existing backend endpoints and contracts in `apps/mcp`.

Only introduce a backend contract change if the current read models are provably insufficient for a minimal explanation or summary workflow.

### Decision 6: auth modernization remains deferred

Sprint 6 already documented the OIDC/OAuth2 direction. Sprint 7 may note how the new tools fit delegated or service-mode MCP usage, but it must not expand into real identity-provider integration.

## Deliverables

By the end of Sprint 7, the repo should contain:

- a Sprint 7 execution doc with a locked MCP/agent-workflow boundary
- one MCP event-explanation tool on top of backend-owned history data
- one MCP city-changes summary tool for a bounded recent tick window
- normalized JSON output for the touched simulation/agent workflow tools
- README or promptable usage examples for the polished MCP demo flow
- validation notes proving the touched MCP path builds and works end to end

## Definition of done

Sprint 7 is done only if all of the following are true:

- agents can retrieve a city snapshot and bounded history without relying on UI-only behavior
- the Sprint 7 explanation tool clearly ties its output back to backend-owned event fields
- the Sprint 7 summary tool can describe recent city changes from an explicit bounded window
- touched MCP tool responses are machine-readable in both `structuredContent` and primary text output
- the main agent demo flow is documented clearly enough to run from repo context alone
- MCP build and at least one focused smoke validation path have been recorded in the sprint status

## Suggested file targets

These are the most likely files or folders Sprint 7 will touch:

- `apps/mcp/src/tools/simulation-tools.ts`
- `apps/mcp/src/backend-client.ts`
- `apps/mcp/src/contracts.ts`
- `apps/mcp/src/errors.ts`
- `apps/mcp/src/index.ts`
- `apps/mcp/README.md`
- `docs/sprints/sprint07/`

Likely new code areas:

- `apps/mcp/src/tools/` helper functions for JSON output normalization or history summarization
- `docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md`
- `docs/sprints/sprint07/sprint-07-prompt-pack.md`

## Features and task breakdown

## Feature 1: Sprint boundary and workflow contract lock

### Goal

Translate Epic 7 into a repo-specific, small milestone centered on agent-facing MCP workflows rather than a generic "improve MCP" bucket.

### Tasks

1. Lock the exact Sprint 7 outcome against the current MCP surface.
2. State which existing tools count as already delivered groundwork.
3. Define the minimum explanation and summary workflows required for this sprint.
4. Record explicit non-goals to prevent backend/auth sprawl.

### Acceptance criteria

- a contributor can tell what Sprint 7 adds beyond the already-existing MCP tools
- the sprint doc separates MCP composition work from deferred backend or auth work

### Best owner

- You

## Feature 2: Read-tool normalization for agent consumption

### Goal

Make the touched simulation read path consistent enough that agents can consume it without ad hoc response parsing.

### Tasks

1. Audit the Sprint 7-touched simulation tools for inconsistent text output versus `structuredContent`.
2. Introduce or reuse a shared JSON response pattern for the touched path.
3. Keep naming, identifiers, and bounded query inputs aligned with current backend contracts.

### Acceptance criteria

- touched MCP read tools expose reliable JSON payloads in text and structured forms
- the implementation reduces response-shape drift across the Sprint 7 workflow path

### Best owner

- Codex

## Feature 3: Event explanation and recent-changes summary tools

### Goal

Add higher-level agent workflows that turn raw history reads into usable city understanding without inventing new canonical semantics.

### Tasks

1. Add an event-explanation tool that accepts a city-scoped event reference and explains the event from deterministic payload fields.
2. Add a city-changes summary tool that uses explicit tick bounds or `lastTicks` to summarize recent history.
3. Reuse existing snapshot/timeline/events/inventions reads instead of duplicating backend projection logic in the MCP layer.
4. Keep outputs concise, traceable, and useful in one tool call.

### Acceptance criteria

- an agent can ask what a specific event means without manually decoding raw payload fields
- an agent can ask what changed recently in a city without manually joining multiple low-level tools

### Best owner

- Codex

## Feature 4: Demo flow documentation and validation

### Goal

Make the Sprint 7 path easy to demo, review, and smoke-test from repo context alone.

### Tasks

1. Document a recommended MCP demo flow from auth/city selection through recent-history summary.
2. Update README examples or nearby docs for the new tool usage.
3. Run MCP build and a focused smoke flow for the touched tools.
4. Record residual risks and deferred follow-on work in the sprint status.

### Acceptance criteria

- the main Sprint 7 demo flow can be run without guessing tool order or inputs
- MCP validation results and remaining gaps are captured in the sprint doc

### Best owner

- Cursor chat and Codex

## Recommended implementation order

1. Lock Sprint 7 scope and workflow semantics.
2. Normalize the touched read-tool output pattern.
3. Add the event-explanation tool.
4. Add the bounded city-changes summary tool.
5. Update README/examples for the polished demo flow.
6. Run MCP build and one focused smoke validation flow.

## Dependencies inside the sprint

- Feature 1 blocks the rest of the sprint.
- Feature 2 should land before or alongside Feature 3 so new tools inherit the same output pattern.
- Feature 3 depends on the existing Sprint 3 and Sprint 5 contracts remaining available through `apps/mcp`.
- Feature 4 depends on at least one explanation/summary path being implemented.

## Suggested delegation

### Best tasks for you

- lock the exact definition of an acceptable agent-facing workflow
- decide how much interpretation is acceptable in the explanation tool
- keep scope disciplined if backend/auth expansion is proposed

### Best tasks for Cursor chat

- README/example updates
- narrow MCP wiring cleanup
- chunk-level validation and sprint-doc sync

### Best tasks for Codex

- response normalization helpers
- event-explanation tool implementation
- bounded recent-history summary logic
- focused MCP smoke validation and regression-safe refactors

## Ready-to-delegate task list

| Task ID | Title | Best owner | Done condition |
| --- | --- | --- | --- |
| Task 1a | Sprint 7 boundary and workflow lock | You | Sprint doc clearly defines what Epic 7 adds beyond existing MCP tooling. |
| Task 2a | Simulation read-tool normalization | Codex | Touched MCP workflow tools emit consistent machine-readable JSON in text and structured content. |
| Task 2b | Event explanation and city-changes summary tools | Codex | Agents can explain one event and summarize a bounded recent history window through MCP. |
| Task 3a | README and demo-flow documentation | Cursor chat | Repo docs show a polished Sprint 7 MCP flow with concrete tool order and inputs. |
| Task 4a | Build, smoke validation, and closeout | Codex | MCP build and one focused smoke path are recorded, and Sprint 7 status is updated with residual risks. |

## Risks

- the current backend history payload may be rich enough for explanation but still awkward for stable human-readable summaries, which could pressure the sprint into backend changes
- response normalization can sprawl if applied to the entire MCP server instead of the Sprint 7 workflow path
- smoke validation depends on a working local backend plus credentials, so validation notes must distinguish real product gaps from environment-only blockers
- a summary tool can become vague or overly narrative if it is not kept bounded to explicit source ticks and counts

## Handoff to next sprint

If Sprint 7 lands cleanly, the next follow-on work should be one of these, not all at once:

- broader MCP test automation and contract checks
- external-client auth modernization for delegated/service MCP access
- additional agent workflows once the first explanation/summary path proves useful

What Sprint 7 should leave behind:

- one strong agent-demo narrative on top of the existing simulation world model
- a cleaner pattern for MCP tool JSON responses
- a clear boundary between MCP composition work and deferred auth/platform work
