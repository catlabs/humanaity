---
name: connect-mcp
description: Explain how to use the Humanaity MCP server (project-0-humanaity-humanaity-mcp) from Cursor, including auth, tool discovery, and common flows like listing the current user's cities.
---

# Connect to Humanaity MCP

## Goal

Give a fast, repeatable way to talk to the Humanaity backend via the MCP server without re-discovering tools or schemas every time.

Use this skill whenever you need to:

- list or create cities
- start/stop/check simulations
- fetch humans or snapshots
- smoke-test backend behavior without touching the UI

## MCP server and tools

- **Server id:** `project-0-humanaity-humanaity-mcp`
- **Tool descriptors:** `~/.cursor/projects/Users-julien-dev-humanaity-humanaity/mcps/project-0-humanaity-humanaity-mcp/tools/*.json`

Key tools:

- `health_check` – quick connectivity check to backend via MCP
- `auth_login` – authenticate and cache JWT access/refresh tokens
- `auth_refresh` – refresh tokens when needed
- `cities_mine` – list cities owned by the authenticated user
- `cities_list` – list all visible cities (admin/broader scope)
- `city_create` / `city_update` – manage cities
- `humans_by_city` – list humans for a given city
- `simulation_create` / `simulation_load` – manage simulation runs
- `simulation_start` / `simulation_stop` / `simulation_status` – control run lifecycle
- `simulation_pause` / `simulation_resume` – pause/resume a run
- `simulation_step` – advance a city by one or more deterministic steps without starting the scheduler
- `simulation_snapshot` – inspect run status + humans + metrics

Always read the specific tool descriptor JSON before calling a tool for the first time in a session to confirm argument names and types.

## Preconditions

- Backend running at `http://localhost:8080` (see backend run skill).
- MCP server for Humanaity already configured in Cursor and running (usually `apps/mcp`).
- Valid API credentials available for login when needed.

If backend contracts changed, regenerate and rebuild `apps/mcp` before smoke-testing:

```bash
cd apps/mcp
npm run api:generate
npm run build
```

## Standard connection flow (high level)

Use this order whenever you need to interact with the backend via MCP:

1. **Health check**
   - Use `CallMcpTool` with:
     - `server: "project-0-humanaity-humanaity-mcp"`
     - `toolName: "health_check"`
   - If this fails, fix backend or MCP before doing anything else.

2. **Authenticate**
   - Preferred: rely on existing cached tokens if MCP is already logged in.
     - First, try the target tool (e.g. `cities_mine`) without supplying explicit tokens.
     - If it fails with an auth error, fall back to `auth_login`.
   - To log in:
     - Read `auth_login` schema from:
       - `~/.cursor/projects/Users-julien-dev-humanaity-humanaity/mcps/project-0-humanaity-humanaity-mcp/tools/auth_login.json`
     - Call:
       - `server: "project-0-humanaity-humanaity-mcp"`
       - `toolName: "auth_login"`
       - `arguments: { "email": "<user email>", "password": "<user password>" }`
   - The MCP server caches tokens internally; most follow-up tools can omit explicit access-token arguments.

3. **Run the desired domain tool**
   - Read the tool descriptor JSON under the same `tools/` folder.
   - Use `CallMcpTool` with:
     - `server: "project-0-humanaity-humanaity-mcp"`
     - `toolName: "<tool_name_here>"`
     - `arguments` matching the schema (often `{}` or a small object).

4. **Handle token refresh if needed**
   - If a tool fails with a token-expired-style error:
     - Try `auth_refresh` first if available (see its descriptor).
     - Otherwise, repeat `auth_login`.

## Tool output contract (critical for AI usage)

To keep MCP tools usable from chat (without direct HTTP calls), every Humanaity MCP tool MUST:

- Return **machine-readable JSON** in its primary `content[0].text` field, not only a human summary.
- Mirror that JSON in `structuredContent` whenever possible.

Recommended pattern:

```ts
return {
  content: [
    {
      type: "text",
      text: JSON.stringify({ ok: true, /* payload */ }, null, 2),
    },
  ],
  structuredContent: { ok: true, /* payload */ },
};
```

When adding new MCP tools (or updating existing ones), always:

- Include ids, names, and key numeric fields (like `x`/`y` positions) in the JSON.
- Avoid lossy summaries such as `"Fetched N cities."` as the only text output.
- Keep error text simple but preserve details in `structuredContent.details` via `toToolError`.

## Concrete example: list cities for current user

When a user asks for their cities via MCP, follow this recipe:

1. **Ensure backend + MCP are running**
   - Backend at `http://localhost:8080` (use backend run skill).
   - MCP server `apps/mcp` is running and hooked up to Cursor.

2. **Optional: health check**
   - `CallMcpTool`:
     - `server: "project-0-humanaity-humanaity-mcp"`
     - `toolName: "health_check"`

3. **Try listing cities without manual tokens**
   - Read `cities_mine` descriptor:
     - `~/.cursor/projects/Users-julien-dev-humanaity-humanaity/mcps/project-0-humanaity-humanaity-mcp/tools/cities_mine.json`
   - Note: in this project, `cities_mine` accepts an optional `accessToken` argument; MCP typically has the token cached from previous `auth_login`.
   - First attempt:
     - `CallMcpTool` with:
       - `server: "project-0-humanaity-humanaity-mcp"`
       - `toolName: "cities_mine"`
       - `arguments: {}` (or omit entirely, depending on bridge).

4. **If `cities_mine` fails due to auth**
   - Call `auth_login` once with credentials.
   - Retry `cities_mine` exactly as in step 3.

5. **Present results**
   - Surface:
     - city ids
     - names
     - any additional fields (tick/year, metrics) returned by the tool.
   - Do not expose raw tokens or secrets in responses.

## Concrete example: minimal simulation smoke flow via MCP

When validating simulation behavior without UI:

1. `health_check`
2. `auth_login` (if needed)
3. `cities_mine` to pick an existing city, or `city_create` to create one.
4. `simulation_status` for the chosen city id.
5. `simulation_start` for that city id.
6. `simulation_status` again, expect running `true`.
7. `simulation_snapshot` for the same city id.
8. `simulation_stop`.
9. `simulation_status` final check, expect running `false`.

This is the same flow described in the `mcp-smoke-validation` skill, but framed explicitly around MCP tools and `CallMcpTool` parameters.

## Failure triage

- **health_check fails**
  - Confirm backend at `http://localhost:8080` is reachable.
  - Confirm MCP server is running and configured with the correct backend URL.
- **auth-related errors**
  - Re-run `auth_login` with known-good credentials.
  - If `auth_refresh` exists, try it when tokens are expired but credentials should still be valid.
- **Tool/schema mismatch**
  - Always re-read the tool’s JSON descriptor before assuming argument shape.
  - If contracts changed, regenerate and rebuild `apps/mcp` (`npm run api:generate && npm run build`) and restart MCP.

