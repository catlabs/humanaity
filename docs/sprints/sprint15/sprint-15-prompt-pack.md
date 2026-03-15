# Sprint 15 Prompt Pack

Use one prompt per task. Do not merge tasks. Each prompt below is bounded to one Sprint 15 task only.

References for every task:

- `docs/roadmap.md`
- `docs/specs/main-simulation-board-spec.md`
- `docs/sprints/sprint15/sprint-15-main-simulation-board-simplification.md`
- `.cursor/rules/docs-sprint-planning.mdc`

Implementation boundary for Sprint 15:

- keep the main simulation page focused on one symbolic board
- use real backend snapshot data for human markers
- do not keep PixiJS in parallel on the authoritative page
- remove authoritative PixiJS references that would misstate ownership of the main page
- do not add new product features beyond the simplification scope

## Task 1a Prompt

Implement only `Task 1a`; do not expand to other sprint tasks.

Goal:

- lock the authoritative-page boundary so the main city simulation route has one symbolic board surface only

Acceptance criteria:

- the implementation or planning notes make it explicit that PixiJS is out of the authoritative main page for Sprint 15
- the main-page ownership rule is unambiguous for follow-up tasks
- authoritative PixiJS references are removed or updated so they no longer mislead execution

In scope:

- authoritative page ownership
- explicit non-goals for PixiJS on the main route
- small doc updates if needed for boundary clarity

Out of scope:

- building the board component
- layout implementation
- backend changes

## Task 2a Prompt

Implement only `Task 2a`; do not expand to other sprint tasks.

Goal:

- audit whether current backend snapshot coordinates are sufficient for the simplified board

Acceptance criteria:

- coordinate ownership is explicit
- deterministic frontend clamping/fallback rules are documented only if needed
- any real API gap is identified narrowly

In scope:

- snapshot contract audit
- coordinate mapping review
- spec/doc updates if necessary

Out of scope:

- page layout work
- board component work
- speculative backend redesign

## Task 3a Prompt

Implement only `Task 3a`; do not expand to other sprint tasks.

Goal:

- build or consolidate one lightweight board component that renders humans from real backend snapshot data

Acceptance criteria:

- humans are visible as simple positioned markers
- movement is animated through CSS transitions
- fixed symbolic places improve readability without changing simulation meaning

In scope:

- `apps/ui/src/app/features/city/components/`
- `apps/ui/src/app/features/city/services/`
- minimal template wiring needed for the board surface

Out of scope:

- broad simulation page redesign beyond what the board needs
- new overlays or chat effects
- backend changes unless already approved through `Task 2a`

## Task 3b Prompt

Implement only `Task 3b`; do not expand to other sprint tasks.

Goal:

- simplify the main simulation page so the board is clearly the primary visible element

Acceptance criteria:

- secondary panels no longer compete with the board
- only minimal controls and compact status/debug information remain visible
- the page is cleaner than before

In scope:

- `apps/ui/src/app/features/city/pages/simulation-detail/`
- small supporting component/template/style adjustments

Out of scope:

- redesigning unrelated routes
- adding new product capabilities
- reintroducing Pixi or a second main rendering surface

## Task 4a Prompt

Implement only `Task 4a`; do not expand to other sprint tasks.

Goal:

- validate that the simplified board visibly reacts to real backend state changes

Acceptance criteria:

- refresh and deterministic step actions are checked against visible marker movement
- residual risks or coordinate caveats are documented explicitly

In scope:

- focused validation
- build/test/type-check notes
- closeout doc updates for Sprint 15

Out of scope:

- new implementation work beyond minor fixes required for validation
- feature expansion or layout polish outside the definition of done
