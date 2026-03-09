# Post-Import Path Assumptions

This document lists path assumptions that were updated after importing the three repositories into the monorepo.

## Updated Assumptions

1. Backend Cursor MCP command no longer hardcodes local absolute paths.
   - File: `apps/backend/.cursor/mcp.json`
   - Change: Use `git rev-parse --show-toplevel` and run from `apps/mcp`, sourcing `apps/backend/.env`.

2. Frontend VS Code task working directory no longer assumes a sibling `humanaity-ui` repository.
   - File: `apps/ui/.vscode/tasks.json`
   - Change: Switch `cwd` and `fileLocation` values to `${workspaceFolder}/apps/ui`.

3. Frontend VS Code debug `webRoot` no longer assumes a sibling `humanaity-ui` repository.
   - File: `apps/ui/.vscode/launch.json`
   - Change: Switch all `webRoot` values to `${workspaceFolder}/apps/ui/src`.

4. MCP docs no longer reference backend env loading with absolute local paths.
   - File: `apps/mcp/README.md`
   - Change: Use monorepo-aware command examples and `apps/...` references.

5. Local run skills no longer point to old standalone repository paths.
   - Files:
     - `apps/backend/.cursor/skills/run-backend/SKILL.md`
     - `apps/ui/.cursor/skills/run-frontend/SKILL.md`
   - Change: Replace standalone paths with monorepo `apps/...` locations.
