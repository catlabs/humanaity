# Sprint 5: AI History Enrichment

## Execution status

- Current phase: Sprint 5 completed
- Active chunk: none
- Next chunk: none
- Blocked items: none
- Last completed chunk: `Task 4b` - enrichment smoke and sprint closeout (2026-03-13)

| Chunk ID | Status | Notes |
| --- | --- | --- |
| Task 1a | completed | Spec locked in `docs/specs/ai-history-enrichment-spec.md` with canonical vs AI-owned field rules and fallback semantics. |
| Task 2a | completed | Added enrichment storage fields on history entities and exposed them through simulation DTO contracts. |
| Task 2b | completed | Implemented prompt-building, structured JSON validation, and explicit fallback behavior in backend enrichment service. |
| Task 3a | completed | Regenerated UI/MCP contracts and aligned wrappers with backend-owned enrichment fields. |
| Task 3b | completed | Simulation detail page now renders backend enrichment fields for inventions/events without local fake narrative synthesis. |
| Task 4a | completed | Added focused enrichment tests plus history contract assertions for enrichment status/fallback fields. |
| Task 4b | completed | MCP smoke validated enrichment-enabled timeline/snapshot flow on fresh in-memory backend schema (city `1`, 353 events, 6 inventions). |

## Sprint intent

Sprint 5 exists to make HUMANAIty's deterministic history legible and distinctive by adding AI-authored narrative text on top of persisted deterministic facts.

This sprint is backend-first, with thin consumer adoption.

It does **not** try to turn the simulation into an AI-first system. Its job is to enrich deterministic inventions and dialogue-capable history with optional narrative text that remains traceable and safe to ignore.

## Why this sprint comes next

Sprint 4 made the simulation UI real, but the product is still mostly factual and mechanical.

If Sprint 5 is skipped:

- the simulation remains technically credible but less differentiated
- invention and dialogue surfaces stay terse and harder to interpret
- AI integration remains disconnected from the real history model
- future recap features will be forced to invent semantics late instead of building on a clean enrichment contract

## Sprint outcome

At the end of Sprint 5, HUMANAIty should persist and expose backend-owned AI enrichment for inventions and dialogue-oriented events, with deterministic source linkage, explicit fallback behavior, and thin MCP/UI adoption on top of the canonical history model.

## Sprint scope

### In scope

- define the MVP AI-enrichment contract for inventions and dialogue-capable events
- keep deterministic history facts canonical and AI text explicitly non-authoritative
- add backend persistence and DTO/API surface for enrichment metadata and generated text
- implement prompt-building and provider orchestration for invention summaries and dialogue snippets
- add deterministic fallback behavior when AI generation is unavailable or invalid
- regenerate API/MCP contracts once the backend surface is stable
- add thin MCP/UI adoption so one real simulation path can display enrichment
- add focused tests and one smoke flow for traceability and fallback semantics

### Out of scope

- periodic civilization recap generation
- broad prompt-management framework redesign
- streaming or background job infrastructure
- AI-written canonical facts or simulation-rule changes
- major simulation page redesign
- provider benchmarking or model-selection optimization beyond one stable MVP path

## Product and technical decisions for this sprint

### Global decision: deterministic facts stay canonical

AI output enriches deterministic inventions and events, but it must never replace canonical history fields.

This means:

- deterministic IDs, ticks, years, eras, actors, and source event keys remain the source of truth
- AI text is optional, supplemental, and safe to ignore
- enrichment failure must not block simulation progression or history reads

### Decision 1: enrich only persisted history artifacts

Sprint 5 should enrich artifacts that already exist in deterministic history:

- inventions
- dialogue-capable events

Do not enrich transient runtime state or invent new non-persisted story objects in this sprint.

### Decision 2: keep enrichment storage explicit

Backend code should store enrichment data separately from canonical deterministic fields, even if read models later flatten the result for convenience.

Avoid overwriting canonical invention or event text fields with AI output.

### Decision 3: fallback text is part of the contract

If AI generation fails, times out, or returns invalid structure:

- backend still returns a usable enrichment surface
- clients can distinguish real AI output from fallback output
- deterministic simulation behavior remains unaffected

### Decision 4: recap generation is deferred

Periodic recap generation is valuable, but it should not share the first implementation sprint with invention and dialogue enrichment.

Sprint 5 should establish the enrichment contract and source-linkage rules first.

### Decision 5: consumers adopt enrichment thinly

MCP and UI should display new enrichment fields where useful, but Sprint 5 should avoid large presentation redesign.

## Deliverables

By the end of Sprint 5, the repo should contain:

- a written AI history enrichment spec
- backend DTOs/entities/services for invention and event enrichment
- prompt builders and provider orchestration for MVP enrichment flows
- regenerated API/MCP contracts for enrichment fields
- thin MCP/UI adoption of enrichment text on real product paths
- automated tests and one smoke flow proving fallback and traceability semantics

## Definition of done

Sprint 5 is done only if all of the following are true:

- invention and dialogue-capable event enrichment semantics are documented and unambiguous
- AI text is exposed without replacing canonical deterministic fields
- enrichment generation failure does not block simulation or history reads
- backend returns enough metadata for consumers to tell `ready` from `fallback`
- `apps/mcp` can exercise at least one enrichment-enabled path using regenerated contracts
- one real frontend path can render enrichment text without introducing local fake data
- focused tests cover fallback and source-linkage behavior

