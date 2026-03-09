---
name: run-backend
description: Run the Humanaity Spring Boot backend from humanaity-be, source the backend .env file, confirm it is reachable at http://localhost:8080, and avoid duplicate launches by checking existing terminals first. Use when the user asks to start, run, launch, debug, serve, or verify the backend, API, Spring Boot server, or local server.
---

# Run Backend

## Goal

Launch or verify the Humanaity backend from `humanaity-be`.

## Project Location

- Backend repo: `/Users/julien/dev/humanaity/humanaity-be`
- Default URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Workflow

1. Check existing terminals first to avoid duplicates.
2. If the backend is already running and healthy, tell the user instead of starting another one.
3. If it is not running, source `/Users/julien/dev/humanaity/humanaity-be/.env` before launch.
4. Start the app from `/Users/julien/dev/humanaity/humanaity-be`.
5. Verify startup from terminal logs or with a request to Swagger/OpenAPI.

## Commands

Compile only:

```bash
sh ./mvnw -q -DskipTests compile
```

Start the backend:

```bash
zsh -lc 'set -a; [ -f .env ] && source .env; set +a; sh ./mvnw spring-boot:run'
```

Debug option in Cursor:

- Use `Run and Debug` with `Debug Humanaity Backend`
- Or use `Terminal: Run Task` with `backend: run`

## Notes

- `.env` is optional for basic startup but should be loaded so OpenAI-related config is available.
- The app also exposes H2 console support and starts an H2 TCP server on `9092`.
- If the user asks for the frontend too, ask for it to be started separately from the UI project.

## Success Checks

- Spring Boot logs show Tomcat started on port `8080`.
- `http://localhost:8080/swagger-ui.html` responds.

## Response Style

- If already running, say so and share the URL.
- If you started it, confirm the command used and the URL.
