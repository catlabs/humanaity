# Humanaity MCP Server

Node.js + TypeScript MCP server that bridges Humanaity backend APIs over MCP stdio transport.

## Requirements

- Node.js 20+ (Node.js 22 recommended)
- Running Humanaity backend API (default: `http://localhost:8080`)

## Install

```bash
npm install
```

## API contract generation

`humanaity-mcp` generates backend-owned schema types with `openapi-typescript` from the live Humanaity backend OpenAPI spec at `http://localhost:8080/v3/api-docs`.

This repository does not use `openapi-generator-cli` for MCP contract generation. The generated output lives in `src/generated/api-types.ts` and should be treated as generated-only.

Application-facing code should import backend DTO aliases from `src/contracts.ts`, which wraps generated DTOs and keeps MCP-specific composite types handwritten in one place.

Regenerate the types:

```bash
npm run api:generate
```

Check for drift against the current live backend spec:

```bash
npm run api:generate:check
```

## CI reproducibility strategy (decision)

Current decision: keep live-spec generation (`http://localhost:8080/v3/api-docs`) as the default workflow for now, and do not commit a spec snapshot yet.

Rationale:

- it keeps backend OpenAPI as the single source of truth
- it avoids maintaining a second contract artifact in this repository
- local/backend-first contract iteration remains fast and simple

When to introduce a committed OpenAPI spec later:

- CI frequently fails because live-spec availability or timing is flaky
- backend and MCP pipelines run independently and cannot reliably coordinate startup ordering
- we need stricter historical reproducibility for release branches/audits

Recommended migration path if/when this becomes necessary:

1. Commit a generated `openapi.json` snapshot in this repository (for CI only).
2. Add `api:generate:ci` to generate from the committed file.
3. Keep `api:generate` (live URL) for day-to-day local development.
4. Add a guard job that compares committed spec vs live backend spec on coordinated branches and fails on drift.
5. Require explicit snapshot refresh in PRs that intentionally change backend contracts.

## Backend-to-MCP regeneration workflow

Use this when backend DTOs, endpoint signatures, or OpenAPI annotations change:

1. Update backend contracts in `humanaity-be` (DTOs/controllers/OpenAPI annotations).
2. Ensure the backend is running locally and exposing `http://localhost:8080/v3/api-docs`.
3. In `humanaity-mcp`, regenerate generated schema types:

   ```bash
   npm run api:generate
   ```

4. Rebuild MCP to verify typed call sites still compile:

   ```bash
   npm run build
   ```

5. Run the drift check command before commit/PR to ensure generated contracts are up to date with the live backend spec:

   ```bash
   npm run api:generate:check
   ```

`api:generate:check` reruns generation and fails if `src/generated/api-types.ts` changes, which protects against contract drift.

## Configuration

The server reads configuration from environment variables:

- `HUMANAITY_API_BASE_URL` (optional, default `http://localhost:8080`)
- `HUMANAITY_API_TIMEOUT_MS` (optional, default `15000`)
- `HUMANAITY_API_ACCESS_TOKEN` (optional)
- `HUMANAITY_API_REFRESH_TOKEN` (optional)
- `HUMANAITY_API_EMAIL` (optional, enables auto-login fallback)
- `HUMANAITY_API_PASSWORD` (optional, enables auto-login fallback)

Example:

```bash
export HUMANAITY_API_BASE_URL="http://localhost:8080"
export HUMANAITY_API_EMAIL="user@example.com"
export HUMANAITY_API_PASSWORD="your-password"
```

## Run

Development:

```bash
npm run dev
```

Build:

```bash
npm run build
```

Start built server:

```bash
npm run start
```

## Cursor MCP launch mode

Keep Cursor connected to this server in development mode first, then switch to built mode only after the end-to-end MCP connection is stable.

Recommended initial command in `humanaity-be/.cursor/mcp.json`:

```json
"args": [
  "-lc",
  "set -a; source /Users/julien/dev/humanaity/humanaity-be/.env; set +a; npm run dev"
]
```

Optional switch after stability is confirmed (same file, same env loading):

```json
"args": [
  "-lc",
  "set -a; source /Users/julien/dev/humanaity/humanaity-be/.env; set +a; npm run start"
]
```

Use built mode when you want quieter startup/runtime logs and behavior closer to production packaging. Rebuild (`npm run build`) before using `npm run start`.

## Available MCP tools

### Health

- `health_check`

### Authentication

- `auth_login`
- `auth_refresh`

### Cities

- `cities_list`
- `cities_mine`
- `city_create`
- `city_update`

### Humans

- `humans_by_city`
- `human_create`

`human_create` input surface:

- Required: `cityId` (positive integer)
- Optional: `name`, `busy`, `x`, `y`, `creativity`, `intellect`, `sociability`, `practicality`, `personality`, `accessToken`
- Numeric constraints: `x`, `y`, `creativity`, `intellect`, `sociability`, `practicality` must be within `[0, 1]`

Behavior/caveats:

- Backend may recompute `personality` from trait values, so caller-provided `personality` is best-effort.
- Avoid passing explicit null-like values for optional numeric fields; omit the field instead.

### Simulations

- `simulation_start`
- `simulation_stop`
- `simulation_status`
- `simulation_snapshot` (composite tool: status + humans + derived metrics)

## Token handling behavior

- Most tools accept optional `accessToken`.
- If `accessToken` is omitted, the server uses a cached token if available.
- `auth_login` and `auth_refresh` update the in-process token cache.
- If no token is cached, tools can auto-login when `HUMANAITY_API_EMAIL` and `HUMANAITY_API_PASSWORD` are configured.

## Local smoke-check flow

Use this quick sequence against a local backend:

1. Run `health_check` to verify server startup and config.
2. Run `auth_login` (or rely on configured email/password fallback).
3. Run `cities_mine` to validate authenticated backend access.
4. Pick a city id and run `simulation_status`.
5. Run `human_create` with minimal input:
   - `{ "cityId": <validCityId> }`
6. Run `human_create` with advanced input:
   - `{ "cityId": <validCityId>, "name": "Ari", "busy": false, "x": 0.3, "y": 0.7, "creativity": 0.9, "intellect": 0.8, "sociability": 0.6, "practicality": 0.5, "personality": "VISIONARY" }`
7. Validate error handling with an invalid request:
   - invalid city: `{ "cityId": -1 }`
   - invalid range: `{ "cityId": <validCityId>, "creativity": 2 }`
8. Optionally run `simulation_start`, then `simulation_snapshot`, then `simulation_stop`.

If any tool fails, inspect returned `structuredContent.error` and `structuredContent.details` for normalized backend error information.
