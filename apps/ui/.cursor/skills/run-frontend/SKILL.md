---
name: run-frontend
description: Run the Humanaity Angular frontend dev server from humanaity-ui, confirm it is reachable at http://localhost:4200, and avoid duplicate launches by checking existing terminals first. Use when the user asks to start, run, launch, serve, or verify the frontend, UI, Angular app, or local web app.
---

# Run Frontend

## Goal

Launch or verify the Humanaity frontend from `humanaity-ui`.

## Project Location

- Frontend repo: `/Users/julien/dev/humanaity/humanaity-ui`
- Default URL: `http://localhost:4200`
- Backend dependency: `http://localhost:8080`

## Workflow

1. Check existing terminals first to avoid duplicates.
2. If a frontend dev server is already running and healthy, tell the user instead of starting another one.
3. If it is not running, start it from `/Users/julien/dev/humanaity/humanaity-ui`.
4. Verify startup from terminal output or by checking `http://localhost:4200`.

## Commands

Install dependencies if needed:

```bash
npm install
```

Start the frontend:

```bash
npm start
```

Alternative direct command:

```bash
ng serve
```

## Notes

- The frontend expects the backend at `http://localhost:8080`.
- If the user asks to run the full stack, ask for the backend to be started separately from the backend project.
- Prefer keeping the process in a dedicated terminal so logs remain visible.

## Success Checks

- Dev server output shows Angular started successfully.
- `http://localhost:4200` responds.

## Response Style

- If already running, say so and share the URL.
- If you started it, confirm the command used and the URL.
