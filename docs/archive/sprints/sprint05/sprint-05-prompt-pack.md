# Sprint 5 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 5" in a single pass.

Before using a prompt, check `docs/sprints/sprint05/sprint-05-ai-history-enrichment.md` and keep its `## Execution status` section current so the active chunk, next chunk, and blocked items stay visible in one place.

Each template includes:

- exact task ID
- one-sentence goal
- acceptance criteria copied from `docs/sprints/sprint05/sprint-05-ai-history-enrichment.md`
- explicit in-scope files
- explicit out-of-scope items
- whether the tool should modify code or only propose a patch

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references in the prompt body:
  - `docs/sprints/sprint05/sprint-05-ai-history-enrichment.md` for scope and acceptance criteria
  - `docs/roadmap.md` for epic alignment
  - `docs/specs/ai-history-enrichment-spec.md` once Task 1a is complete
  - relevant `.cursor/rules/*.mdc` files when they contain mandatory policy for the chunk
- do not assume hidden Cursor skills are visible to Codex; restate mandatory constraints directly
- include a hard boundary line: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- Cursor reviews canonical-versus-enrichment boundaries and fallback semantics
- Cursor runs chunk-level validation for the touched backend/MCP/UI path
- if contract fields changed, Cursor confirms generated clients and wrappers are synchronized
- Cursor compares the result to sprint acceptance criteria and definition of done
- Cursor updates sprint/spec docs if implementation changed sprint-shaping decisions

## Per-chunk review/test/handoff checklist

Use `.cursor/rules/docs-chunk-review-loop.mdc` as the standard checklist and go/no-go gate after every chunk implementation.

For Sprint 5, also verify:

- deterministic fields remain canonical
- enrichment fields remain explicitly non-authoritative
- fallback behavior is visible and testable
- consumers do not synthesize AI text locally

---

## Task 1a - AI History Enrichment Spec

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Write and lock the AI history enrichment spec so invention and dialogue enrichment can be implemented without guessing field ownership or fallback semantics.

Acceptance criteria (copied from sprint doc):
- a developer can implement invention and event enrichment without guessing canonical versus AI-owned fields
- the spec clearly states fallback and source-linkage semantics

In-scope files:
- docs/specs/ai-history-enrichment-spec.md
- docs/sprints/sprint05/sprint-05-ai-history-enrichment.md (read-only reference)
- docs/roadmap.md (read-only reference)

Out of scope:
- backend implementation
- UI changes
- OpenAPI regeneration
- recap generation

Instructions:
Implement only Task 1a. Define MVP enrichment targets, canonical versus AI-owned fields, fallback/no-enrichment-yet semantics, and source-linkage rules.
```

## Task 2a - Backend Enrichment Storage and DTO Surface

```text
Task ID: Task 2a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add backend-owned storage and DTO/API surface for invention and dialogue-capable event enrichment.

Acceptance criteria (copied from sprint doc):
- backend can store and return enrichment for the Sprint 5 targets
- enrichment failure does not block canonical history persistence or reads

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/ai/
- apps/backend/src/main/java/eu/catlabs/humanaity/event/
- apps/backend/src/main/java/eu/catlabs/humanaity/invention/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/
- docs/specs/ai-history-enrichment-spec.md (read-only reference after Task 1a)

Out of scope:
- provider prompt tuning
- MCP updates
- frontend rendering work
- recap generation

