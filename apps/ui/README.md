# Humanaity UI

Angular 20 frontend for the Humanaity project. It handles authentication, city browsing and creation, simulation views, and integration with the Spring Boot backend through a generated OpenAPI client.

## Stack

- Angular 20 with standalone components
- TypeScript 5.8 in strict mode
- Angular Material + SCSS
- RxJS for async flows
- Angular SSR / hydration support
- OpenAPI Generator for typed API services and models
- Pixi.js for the simulation canvas

## Current Features

- Authentication pages for `login` and `signup`
- JWT-based session handling with access + refresh tokens
- Route protection with `authGuard`
- HTTP interceptor that attaches bearer tokens and retries on `401` via refresh
- City pages for listing all cities, creating a city, and listing the current user's cities
- City detail / simulation page backed by Pixi.js rendering
- Admin tools page under `/admin`
- Light/dark theme handling through `ThemeService`

## Project Structure

```text
src/app/
├── api/                  # Generated OpenAPI client
├── core/                 # App-wide guards, interceptors, services, utilities
├── features/
│   ├── admin/
│   ├── auth/
│   └── city/
├── shared/               # Reusable layout and UI components
├── app.config.ts         # Global Angular providers
└── app.routes.ts         # Root routing
```

### Routing

- `/login`
- `/signup`
- `/cities`
- `/cities/create`
- `/cities/mine`
- `/cities/:id`
- `/admin`

All routes under `/cities` and `/admin` are protected by the auth guard.

## Backend Integration

The app is configured to call the backend at `http://localhost:8080` via `provideApi()` in `src/app/app.config.ts`.

Generated API code lives in `src/app/api/`:

- `src/app/api/api/` contains generated Angular services
- `src/app/api/model/` contains generated request/response models

Feature services wrap the generated client where app-specific behavior is needed.

## Getting Started

### Prerequisites

- Node.js and npm
- The backend running locally on `http://localhost:8080`

### Install dependencies

```bash
npm install
```

### Start the development server

```bash
npm start
```

The app will be available at `http://localhost:4200`.

## Available Scripts

```bash
npm start        # Angular dev server
npm run build    # Production build
npm run watch    # Development build in watch mode
npm test         # Karma test runner
```

### Regenerate the OpenAPI client

If the backend API changes, regenerate the frontend client with:

```bash
npm run api:generate
```

This pulls `http://localhost:8080/v3/api-docs` and updates `src/app/api/`.

## SSR Build

Build the app:

```bash
npm run build
```

Run the generated server bundle:

```bash
npm run serve:ssr:humanaity-ui
```
