---
name: spec-update
description: Legacy: former spec docs live under docs/archive/2025-03-docs-consolidation/specs. Use only when explicitly asked to edit archived spec text; contracts live in code and OpenAPI.
---

# Spec update (archived tree)

Former `docs/specs/` files are under **`docs/archive/2025-03-docs-consolidation/specs/`**. They are **historical narrative**, not authoritative over the codebase.

## Use when

- The user explicitly asks to align archived spec wording with history or links.

## Workflow

1. Edit only the sections the user requested.
2. For API shape and behavior, trust **code** and **generated OpenAPI clients/types**, not markdown.

## Guardrails

- If execution order changes, use **`docs/milestones.md`**, not long planning blocks in archived specs.
