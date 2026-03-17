# Sprint 4 Prompt Pack (Copy-Paste Templates)

Use one prompt per sub-chunk. Do not ask for "implement Sprint 4" in a single pass.

Before using a prompt, check `docs/sprints/sprint04/sprint-04-frontend-simulation-experience.md` and keep its `## Execution status` section current so the active chunk, next chunk, and blocked items stay visible in one place.

Each template includes:
- exact task ID
- one-sentence goal
- acceptance criteria copied from `docs/sprints/sprint04/sprint-04-frontend-simulation-experience.md`
- explicit in-scope files
- explicit out-of-scope items
- whether the tool should modify code or only propose a patch

## Codex input contract and Cursor re-integration

Use this contract for every Codex-targeted prompt:

- include repo-visible references in the prompt body:
  - `docs/sprints/sprint04/sprint-04-frontend-simulation-experience.md` for scope and acceptance criteria
  - `docs/roadmap.md` for epic alignment
  - `docs/specs/frontend-simulation-experience-spec.md` once Task 1a is complete
  - relevant `.cursor/rules/*.mdc` files when they contain mandatory policy for the chunk
- do not assume hidden Cursor skills are visible to Codex; restate mandatory constraints directly
- include a hard boundary line: "Implement only this task ID; do not expand to other sprint tasks"

Re-integrate Codex output before opening the next chunk:

- Cursor reviews scope fit, route ownership, and page-responsibility boundaries
- Cursor runs chunk-level frontend validation for the touched area
- if backend contract use changes, Cursor confirms generated-client usage still matches Sprint 3 surfaces
- Cursor compares the result to sprint acceptance criteria and definition of done
- Cursor updates sprint/spec docs if implementation changed sprint-shaping decisions

## Per-chunk review/test/handoff checklist

Use `.cursor/rules/docs-chunk-review-loop.mdc` as the standard checklist and go/no-go gate after every chunk implementation.

For Sprint 4, also verify:

- there is one authoritative city simulation page
- fake page-local simulation metadata is not reintroduced
- loading, empty, and error states are explicit on the real simulation path

When a chunk changes the primary simulation page path, include this handoff gate:

- confirm the intended route is the only primary city simulation surface
- run focused frontend type-check/build validation for the touched page
- validate one city load/control/render flow using the generated contracts

---

## Task 1a - Frontend Simulation Experience Spec

```text
Task ID: Task 1a
Owner: You
Mode: modify code (docs only)

Goal (one sentence):
Write and lock the frontend simulation experience spec so the authoritative city simulation page can be implemented without guessing.

Acceptance criteria (copied from sprint doc):
- a developer can implement the primary city simulation page without guessing which route/page should survive
- the spec clearly states which UI behaviors are required now versus deferred

In-scope files:
- docs/specs/frontend-simulation-experience-spec.md
- docs/sprints/sprint04/sprint-04-frontend-simulation-experience.md (read-only reference)
- docs/roadmap.md (read-only reference)

Out of scope:
- Angular implementation
- backend changes
- OpenAPI regeneration
- broad design-system work

Instructions:
Implement only Task 1a. Define the authoritative city simulation page, required controls/panels, contract-to-UI mapping, and explicit loading/empty/error behavior.
```

## Task 2a - Route and Page Consolidation

```text
Task ID: Task 2a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Consolidate duplicate city simulation routes/pages into one authoritative frontend surface.

Acceptance criteria (copied from sprint doc):
- one city route serves as the authoritative simulation experience
- the repo no longer has two competing city simulation pages for the same product responsibility

In-scope files:
- apps/ui/src/app/features/city/city.route.ts
- apps/ui/src/app/features/city/pages/details/
- apps/ui/src/app/features/city/pages/simulation-detail/
- docs/specs/frontend-simulation-experience-spec.md (read-only reference after Task 1a)

Out of scope:
- backend business logic changes
- MCP tooling
- full feature completion for controls/history panels
- unrelated route refactors

Instructions:
Implement only Task 2a. Decide which page survives, route city detail to that page, and retire or redirect the duplicate implementation cleanly.
```

## Task 2b - Real Controls and Pixi Integration

