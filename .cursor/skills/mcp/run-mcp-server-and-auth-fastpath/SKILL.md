---
name: run-mcp-server-and-auth-fastpath
description: Start the Humanaity MCP server from apps/mcp (avoid duplicates, source monorepo .env), then use a fast MCP workflow that checks auth and calls auth_login only when needed (no full smoke flow unless requested).
---

# Run MCP server + Auth fastpath

Use this skill when the user asks to do anything “via MCP” (create/list cities, start/stop simulations, snapshots, etc.) and they did **not** explicitly ask for a full smoke test.

## Goal

- Ensure the MCP server is running (without launching duplicates).
- Ensure the MCP server is authenticated (prefer cached tokens).
- Run the requested MCP tool(s) with minimal overhead.

## Preconditions

- Backend running at `http://localhost:8080`
- MCP server id in Cursor: `project-0-humanaity-humanaity-mcp`
- Credentials available when needed (user-provided or env):
  - `HUMANAITY_API_EMAIL`
  - `HUMANAITY_API_PASSWORD`

## 1) Start MCP server (`apps/mcp`)

### Avoid duplicate launches

Before starting, check existing terminals for an already-running command like:

- `cd <monorepo>/apps/mcp && npm run dev`

If it’s already running and healthy, **do not start another one**.

### Start command (loads monorepo `.env`)

Run from anywhere:

```bash
cd "<monorepo>/apps/mcp" && zsh -lc 'MONOREPO_ROOT="$(git rev-parse --show-toplevel)"; cd "$MONOREPO_ROOT/apps/mcp"; set -a; [ -f "$MONOREPO_ROOT/.env" ] && source "$MONOREPO_ROOT/.env"; set +a; npm run dev'
```

Success signal: dev output stays running, no crash loop, and MCP tools respond in Cursor.

## 2) Fast MCP auth workflow (no smoke test)

### Principle

Most MCP tools can be called with `{}` because the MCP server caches tokens after login.  
So: **try the requested tool first**, and only authenticate if it fails for auth reasons.

### Step-by-step

1. **Health check** (always first)
   - Tool: `health_check`
   - Expect: `status: "ok"` and `apiBaseUrl: "http://localhost:8080"`

2. **Try the target tool with cached auth**
   - Example: `cities_mine` with `arguments: {}`
   - If it succeeds, continue; **do not** login again.

3. **If auth is missing/expired**
   - Call `auth_login` with:
     - `email`
     - `password`
   - Then retry the target tool.

4. **If refresh is supported**
   - Prefer `auth_refresh` when the error indicates token expiry.
   - If refresh fails, fall back to `auth_login`.

### When to run the full smoke flow

Only run the smoke flow skill (`mcp-smoke-validation`) when:

- the user explicitly asks for a “smoke test” / “validate end-to-end”
- you suspect contract drift between MCP and backend
- you changed backend endpoints/DTOs affecting MCP tools

## 3) Tool schema discipline (required)

Before calling a tool for the first time in a session, read its schema descriptor:

`~/.cursor/projects/Users-julien-dev-humanaity-humanaity/mcps/project-0-humanaity-humanaity-mcp/tools/<tool>.json`

Confirm required args (often just `cityId`, optional `accessToken`).

## 4) Response contract (what to tell the user)

- If MCP server already running: say it’s running.
- If you started it: confirm it is running.
- If you had to login: say “authenticated via MCP” (never paste tokens).
- Then return the requested result (ids/names/status fields).

