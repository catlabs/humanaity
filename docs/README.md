# Docs structure

The `docs/` folder is organized around **system concepts** and kept intentionally lightweight.

## Folders and files

| Path | Purpose |
|---|---|
| `concepts/` | Primary docs grouped by core system blocks (`simulation-engine`, `commands`, `human-goals`, etc.) |
| `specs/` | Locked/technical reference specs with deeper invariants and contracts |
| `roadmap.md` | Short docs-navigation roadmap and maintenance rule |
| `archive/` | Historical sprint/legacy roadmap documents kept for traceability |
| `agent-context.md` | How docs/rules/context are split for AI-assisted execution |

## Working approach

1. Start in `docs/concepts/`.
2. Use `docs/specs/` only when exact rules or schemas are needed.
3. Treat `docs/archive/` as read-only history.

## Design principles

- short, human-readable filenames
- easy to scan
- low maintenance overhead
- no sprint-by-sprint dependency for current understanding
