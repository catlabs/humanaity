# AI History Enrichment Spec (Sprint 5)

## Status

- Status: LOCKED for Sprint 5 planning
- Scope anchor: `docs/sprints/sprint05/sprint-05-ai-history-enrichment.md`
- Applies to: backend-owned AI enrichment for persisted history artifacts

## Purpose

This document fixes the field-ownership and fallback semantics for Sprint 5 AI history enrichment.

If implementation choices conflict with this spec during Sprint 5, this spec is the source of truth.

## Primary Product Goal

Sprint 5 adds optional AI-authored narrative text to deterministic history artifacts so users can read more interpretable invention and dialogue content without weakening reproducibility.

## Authoritative Data Rule

Deterministic history facts remain canonical.

Canonical facts include:

- IDs and keys
- city, tick, year, and era
- event category and event type
- actor IDs and deterministic payload
- invention category and source event keys

AI enrichment is supplemental and non-authoritative.

Consumers must remain able to function correctly when enrichment is absent or fallback-generated.

## Sprint 5 Enrichment Targets

Sprint 5 covers only:

- inventions
- dialogue-capable events

For Sprint 5, "dialogue-capable events" means event records whose deterministic type/category makes a dialogue snippet meaningful, especially `DIALOGUE_EXCHANGED`.

## Contract Shape Rule

Backend DTOs should expose enrichment in dedicated enrichment fields or sub-objects rather than overwriting canonical text fields.

The contract must let a consumer distinguish:

- no enrichment available yet
- AI-authored enrichment ready
- fallback enrichment returned instead of AI output

## Required Invention Enrichment Semantics

Invention enrichment must be able to expose:

- a display-ready title variant
- a display-ready summary variant
- enrichment status
- fallback indicator
- provider/model metadata only if already available cheaply

Rules:

- canonical invention identity and source linkage stay unchanged
- AI title/summary do not replace canonical deterministic invention fields
- if AI output is missing or invalid, backend returns fallback-safe enrichment fields or an explicit non-ready state

## Required Event Enrichment Semantics

Event enrichment for dialogue-capable events must be able to expose:

- a short dialogue-oriented snippet or narrative line
- enrichment status
- fallback indicator
- source linkage to the canonical event

Rules:

- event type, payload, actors, and ordering remain canonical
- enrichment must not invent new canonical actors, years, or event categories
- non-dialogue event types may expose no enrichment without being considered an error

## Status Semantics

Sprint 5 should support stable consumer-visible status semantics equivalent to:

- no enrichment yet
- ready
- fallback

Exact enum names may differ in code, but consumers must be able to distinguish these states without guessing.

## Fallback Rules

Fallback behavior is mandatory.

If AI generation:

- fails
- times out
- returns invalid structure
- is unavailable by configuration

then:

- deterministic simulation/history persistence still succeeds
- backend returns either fallback content or an explicit non-ready status, depending on the chosen contract path
- consumers must not need to synthesize their own fallback narrative text

If fallback content is returned, clients must be able to tell it is fallback-derived.

## Source-Linkage Rules

Each enrichment record must remain traceable to one canonical source artifact.

Minimum linkage rules:

- invention enrichment links to one invention record
- event enrichment links to one event record
- canonical source IDs/keys remain queryable independently of enrichment text

Consumers must never need to parse AI text to discover canonical identifiers.

## Generation Timing Rule

Sprint 5 generation should occur in a backend-owned path after canonical deterministic data exists.

Rules:

- canonical simulation/event/invention persistence must not depend on successful AI generation
- enrichment generation must not alter deterministic outcomes
- if enrichment is triggered inline for MVP simplicity, failure must still degrade safely

## Consumer Rules

### MCP

MCP tools must expose backend-owned enrichment fields from regenerated contracts rather than synthesizing narrative text locally.

### UI

UI surfaces may render enrichment when available, but they must:

- preserve deterministic source facts as the primary frame of reference
- show explicit empty/fallback behavior
- avoid creating page-local fake narrative text

## Empty-State Rules

### No enrichment requested or generated yet

The contract must remain valid and explicit.

Consumers should be able to render:

- canonical invention/event data
- no enrichment yet state

without guessing whether data is missing due to an error or simply not generated.

### Event not eligible for enrichment

Non-dialogue or otherwise non-targeted events may return no enrichment without violating the contract.

This should be represented explicitly or through documented omission semantics.

## Deferred for Later Sprints

Sprint 5 explicitly defers:

- periodic historical recap generation
- prompt-versioning and regeneration workflows
- multi-paragraph narrative cards
- user-editable enrichment
- AI-authored canonical fact correction
- advanced UI storytelling layouts

## Implementation Guardrails

- keep deterministic source data and AI output separated
- prefer dedicated output fields over overloaded canonical fields
- keep the MVP contract small and consumer-readable
- do not force UI or MCP to infer enrichment status from missing strings alone
- do not let AI enrichment become a prerequisite for simulation progress

## Done Signals for Sprint 5

Sprint 5 implementation is aligned with this spec only if:

- canonical versus AI-owned fields are clearly separated
- invention and dialogue-capable event enrichment can be queried from backend-owned contracts
- fallback behavior is explicit and testable
- consumers can tell ready versus fallback without guessing
- deterministic history remains usable when enrichment is absent
