---
name: mcp-smoke-validation
description: Validate backend chunks without frontend by running a standard MCP smoke flow (auth, city, simulation start/status/snapshot/stop), with quick triage for auth/API/contract drift issues. Use when the user asks to test changes via MCP or verify backend behavior from Cursor.
---
# MCP Smoke Validation

Use this skill to validate backend behavior end-to-end via MCP tools when frontend is missing or intentionally out of scope.

## When To Use

- After backend chunk implementation
- When user asks "how do I test this without UI?"
- When confirming MCP and backend still align after API/service changes

## Preconditions

- Backend running at `http://localhost:8080`
- MCP server in `apps/mcp` running and connected to Cursor
- Auth credentials available either via tool input or MCP env

If backend contract changed, in `apps/mcp` regenerate and rebuild before smoke testing:

```bash
npm run api:generate
npm run build
```

## Standard MCP Smoke Flow

Run these tools in order:

1. `health_check`
2. `auth_login`
3. `city_create` with a unique name (or `cities_mine` and pick an existing city id)
4. `simulation_status` for chosen city id
5. `simulation_start` for chosen city id
6. `simulation_status` again (expect running true)
7. `simulation_snapshot` (status + humans + derived metrics)
8. `simulation_stop`
9. `simulation_status` final check (expect running false)

## Expected Validation Outcome

- Authentication works through MCP.
- City access/creation works.
- Simulation start/stop/status path works.
- Snapshot tool can read status + humans for the same city.

For Task 2b-style lifecycle wiring, this validates create/load/resume/pause behavior indirectly through start/stop/status.

## Endpoint Map (for direct HTTP fallback)

- `POST /auth/login`
- `GET /api/cities/mine`
- `POST /api/cities`
- `POST /api/simulations/{cityId}/start`
- `POST /api/simulations/{cityId}/stop`
- `GET /api/simulations/{cityId}/status`
- `GET /api/humans/city/{cityId}`

## Failure Triage

- **Auth failure**
  - Re-run `auth_login`
  - Verify `HUMANAITY_API_EMAIL` and `HUMANAITY_API_PASSWORD`
- **Simulation 4xx/5xx**
  - Confirm city id is valid and owned by current user
  - Confirm backend base URL and backend process health
- **MCP contract drift**
  - Run `npm run api:generate` in `apps/mcp`
  - Rebuild and restart MCP server

## Minimal Done Gate

- Backend compile passes
- Full smoke flow passes once with a newly created city
- No out-of-scope API surface expansion slipped into the chunk
