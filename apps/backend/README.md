# Humanaity Backend

Spring Boot 3.5 REST API for the Humanaity project. The backend manages authentication, cities, humans, and city simulations, and can generate inhabitants with OpenAI plus a Faker-based fallback.

## Stack

- Java 17
- Spring Boot 3.5
- Spring Web MVC
- Spring Security with stateless JWT authentication
- Spring Data JPA
- H2 file database
- Springdoc OpenAPI / Swagger UI
- Spring AI OpenAI starter
- Java Faker
- Lombok

## Main Modules

- `auth`: signup, login, refresh token rotation, logout
- `city`: city CRUD and ownership-aware endpoints
- `human`: human CRUD and city-scoped queries
- `simulation`: start, stop, and status endpoints for city simulations
- `ai`: AI prompt building and OpenAI adapter
- `common`: shared exceptions and cross-cutting utilities
- `infrastructure`: security, CORS, OpenAPI, and H2 configuration

## Architecture

The codebase follows a layered module structure:

```text
{module}/
├── api/              # Controllers and DTOs
├── application/      # Use-case orchestration
├── domain/           # Business entities and enums
└── infrastructure/   # Repositories and technical adapters
```

Examples in the current codebase:

- `auth` includes JWT services and token persistence
- `city` exposes city endpoints and delegates business logic to `CityApplicationService`
- `ai` uses a port/adapter approach with `AiProviderPort` and the OpenAI adapter

## API Summary

### Public auth endpoints

- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### Protected endpoints

- `GET /api/cities`
- `GET /api/cities/mine`
- `GET /api/cities/search?name=foo`
- `GET /api/cities/{id}`
- `POST /api/cities`
- `PUT /api/cities/{id}`
- `DELETE /api/cities/{id}`
- `GET /api/humans/{id}`
- `GET /api/humans/city/{cityId}`
- `POST /api/humans`
- `PUT /api/humans/{id}`
- `DELETE /api/humans/{id}`
- `POST /api/simulations/{cityId}/start`
- `POST /api/simulations/{cityId}/stop`
- `GET /api/simulations/{cityId}/status`

All non-auth application routes require `Authorization: Bearer <access_token>`.

## Local Configuration

The default local configuration is in `src/main/resources/application.properties`.

Important values:

- Server port: `8080`
- H2 database: `jdbc:h2:file:./data/testdb;AUTO_SERVER=TRUE`
- Swagger/OpenAPI docs: `/v3/api-docs` and `/swagger-ui.html`
- JWT access token expiration: `900000` ms
- JWT refresh token expiration: `604800000` ms
- OpenAI API key: `OPENAI_API_KEY`

Current CORS configuration allows `http://localhost:4200`.

## Running Locally

### Prerequisites

- Java 17
- Optional: `OPENAI_API_KEY` if you want OpenAI-backed generation instead of fallback behavior

### Start the app

```bash
./mvnw spring-boot:run
```

Useful local URLs:

- API base: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

The application also starts an H2 TCP server on port `9092`.

## Testing

Run the test suite with:

```bash
./mvnw test
```

## Frontend Pairing

The Angular frontend is configured to call this backend on `http://localhost:8080` and can regenerate its typed client from `/v3/api-docs`.
