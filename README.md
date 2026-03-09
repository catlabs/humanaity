# Humanaity Monorepo

This repository contains all Humanaity applications under a single history-preserving monorepo layout.

## Applications

- `apps/ui` (Angular frontend)
- `apps/backend` (Spring Boot API)
- `apps/mcp` (TypeScript MCP server)

## Local Run

Backend:

```bash
cd apps/backend
zsh -lc 'set -a; [ -f .env ] && source .env; set +a; sh ./mvnw spring-boot:run'
```

Frontend:

```bash
cd apps/ui
npm install
npm start
```

MCP:

```bash
cd apps/mcp
npm install
zsh -lc 'set -a; [ -f ../backend/.env ] && source ../backend/.env; set +a; npm run dev'
```

## Contract Generation

- Backend OpenAPI endpoint: `http://localhost:8080/v3/api-docs`
- Frontend client generation is run from `apps/ui`
- MCP schema generation is run from `apps/mcp` via `npm run api:generate`

## Shared Workspace Assets

- Shared skills live in `.cursor/skills/`
- Shared migration docs and roadmap notes live in `docs/roadmap/`