Instructions:
Implement only Task 2a. Add minimal enrichment persistence and backend DTO surface for invention and dialogue-capable event enrichment while preserving canonical deterministic fields.
```

## Task 2b - Prompting, Validation, and Fallback Generation

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Implement prompt-building, provider orchestration, validation, and fallback-safe enrichment generation for Sprint 5 targets.

Acceptance criteria (copied from sprint doc):
- provider output is validated before persistence or exposure
- API and MCP consumers can read enrichment from one backend-owned contract path

In-scope files:
- apps/backend/src/main/java/eu/catlabs/humanaity/ai/application/
- apps/backend/src/main/java/eu/catlabs/humanaity/ai/application/prompt/
- apps/backend/src/main/java/eu/catlabs/humanaity/event/
- apps/backend/src/main/java/eu/catlabs/humanaity/invention/
- apps/backend/src/main/java/eu/catlabs/humanaity/simulation/application/
- docs/specs/ai-history-enrichment-spec.md (read-only reference after Task 1a)

Out of scope:
- frontend changes
- MCP wrapper changes
- broad provider abstraction redesign
- recap generation

Instructions:
Implement only Task 2b. Generate enrichment for inventions and dialogue-capable events with strict structured validation and explicit fallback behavior that never blocks canonical history flow.
```

## Task 3a - Contract Regeneration and MCP Alignment

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Regenerate API/MCP contracts for enrichment fields and align MCP wrappers with the backend-owned enrichment surface.

Acceptance criteria (copied from sprint doc):
- provider output is validated before persistence or exposure
- API and MCP consumers can read enrichment from one backend-owned contract path

In-scope files:
- apps/ui/src/app/api/
- apps/mcp/src/generated/
- apps/mcp/src/backend-client.ts
- apps/mcp/src/tools/simulation-tools.ts

Out of scope:
- backend canonical history redesign
- broad UI changes
- recap generation

Instructions:
Implement only Task 3a. Regenerate contract artifacts, update MCP wrappers to the enrichment-enabled backend responses, and remove local enrichment synthesis if any appears.
```

## Task 3b - Thin Frontend Enrichment Adoption

```text
Task ID: Task 3b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Display backend-owned enrichment text on one real simulation detail path without redesigning the page.

Acceptance criteria (copied from sprint doc):
- one real frontend path can display backend-owned enrichment text
- regressions in fallback and source-linkage semantics are easy to detect

In-scope files:
- apps/ui/src/app/features/city/city.service.ts
- apps/ui/src/app/features/city/pages/simulation-detail/
- apps/ui/src/app/api/ (generated client usage only)

Out of scope:
- broad simulation page redesign
- fake fallback text generated in the UI
- recap surfaces

Instructions:
Implement only Task 3b. Render backend-owned enrichment text for Sprint 5-covered invention/event surfaces and keep empty/fallback states explicit.
```

## Task 4a - Fallback and Traceability Tests

```text
Task ID: Task 4a
Owner: Codex
Mode: modify code

Goal (one sentence):
Add focused backend/API tests for enrichment fallback, source linkage, and non-authoritative field behavior.

Acceptance criteria (copied from sprint doc):
- one real frontend path can display backend-owned enrichment text
- regressions in fallback and source-linkage semantics are easy to detect

In-scope files:
- apps/backend/src/test/java/eu/catlabs/humanaity/
- docs/specs/ai-history-enrichment-spec.md (read-only reference after Task 1a)

Out of scope:
- broad test-suite rescue
- UI tests beyond a touched-path need
- recap generation tests

Instructions:
Implement only Task 4a. Add focused tests proving that canonical fields remain stable, fallback output is visible, and enrichment stays traceable to its deterministic source.
```

## Task 4b - Enrichment Smoke and Sprint Closeout

```text
Task ID: Task 4b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Validate one real enrichment flow through backend plus MCP/UI touchpoints and record Sprint 5 execution status.

Acceptance criteria (copied from sprint doc):
- one real frontend path can display backend-owned enrichment text
- regressions in fallback and source-linkage semantics are easy to detect

In-scope files:
- docs/sprints/sprint05/sprint-05-ai-history-enrichment.md (execution-status update only)
- touched MCP/UI files only if tiny validation fixes are required

Out of scope:
- major backend redesign
- recap generation
- unrelated cleanup

Instructions:
Implement only Task 4b. Run one enrichment-enabled validation flow, confirm backend-owned enrichment reaches at least one consumer path, and record the result in the sprint doc.
```
