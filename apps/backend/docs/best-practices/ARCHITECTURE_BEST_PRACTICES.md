# Architecture Best Practices

## Architecture Overview

This project uses **layered architecture with domain modules**. Each feature is organized as a self-contained module with four layers:

```
{module}/
├── api/              # REST controllers and DTOs
├── application/      # Use cases and orchestration
├── domain/           # Business entities and logic
└── infrastructure/   # Technical implementations (persistence, external services)
```

**Key Principles:**
- Domain layer has no dependencies on infrastructure
- Application layer orchestrates use cases
- API layer handles HTTP concerns only
- Infrastructure implements technical details

## Module Structure

### Standard Module Layout

```
{module}/
├── api/
│   ├── {Module}Controller.java
│   └── dto/
│       ├── {Module}Input.java
│       └── {Module}Output.java
├── application/
│   └── {Module}ApplicationService.java
├── domain/
│   └── {Module}.java              # Domain entity
└── infrastructure/
    └── persistence/
        └── {Module}Repository.java
```

### Layer Responsibilities

**API Layer** (`api/`)
- REST controllers (`@RestController`)
- Request/Response DTOs
- Input validation (`@Valid`)
- HTTP status codes
- **Never** access repositories directly

**Application Layer** (`application/`)
- Use case orchestration
- Transaction management (`@Transactional`)
- Coordinates domain and infrastructure
- Business workflow logic

**Domain Layer** (`domain/`)
- Business entities (JPA `@Entity`)
- Domain enums and value objects
- Pure business logic (no infrastructure dependencies)
- Business rules and validations

**Infrastructure Layer** (`infrastructure/`)
- Persistence: JPA repositories
- External services: adapters implementing ports
- Technical implementations

## Creating a New Module

### Step-by-Step: Example "notification" Module

1. **Create domain entity**
   ```
   notification/domain/Notification.java
   ```
   - JPA entity with `@Entity`
   - Business fields and relationships

2. **Create repository**
   ```
   notification/infrastructure/persistence/NotificationRepository.java
   ```
   - Extends `JpaRepository<Notification, Long>`
   - Custom query methods if needed

3. **Create DTOs**
   ```
   notification/api/dto/NotificationInput.java
   notification/api/dto/NotificationOutput.java
   ```
   - Input: validation annotations
   - Output: what the API returns

4. **Create application service**
   ```
   notification/application/NotificationApplicationService.java
   ```
   - `@Service` annotation
   - Orchestrates use cases
   - Uses repository for persistence

5. **Create controller**
   ```
   notification/api/NotificationController.java
   ```
   - `@RestController` with `@RequestMapping`
   - Calls application service
   - Maps domain entities to DTOs

### Quick Checklist

- [ ] Domain entity in `{module}/domain/`
- [ ] Repository in `{module}/infrastructure/persistence/`
- [ ] DTOs in `{module}/api/dto/`
- [ ] Application service in `{module}/application/`
- [ ] Controller in `{module}/api/`
- [ ] Update imports in dependent modules

## Adding Features to Existing Modules

### Adding a New Endpoint

1. Add method to Application Service:
   ```java
   public SomeOutput doSomething(SomeInput input) {
       // Orchestration logic
   }
   ```

2. Add endpoint to Controller:
   ```java
   @PostMapping("/something")
   public ResponseEntity<SomeOutput> doSomething(@Valid @RequestBody SomeInput input) {
       SomeOutput result = applicationService.doSomething(input);
       return ResponseEntity.ok(result);
   }
   ```

### Adding External Service Integration (Ports & Adapters)

1. **Create port interface** in `infrastructure/port/`:
   ```java
   public interface ExternalServicePort {
       Result doSomething(Input input);
   }
   ```

2. **Create adapter** in `infrastructure/adapter/{provider}/`:
   ```java
   @Component
   public class ProviderAdapter implements ExternalServicePort {
       // Implementation
   }
   ```

3. **Use in application service**:
   ```java
   private final ExternalServicePort externalService;
   ```

**Example:** See `ai/infrastructure/port/AiProviderPort.java` and `ai/infrastructure/adapter/openai/OpenAiAdapter.java`

## Key Patterns

### Application Service Pattern

Application services orchestrate use cases:

```java
@Service
public class CityApplicationService {
    private final CityRepository repository;
    private final OtherService otherService;
    
    @Transactional
    public City createCity(CityInput input, User owner) {
        // Validation
        // Business logic
        // Persistence
        // Side effects (e.g., call other services)
        return savedCity;
    }
}
```

