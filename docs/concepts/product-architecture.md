# Product architecture

## Scope
Primary system framing for the HUMANAIty demo.

## Core rule
HUMANAIty is a deterministic simulation system with an AI narration layer.

- backend logic owns state, events, and commands
- Angular owns visualization and interaction surfaces
- AI may enrich text, but it never chooses actions or mutates state

## Covers
- deterministic simulation engine as the backend source of truth
- board-first Angular UI for simulation state, event context, and commands
- read-only AI narration attached to deterministic history
- the primary demo loop: `User -> Command -> Simulation -> Events -> Narration -> UI`

## Source docs
- `docs/concepts/simulation-engine.md`
- `docs/concepts/commands.md`
- `docs/concepts/ui-simulation.md`
- `docs/concepts/ai-narration.md`
