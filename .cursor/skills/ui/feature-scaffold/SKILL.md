---
name: feature-scaffold
description: Scaffold Angular feature folders, routes, wrappers, and exports using this repository architecture. Use when creating a new feature under src/app/features.
---

# Feature Scaffold

## Goal

Create a new feature that follows the project layout and modern Angular patterns.

## Workflow

1. Create `src/app/features/<feature>/` with:
   - `pages/`
   - `components/`
   - `services/`
   - `<feature>.routes.ts`
   - `index.ts`
2. Add standalone page components under `pages/`.
3. Add lazy routes in `<feature>.routes.ts` using `loadComponent`.
4. Create feature service wrappers in `services/` when API access is needed.
5. Export only public feature APIs from `index.ts`.

## Guardrails

- Use `inject()`, signals, and modern template control flow.
- Keep cross-feature imports through public APIs.
- Keep files small and split responsibilities early.
