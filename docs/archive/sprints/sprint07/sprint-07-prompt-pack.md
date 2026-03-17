# Sprint 7 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 7" in a single pass.

Before using a prompt, check `docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md` and keep its `## Execution status` section current so the active chunk, next chunk, and blocked items stay visible in one place.

Each template includes:

- exact task ID
- one-sentence goal
- acceptance criteria copied from `docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md`
- explicit in-scope files
- explicit out-of-scope items
- whether the tool should modify code or only propose a patch

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references in the prompt body:
  - `docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md` for scope and acceptance criteria
  - `docs/roadmap.md` for epic alignment
  - `apps/mcp/README.md` for current MCP conventions
  - `docs/specs/simulation-read-model-spec.md` and `docs/specs/ai-history-enrichment-spec.md` when explanation/summary behavior touches backend-owned history fields
  - relevant `.cursor/rules/*.mdc` files when they contain mandatory policy for the chunk
- do not assume hidden Cursor skills are visible to Codex; restate mandatory constraints directly
- include a hard boundary line: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- Cursor reviews whether the implementation stayed MCP-first and read-oriented
- Cursor checks that touched tool outputs remain machine-readable in both text and structured forms
- if a backend contract change slipped in, Cursor verifies it was truly necessary for the Sprint 7 milestone
- Cursor runs chunk-level validation for the touched MCP path
- Cursor updates sprint docs if implementation changes any sprint-shaping decision

## Per-chunk review/test/handoff checklist

Use `.cursor/rules/docs-chunk-review-loop.mdc` as the standard checklist and go/no-go gate after every chunk implementation.

For Sprint 7, also verify:

- new MCP workflows compose backend-owned reads rather than replacing them with a second semantic model
- explanation output remains traceable to deterministic event fields
- summary output is explicitly bounded by tick range or `lastTicks`
- touched tool outputs remain readable in clients that only show text

When a chunk changes MCP tool behavior, include this handoff gate:

- run `npm run build` in `apps/mcp`
- re-check `apps/mcp/README.md` examples if the public tool surface changed
- validate one focused MCP smoke path for the touched tool flow

---

## Task 1a - Sprint 7 Boundary and Workflow Lock

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Lock Sprint 7 to a small MCP-first milestone that adds agent-facing explanation and summary workflows on top of existing read tools.

Acceptance criteria (copied from sprint doc):
- a contributor can tell what Sprint 7 adds beyond the already-existing MCP tools
- the sprint doc separates MCP composition work from deferred backend or auth work

In-scope files:
- docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md
- docs/roadmap.md (read-only reference)
- apps/mcp/README.md (read-only reference)
- .cursor/rules/docs-sprint-planning.mdc (read-only reference)

Out of scope:
- MCP code changes
- backend endpoint changes
- frontend work
- auth modernization code

Instructions:
Implement only Task 1a. Tighten the Sprint 7 wording, scope, acceptance criteria, and non-goals so the sprint stays centered on agent-facing MCP workflows.
```

## Task 2a - Simulation Read-Tool Normalization

```text
Task ID: Task 2a
Owner: Codex
Mode: modify code

Goal (one sentence):
Normalize the Sprint 7 MCP workflow path so touched simulation read tools return consistent machine-readable JSON in text and structured payloads.

Acceptance criteria (copied from sprint doc):
- touched MCP read tools expose reliable JSON payloads in text and structured forms
- the implementation reduces response-shape drift across the Sprint 7 workflow path

In-scope files:
- apps/mcp/src/tools/simulation-tools.ts
- apps/mcp/src/contracts.ts
- apps/mcp/src/errors.ts
- apps/mcp/src/index.ts (only if registration or helper wiring requires it)
- apps/mcp/README.md (read-only reference)
- docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md (read-only reference)

Out of scope:
- new backend endpoints
- broad MCP rewrite outside the touched simulation workflow path
- frontend changes
- auth redesign

Instructions:
Implement only Task 2a. Normalize the Sprint 7-touched simulation read tools so their text output mirrors structured JSON closely enough for agent clients that only surface text.
```

## Task 2b - Event Explanation and Recent-Changes Summary Tools

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Add one event-explanation tool and one bounded city-changes summary tool that compose existing backend-owned history and snapshot reads into agent-friendly answers.

Acceptance criteria (copied from sprint doc):
- an agent can ask what a specific event means without manually decoding raw payload fields
- an agent can ask what changed recently in a city without manually joining multiple low-level tools

In-scope files:
- apps/mcp/src/tools/simulation-tools.ts
- apps/mcp/src/backend-client.ts
- apps/mcp/src/contracts.ts
- apps/mcp/README.md (read-only reference)
- docs/specs/simulation-read-model-spec.md (read-only reference)
- docs/specs/ai-history-enrichment-spec.md (read-only reference)
- docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md (read-only reference)

Out of scope:
- broad backend history-model redesign
- introducing a new LLM dependency in `apps/mcp`
- frontend integration
- full auth-flow changes

Instructions:
Implement only Task 2b. Prefer MCP-local composition over backend changes, keep summaries bounded by explicit tick inputs, and keep explanation output traceable to backend-owned event fields.
```

## Task 3a - README and Demo-Flow Documentation

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code (docs and narrow MCP examples only)

Goal (one sentence):
Document a polished Sprint 7 MCP demo flow so contributors can run snapshot, history, explanation, and recent-change summary steps without guessing.

Acceptance criteria (copied from sprint doc):
- the main Sprint 7 demo flow can be run without guessing tool order or inputs
- MCP validation results and remaining gaps are captured in the sprint doc

In-scope files:
- apps/mcp/README.md
- docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md
- docs/sprints/sprint07/sprint-07-prompt-pack.md

Out of scope:
- new backend implementation
- frontend docs rewrite
- broad MCP product copy editing unrelated to the touched flow

Instructions:
Implement only Task 3a. Add concrete usage examples and a clear recommended tool sequence for the Sprint 7 demo path, including `simulation_changes_summary` and `simulation_event_explain`.
```

## Task 4a - MCP Build, Smoke Validation, and Closeout

```text
Task ID: Task 4a
Owner: Codex
Mode: modify code (tests/docs/status sync allowed)

Goal (one sentence):
Run focused validation for the Sprint 7 MCP workflow path, record residual risks, and update the sprint execution status accurately.

Acceptance criteria (copied from sprint doc):
- the main agent demo flow is documented clearly enough to run from repo context alone
- MCP build and at least one focused smoke validation path have been recorded in the sprint status

In-scope files:
- docs/sprints/sprint07/sprint-07-mcp-agent-workflows.md
- apps/mcp/src/tools/simulation-tools.ts
- apps/mcp/src/backend-client.ts
- apps/mcp/README.md
- nearby touched files only if validation reveals a Sprint 7-scoped blocker

Out of scope:
- new feature expansion beyond Sprint 7
- broad cleanup unrelated to the touched MCP workflow
- auth modernization implementation

Instructions:
Implement only Task 4a. Run the nearest useful MCP validation for the touched tools, sync the sprint execution block, and capture deferred follow-on work plus any environment-specific blockers.
```
