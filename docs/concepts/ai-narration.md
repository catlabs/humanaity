# AI narration

## Scope
Read-only AI text enrichment for simulation events and related history.

## Covers
- narrative text built from backend-owned deterministic event and invention records
- explicit `ready`, `fallback`, and absent states for enriched text
- UI rendering that keeps canonical facts primary and narration supplemental
- the rule that AI does not parse commands or change simulation state in the primary product path

## Source docs
- `docs/specs/ai-history-enrichment-spec.md`
- `docs/specs/deterministic-command-contract-spec.md`
