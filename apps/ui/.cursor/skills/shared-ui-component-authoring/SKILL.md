---
name: shared-ui-component-authoring
description: Build reusable shared UI components with strict typing and project styling conventions. Use when creating components in src/app/shared/ui.
---

# Shared UI Component Authoring

## Goal

Create reusable UI components in `src/app/shared/ui/` with stable APIs.

## Workflow

1. Confirm the component is cross-feature reusable.
2. Create `src/app/shared/ui/<component>/`.
3. Build as a standalone component with typed `input()`/`output()`.
4. Keep component API minimal and explicit.
5. Style with Material token + SCSS mixin conventions.
6. Export via shared public API barrel as needed.

## Guardrails

- Do not move feature-specific logic into shared UI.
- Avoid leaking domain-specific models if generic contracts can be used.
- Prefer composition and small focused variants over one giant configurable component.
