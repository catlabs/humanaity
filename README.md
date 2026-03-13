# Humanaity

**Humanaity** is an AI-augmented **observation and simulation platform** where autonomous humans interact, exchange knowledge, and generate a coherent historical timeline. It is built as a showcase project demonstrating full-stack architecture, contract-driven development, and agile delivery practices.

## What It Does

- **User authentication** — Sign up, login, JWT-based sessions with refresh token rotation
- **City management** — Create cities, seed populations, browse and search
- **Deterministic simulation** — Tick-based civilization simulation with reproducible results (same seed ⇒ same outcome)
- **Historical timeline** — Events, inventions, and AI-enriched narratives derived from simulation state
- **Interactive visualization** — Live Pixi.js canvas showing humans on a map, event feed, inventions panel
- **MCP integration** — All backend features testable via Model Context Protocol tools, without the UI

## Architecture

The monorepo contains three applications:

| App | Tech | Role |
|-----|------|------|
| **Backend** | Spring Boot 3.5, Java 17 | REST API, auth, simulation engine, AI enrichment, H2/PostgreSQL-ready |
| **Frontend** | Angular 20, TypeScript 5.8 | SPA with Material, Pixi.js canvas, SSR support |
| **MCP Server** | Node.js, TypeScript | Bridges backend APIs over MCP stdio for agent-driven testing and exploration |

## Technical Highlights

### Authentication

- Stateless JWT with access + refresh tokens
- Spring Security integration
- Route protection and HTTP interceptor for token attachment and 401 retry via refresh
- MCP tools support auth flow for backend smoke tests without the UI

### Contract-Driven Frontend & MCP

- **Backend as single source of truth** — OpenAPI spec at `/v3/api-docs`
- **Frontend** — Typed Angular services and models generated with [OpenAPI Generator](https://openapi-generator.tech) (`@openapitools/openapi-generator-cli`). Full mapping of backend DTOs to `src/app/api/`; feature services wrap generated clients.
- **MCP** — Types generated with `openapi-typescript` from the same spec. Every backend capability used in iteration is exposed as an MCP tool and testable end-to-end.

### MCP-First Testing

The project treats MCP as a first-class consumption surface. Critical flows (auth → city → simulation → history) are executable via MCP tools, enabling backend validation without the frontend. Smoke flows and contract drift checks are part of the workflow.

## Documentation & Methodology

This project was built using **agile practices** with explicit documentation:

- **`docs/roadmap.md`** — Product vision, epics, feature dependency map, implementation order
- **`docs/sprints/`** — Sprint-by-sprint execution documents with intent, scope, task breakdown, and completion status
- **`docs/specs/`** — Locked domain specs (deterministic simulation, history ledger, read model, etc.)
- **Prompt packs** — Delegable task packs for Cursor/Codex with references to sprint and spec files

Sprints define in-scope/out-of-scope, acceptance criteria, and chunk-level status. The documentation reflects what was actually executed, not aspirational planning.

## Project Structure

```
humanaity/
├── apps/
│   ├── backend/     # Spring Boot API
│   ├── ui/          # Angular frontend
│   └── mcp/         # MCP server
├── docs/
│   ├── roadmap.md
│   ├── sprints/     # Sprint execution docs
│   └── specs/       # Domain specs
└── .cursor/         # Rules and skills for AI-assisted development
```

## Quick Start

**Prerequisites:** Java 17, Node.js 20+, optional `OPENAI_API_KEY` for AI enrichment

1. **Backend** (port 8080):

   ```bash
   cd apps/backend
   zsh -lc 'set -a; [ -f .env ] && source .env; set +a; sh ./mvnw spring-boot:run'
   ```

2. **Frontend** (port 4200):

   ```bash
   cd apps/ui
   npm install && npm start
   ```

3. **MCP** (for Cursor/agent workflows):

   ```bash
   cd apps/mcp
   npm install
   zsh -lc 'set -a; [ -f ../backend/.env ] && source ../backend/.env; set +a; npm run dev'
   ```

## Contract Generation

- **Backend OpenAPI:** `http://localhost:8080/v3/api-docs`
- **Frontend client:** `npm run api:generate` in `apps/ui`
- **MCP types:** `npm run api:generate` in `apps/mcp`

## Further Reading

- [Backend README](apps/backend/README.md) — API summary, modules, local config
- [Frontend README](apps/ui/README.md) — Features, routing, OpenAPI client usage
- [MCP README](apps/mcp/README.md) — Tools, smoke flow, contract workflow
- [Docs structure](docs/README.md) — Roadmap, sprints, specs workflow