## Suggested file targets

These are the most likely files or folders Sprint 5 will touch:

- `apps/backend/src/main/java/eu/catlabs/humanaity/ai/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/event/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/invention/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/`
- `apps/backend/src/test/java/eu/catlabs/humanaity/`
- `apps/mcp/src/backend-client.ts`
- `apps/mcp/src/tools/simulation-tools.ts`
- `apps/ui/src/app/features/city/pages/simulation-detail/`
- `docs/specs/ai-history-enrichment-spec.md`

Likely new code areas:

- `apps/backend/src/main/java/eu/catlabs/humanaity/ai/application/prompt/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/ai/application/enrichment/`
- `apps/backend/src/main/java/eu/catlabs/humanaity/simulation/api/dto/*EnrichmentOutput.java`

## Features and task breakdown

## Feature 1: AI history enrichment specification

### Goal

Define which fields stay canonical, which fields are AI-authored, and how fallback/traceability behave so implementation does not drift.

### Tasks

1. Define the MVP enrichment targets for Sprint 5.
2. Define event and invention enrichment field semantics.
3. Define fallback and no-enrichment-yet behavior.
4. Define source-linkage and non-authoritative rules.
5. Define what is deferred to later AI work.

Locked Sprint 5 spec artifact:

- `docs/specs/ai-history-enrichment-spec.md`

### Acceptance criteria

- a developer can implement invention and event enrichment without guessing canonical versus AI-owned fields
- the spec clearly states fallback and source-linkage semantics

### Best owner

- You

## Feature 2: Backend enrichment generation and storage

### Goal

Persist and generate backend-owned enrichment for inventions and dialogue-capable events without contaminating deterministic history.

### Tasks

1. Add enrichment DTO/entity/storage shape for inventions and events.
2. Define where enrichment generation is triggered in application flow.
3. Implement fallback-safe persistence behavior.
4. Keep naming aligned with existing DTO and backend conventions.

### Acceptance criteria

- backend can store and return enrichment for the Sprint 5 targets
- enrichment failure does not block canonical history persistence or reads

### Best owner

- Cursor chat

## Feature 3: Prompting, validation, and contract exposure

### Goal

Make the enrichment pipeline reliable enough that generated text is structured, traceable, and consumable through API and MCP surfaces.

### Tasks

1. Add prompt builders for invention summary and dialogue snippet generation.
2. Validate provider output against strict structured expectations.
3. Expose enrichment through backend DTOs and regenerated contracts.
4. Update MCP wrappers to consume backend-owned enrichment rather than synthesizing text locally.

### Acceptance criteria

- provider output is validated before persistence or exposure
- API and MCP consumers can read enrichment from one backend-owned contract path

### Best owner

- Codex

## Feature 4: Thin consumer adoption and validation

### Goal

Show the new enrichment on one real UI path and one MCP flow, then prove fallback and linkage behavior with tests.

### Tasks

1. Render enrichment text on the simulation detail page without redesigning it.
2. Validate one MCP path that returns enriched invention or dialogue content.
3. Add focused tests for fallback, source linkage, and non-authoritative semantics.
4. Record Sprint 5 execution status after one real validation pass.

### Acceptance criteria

- one real frontend path can display backend-owned enrichment text
- regressions in fallback and source-linkage semantics are easy to detect

### Best owner

- Cursor chat and Codex

## Recommended implementation order

1. Lock the AI history enrichment spec.
2. Add backend enrichment storage and DTO semantics.
3. Implement prompt-building, validation, and fallback-safe generation.
4. Expose enrichment through API and regenerate MCP/UI contracts.
5. Add thin MCP/UI adoption.
6. Add focused tests and one smoke validation flow.

## Dependencies inside the sprint

- Feature 1 blocks all implementation tasks.
- Feature 2 must stabilize before contract regeneration.
- Feature 3 depends on Feature 2 storage/DTO decisions.
- Feature 4 depends on Feature 3 contract stability.

## Suggested delegation

- You: lock semantic decisions in the spec and review fallback/non-authoritative boundaries
- Cursor chat: backend wiring, DTOs, consumer cleanup, and sprint-doc execution tracking
- Codex: prompt-generation pipeline, fallback logic, and focused automated tests

## Ready-to-delegate task list

- `Task 1a`: write and lock the AI history enrichment spec
- `Task 2a`: add backend enrichment DTO/entity surface for inventions and dialogue-capable events
- `Task 2b`: implement prompt-building, provider orchestration, and fallback-safe enrichment generation
- `Task 3a`: regenerate API/MCP contracts and expose backend-owned enrichment fields
- `Task 3b`: adopt enrichment text on one real frontend simulation path
- `Task 4a`: add focused backend/API tests for fallback and source-linkage semantics
- `Task 4b`: run one enrichment smoke flow and record sprint execution status

## Risks

- enrichment can accidentally leak into canonical deterministic fields if DTO boundaries stay vague
- provider output may be structurally inconsistent without strict validation
- UI/MCP consumers may start depending on enrichment being always present unless empty/fallback states are explicit
- recap generation pressure may expand sprint scope unless kept deferred

## Handoff to next sprint

If Sprint 5 lands cleanly, the next sprint can build on it with:

- periodic historical recap generation
- richer UI presentation of narrative artifacts
- prompt-versioning or regeneration workflows
- broader AI-enriched MCP exploration tools