**Responsibilities:**
- Coordinate domain and infrastructure
- Manage transactions
- Orchestrate workflows
- **Not** business logic (that's in domain)

### Port & Adapter Pattern (External Services)

For external services (AI, email, etc.):

1. **Port** (interface in `infrastructure/port/`):
   ```java
   public interface EmailServicePort {
       void sendEmail(String to, String subject, String body);
   }
   ```

2. **Adapter** (implementation in `infrastructure/adapter/{provider}/`):
   ```java
   @Component
   public class SendGridAdapter implements EmailServicePort {
       // SendGrid-specific implementation
   }
   ```

3. **Use in application service**:
   ```java
   private final EmailServicePort emailService;
   ```

**Benefits:** Easy to swap providers, testable, domain-agnostic

### Repository Pattern

Repositories are in `infrastructure/persistence/`:

```java
@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByNameContainingIgnoreCase(String name);
    List<City> findByOwner(User owner);
}
```

- Extend `JpaRepository<Entity, ID>`
- Custom queries as needed
- Only accessed by application services

### DTO Pattern

DTOs separate API contracts from domain:

- **Input DTOs**: Validation, what API accepts
- **Output DTOs**: What API returns (may differ from domain)

```java
// Input: validation
public class CityInput {
    @NotBlank
    @Size(max = 100)
    private String name;
}

// Output: API response
public class CityOutput {
    private Long id;
    private String name;
    // May exclude internal fields
}
```

## Cross-Module Dependencies

### Rules

1. **Domain entities can reference other domain entities**:
   ```java
   // city/domain/City.java
   @ManyToOne
   private User owner;  // auth.domain.User
   ```

2. **Application services can use other application services**:
   ```java
   // city/application/CityApplicationService.java
   private final HumanGenerationApplicationService humanService;
   ```

3. **Never** import infrastructure from other modules:
   - Don't import `auth.infrastructure.*` from city module
   - Use domain entities or application services instead

4. **Common module** (`common/`) is shared:
   - Exception handling
   - Utilities
   - Mappers (future)

### Example: City References User

```java
// city/domain/City.java
import eu.catlabs.humanaity.auth.domain.User;  // ✅ OK - domain to domain

// city/application/CityApplicationService.java
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;  // ✅ OK - infrastructure to infrastructure (same module or common)
```

## Infrastructure

### Configuration Files

All Spring configuration in `infrastructure/config/`:
- `SecurityConfig.java` - Security setup
- `CorsConfig.java` - CORS configuration
- `OpenApiConfig.java` - Swagger/OpenAPI
- `H2ServerConfig.java` - Database config

### Exception Handling

Global exception handler in `common/exception/GlobalExceptionHandler.java`:
- Handles `BusinessException`
- Handles `IllegalArgumentException`
- Returns appropriate HTTP status codes

## Quick Reference

### Package Naming

- Module: `eu.catlabs.humanaity.{module}`
- API: `eu.catlabs.humanaity.{module}.api`
- Application: `eu.catlabs.humanaity.{module}.application`
- Domain: `eu.catlabs.humanaity.{module}.domain`
- Infrastructure: `eu.catlabs.humanaity.{module}.infrastructure`

### Annotations

- Controllers: `@RestController`, `@RequestMapping`
- Services: `@Service`
- Repositories: `@Repository`
- Entities: `@Entity`, `@Id`, `@GeneratedValue`
- Transactions: `@Transactional` (on application services)

### File Locations

| Component | Location |
|-----------|----------|
| Controller | `{module}/api/{Module}Controller.java` |
| DTOs | `{module}/api/dto/` |
| Application Service | `{module}/application/{Module}ApplicationService.java` |
| Domain Entity | `{module}/domain/{Module}.java` |
| Repository | `{module}/infrastructure/persistence/{Module}Repository.java` |
| External Adapter | `{module}/infrastructure/adapter/{provider}/` |
| Port Interface | `{module}/infrastructure/port/` |
| Config | `infrastructure/config/` |

## Examples

### Complete Module Example: City

- **Domain**: `city/domain/City.java` - Entity with JPA annotations
- **Repository**: `city/infrastructure/persistence/CityRepository.java`
- **DTOs**: `city/api/dto/CityInput.java`, `CityOutput.java`
- **Service**: `city/application/CityApplicationService.java`
- **Controller**: `city/api/CityController.java`

### Port & Adapter Example: AI

- **Port**: `ai/infrastructure/port/AiProviderPort.java`
- **Adapter**: `ai/infrastructure/adapter/openai/OpenAiAdapter.java`
- **Application Service**: `ai/application/AiGenerationService.java`
- **Domain**: `ai/domain/AiPrompt.java`, `AiResponse.java`, `AiProvider.java`

## Common Mistakes to Avoid

1. **Controllers accessing repositories** - Use application services
2. **Business logic in controllers** - Move to application/domain
3. **Infrastructure dependencies in domain** - Keep domain pure
4. **Cross-module infrastructure imports** - Use domain or application services
5. **Missing `@Transactional`** - Add to write operations in application services
