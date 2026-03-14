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

`humanaity-mcp` generates backend-owned schema types with `openapi-typescript` from the live Humanaity backend OpenAPI spec at `${HUMANAITY_API_BASE_URL:-http://localhost:8080}/v3/api-docs`.

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

Current decision: keep live-spec generation (`${HUMANAITY_API_BASE_URL:-http://localhost:8080}/v3/api-docs`) as the default workflow for now, and do not commit a spec snapshot yet.

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

1. Update backend contracts in `apps/backend` (DTOs/controllers/OpenAPI annotations).
2. Ensure the backend is running and exposing `${HUMANAITY_API_BASE_URL:-http://localhost:8080}/v3/api-docs`.
3. In `apps/mcp`, regenerate generated schema types:

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

## MCP-proof contract (project rule)

This project treats MCP as a first-class consumption and testing surface.

That means every backend capability that matters for an iteration must be **MCP-proof**:

- it is reachable through an MCP tool (endpoint parity for critical flows)
- it can be exercised end-to-end without relying on UI-only behavior
- tool responses include machine-readable JSON so automated agents (and this chat) can read ids and coordinates

### Endpoint parity expectation

When a backend endpoint is added or changed in a sprint (especially anything used by UI/MVP flows), the same sprint must also:

1. regenerate types (`npm run api:generate`)
2. add/update the MCP tool wrapper in `src/tools/*`
3. rebuild (`npm run build`)
4. run at least one smoke path through MCP

### Tool output expectation (JSON)

MCP tools must include a JSON payload in the primary `content[0].text` output (in addition to `structuredContent`).

This is required because some clients only surface the tool `content` text, and we still need to read:

- ids (cityId, humanId, runId)
- names
- key numeric state (e.g. human `x`/`y`)

Recommended response pattern:

```ts
return {
  content: [{ type: "text", text: JSON.stringify({ ok: true, /* payload */ }, null, 2) }],
  structuredContent: { ok: true, /* payload */ },
};
```

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

Recommended initial command in `apps/backend/.cursor/mcp.json`:

```json
"args": [
  "-lc",
  "MONOREPO_ROOT=\"$(git rev-parse --show-toplevel)\" && set -a && source \"$MONOREPO_ROOT/apps/backend/.env\" && set +a && npm run dev"
]
```

Optional switch after stability is confirmed (same file, same env loading):

```json
"args": [
  "-lc",
  "MONOREPO_ROOT=\"$(git rev-parse --show-toplevel)\" && set -a && source \"$MONOREPO_ROOT/apps/backend/.env\" && set +a && npm run start"
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

- `simulation_create` (optional `seed`)
- `simulation_load`
- `simulation_pause`
- `simulation_resume`
- `simulation_step` (optional `count`, repeats deterministic single-step execution)
- `simulation_start`
- `simulation_stop`
- `simulation_status`
- `simulation_snapshot` (composite tool: status + humans + derived metrics)
- `simulation_history_events` (optional `fromTick`, `toTick`, `limit`)
- `simulation_history_inventions` (optional `fromTick`, `toTick`, `limit`)
- `simulation_history_timeline` (optional `fromTick`, `toTick`, `limit`)

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
4. Pick a city id and run `simulation_create` then `simulation_load`.
5. Run `simulation_resume` and verify `simulation_status`.
6. Run `simulation_pause` and verify `simulation_status` again.
7. Run `simulation_start` and optionally `simulation_snapshot`, then `simulation_stop`.
8. Run `simulation_history_timeline` with a city id after stepping:
   - `{ "cityId": <validCityId>, "fromTick": 0, "limit": 200 }`
9. Optionally verify list endpoints:
   - `{ "cityId": <validCityId>, "fromTick": 0, "limit": 50 }` via `simulation_history_events`
   - `{ "cityId": <validCityId>, "fromTick": 0, "limit": 50 }` via `simulation_history_inventions`
10. Run `human_create` with minimal input:
   - `{ "cityId": <validCityId> }`
11. Run `human_create` with advanced input:
   - `{ "cityId": <validCityId>, "name": "Ari", "busy": false, "x": 0.3, "y": 0.7, "creativity": 0.9, "intellect": 0.8, "sociability": 0.6, "practicality": 0.5, "personality": "VISIONARY" }`
12. Validate error handling with an invalid request:
   - invalid city: `{ "cityId": -1 }`
   - invalid range: `{ "cityId": <validCityId>, "creativity": 2 }`

If any tool fails, inspect returned `structuredContent.error` and `structuredContent.details` for normalized backend error information.
