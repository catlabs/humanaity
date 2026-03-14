# Sprint 9 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 9" in a single pass.

Before using a prompt, check `docs/sprints/sprint09/sprint-09-agentic-simulation-console.md` and keep its `## Execution status` section current.

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references:
  - `docs/sprints/sprint09/sprint-09-agentic-simulation-console.md`
  - `docs/specs/agent-chat-orchestration-spec.md`
  - `docs/roadmap.md`
  - relevant `.cursor/rules/*.mdc`
- include the hard boundary: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- confirm the page still reads as one flagship simulation surface
- validate that Sprint 8 chat flows still work
- update sprint docs if contract or scope assumptions changed

## Per-chunk review/test/handoff checklist

For Sprint 9, also verify:

- chat is the dominant interaction surface
- map remains visually central
- supporting panels are lighter, not more complex
- no new command classes slipped into this sprint

---

## Task 1a - Console-First Boundary Lock

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Lock Sprint 9 to console-first UI consolidation rather than command expansion or full redesign.

Acceptance criteria (copied from sprint doc):
- a contributor can tell what "console-first" means in this repo
- Sprint 9 clearly excludes new command-class expansion

In-scope files:
- docs/sprints/sprint09/sprint-09-agentic-simulation-console.md
- docs/specs/agent-chat-orchestration-spec.md (read-only reference)
- docs/roadmap.md (read-only reference)

Out of scope:
- backend feature expansion
- guided or director commands
- design-system rewrite

Instructions:
Implement only Task 1a. Tighten the console-first scope and non-goals so Sprint 9 stays product-facing and coherent.
```

## Task 2a - Simulation Console Layout Consolidation

```text
Task ID: Task 2a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Refactor the authoritative simulation page into a clearer map + chat + lightweight context console.

Acceptance criteria (copied from sprint doc):
- the page reads as one product surface
- the map and chat surfaces are visually primary

In-scope files:
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/shared/components/
- docs/specs/agent-chat-orchestration-spec.md (read-only reference)

Out of scope:
- new command classes
- backend orchestration redesign
- unrelated UI surfaces

Instructions:
Implement only Task 2a. Consolidate the page hierarchy without turning the chunk into a global redesign.
```

## Task 2b - Effect Stabilization

```text
Task ID: Task 2b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Turn backend refresh/focus/highlight effects into one stable frontend handling path.

Acceptance criteria (copied from sprint doc):
- backend effects feel intentional and visible in the UI
- supporting panels remain useful without dominating the page

In-scope files:
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/features/city/services/

Out of scope:
- guided follow behaviors
- backend contract expansion unless narrowly required
- whole-page redesign beyond the Sprint 9 shell

Instructions:
Implement only Task 2b. Centralize the touched effect behavior so the console responds consistently to orchestration replies.
```

## Task 3a - Supporting-Panel Simplification

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Simplify history and context panels so they support the console flow instead of competing with chat and map.

Acceptance criteria (copied from sprint doc):
- backend effects feel intentional and visible in the UI
- supporting panels remain useful without dominating the page

In-scope files:
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/shared/components/

Out of scope:
- new backend features
- guided/director workflows
- broad navigation changes

Instructions:
Implement only Task 3a. Reduce clutter and keep the page aligned with the map + chat product story.
```

## Task 4a - Validation and Sprint Closeout

```text
Task ID: Task 4a
Owner: Codex and Cursor
Mode: modify code (tests/docs/status sync allowed)

Goal (one sentence):
Validate the consolidated console flow and close Sprint 9 with accurate residual-risk notes.

Acceptance criteria (copied from sprint doc):
- Sprint 8 chat flows still work on the new page
- residual console/layout risks are documented clearly

In-scope files:
- docs/sprints/sprint09/sprint-09-agentic-simulation-console.md
- touched UI files from Tasks 2a through 3a

Out of scope:
- new command development
- backend orchestration redesign
- unrelated UI cleanup

Instructions:
Implement only Task 4a. Run the nearest useful UI validation for the console flow and sync the sprint doc with the real result.
```
