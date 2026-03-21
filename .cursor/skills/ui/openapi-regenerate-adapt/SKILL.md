---
name: openapi-regenerate-adapt
description: Regenerate OpenAPI Angular client and adapt feature wrappers safely. Use when backend API spec changes or generated client drift appears.
---

# OpenAPI Regenerate and Adapt

## Goal

Regenerate `src/app/api` and safely align feature services with updated signatures.

If the backend change altered the **public API contract**, also regenerate MCP types (separate generated file): from `apps/mcp` with the backend running, `npm run api:generate`, then commit `src/generated/api-types.ts`. Policy: `.cursor/rules/agent-openapi-contract-sync.mdc`.

## Workflow

1. Regenerate API client from the configured OpenAPI source.
2. Review generated diff under `src/app/api/`.
3. Update feature wrapper services (not generated files).
4. Reconcile type changes in components/services that consume wrappers.
5. Verify Blob handling paths if fetch-based responses are used.
6. Validate error handling and endpoint coverage.

## Guardrails

- Never hand-edit generated files in `src/app/api/`.
- Prefer typed wrappers over direct endpoint calls.
- Document temporary direct calls only as explicit short-term workarounds.