```text
Task ID: Task 2b
Owner: Codex
Mode: modify code

Goal (one sentence):
Wire real simulation controls, refresh behavior, and Pixi rendering into the authoritative city simulation page.

Acceptance criteria (copied from sprint doc):
- simulation controls update backend state and visible UI state coherently
- Pixi uses real backend human positions on the authoritative page

In-scope files:
- apps/ui/src/app/features/city/city.service.ts
- apps/ui/src/app/features/city/pages/details/ or apps/ui/src/app/features/city/pages/simulation-detail/ (whichever survives Task 2a)
- apps/ui/src/app/features/city/services/pixi-canvas.service.ts
- apps/ui/src/app/api/ (generated client usage only)

Out of scope:
- backend contract changes
- MCP updates
- broad visual redesign
- AI/narrative UI work

Instructions:
Implement only Task 2b. Use existing Sprint 3 contracts, make controls real, and keep the Pixi integration anchored to the authoritative page lifecycle.
```

## Task 3a - Real History Panel Integration

```text
Task ID: Task 3a
Owner: Cursor
Mode: modify code

Goal (one sentence):
Replace fake event, invention, and timeline summary content with real backend history data.

Acceptance criteria (copied from sprint doc):
- history panels show real backend data for Sprint 4-covered surfaces
- panel state is explicit when history data is absent or loading

In-scope files:
- apps/ui/src/app/features/city/city.service.ts
- authoritative city simulation page files selected in Task 2a
- apps/ui/src/app/shared/components/event-item/
- apps/ui/src/app/shared/components/timeline-node/
- apps/ui/src/app/api/ (generated client usage only)

Out of scope:
- backend history rule changes
- MCP tooling
- full chronology redesign
- unrelated shared-component cleanup

Instructions:
Implement only Task 3a. Drive event/invention/timeline summary UI from backend history data and remove remaining fake panel content for Sprint 4 surfaces.
```

## Task 3b - Loading, Error, and Selection Polish

```text
Task ID: Task 3b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Add loading, empty, error, and selection-state polish to the real simulation page without redesigning it.

Acceptance criteria (copied from sprint doc):
- history panels show real backend data for Sprint 4-covered surfaces
- panel state is explicit when history data is absent or loading

In-scope files:
- authoritative city simulation page files selected in Task 2a
- apps/ui/src/app/features/city/city.service.ts
- apps/ui/src/app/shared/components/

Out of scope:
- large design-system changes
- backend changes
- unrelated global styling work
- admin flow cleanup

Instructions:
Implement only Task 3b. Tighten the real simulation page behavior around async state and selection handling while preserving the existing visual language.
```

## Task 4a - Frontend Validation Coverage

```text
Task ID: Task 4a
Owner: Codex
Mode: modify code

Goal (one sentence):
Add focused frontend validation for the consolidated simulation page, especially route/service/page behavior.

Acceptance criteria (copied from sprint doc):
- regressions in the main simulation page are easier to catch
- the Sprint 4 experience can be demonstrated without relying on mocked data

In-scope files:
- apps/ui/src/app/features/city/
- existing frontend test files or adjacent test targets when available
- docs/specs/frontend-simulation-experience-spec.md (read-only reference after Task 1a)

Out of scope:
- backend tests
- MCP tool tests
- broad frontend test-suite rescue outside the touched path

Instructions:
Implement only Task 4a. Add focused validation for the authoritative simulation route/page and its real data wiring, not a broad frontend testing initiative.
```

## Task 4b - Validation Flow and Sprint Closeout

```text
Task ID: Task 4b
Owner: Cursor
Mode: modify code

Goal (one sentence):
Validate one real UI flow on the consolidated simulation page and record Sprint 4 execution status.

Acceptance criteria (copied from sprint doc):
- regressions in the main simulation page are easier to catch
- the Sprint 4 experience can be demonstrated without relying on mocked data

In-scope files:
- docs/sprints/sprint04/sprint-04-frontend-simulation-experience.md (execution-status update only)
- authoritative city simulation page files only if tiny validation fixes are required

Out of scope:
- backend feature work
- large UI redesign
- unrelated cleanup outside validation fixes

Instructions:
Implement only Task 4b. Run one city load/control/render/history validation flow, record the result in the sprint doc, and keep any code changes limited to small fixes discovered during validation.
```
