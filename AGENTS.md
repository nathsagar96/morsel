# Morsel — Spring Boot 4.1.0 / Java 25

## Key Commands

- `./mvnw test` - Run all tests (uses Testcontainers with postgres:18-alpine)
- `./mvnw test -Dtest=ClassName` - Run a single test class
- `./mvnw spotless:apply` - Format code with Palantir JavaFormat (run before committing)
- `./mvnw spotless:check` - Verify formatting in CI
- `./mvnw spring-boot:run` - Start dev server with Docker Compose auto-start
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` - Production profile (requires env vars)

## Architecture Overview

Single-module Maven project with 16 API endpoints. Key patterns:

- **EntityGraph required** on all repository methods serving responses (avoids N+1 with OSIV disabled)
- **Specification.where(null)` ambiguous** in Spring Data JPA 3.x - use `(root, query, cb) -> cb.conjunction()`
- **RecipeSpecification** escapes LIKE wildcards with `!` character
- **Rating upsert** uses native PostgreSQL `INSERT ... ON CONFLICT DO UPDATE` for atomic one-rating-per-user
- **List recipes** uses `RecipeSummaryResponse` (excludes `instructions`); sort fields: `{id, title, averageRating, createdAt, updatedAt}`

## Testing Quirks

Four test styles under `src/test/java/com/morsel/`:

- **WebMvcTest**: Requires manual `SecurityContextHolder` setup in `@BeforeEach` and cleanup in `@AfterEach`
- **DataJpaTest**: Uses Testcontainers with PostgreSQL (no H2 allowed)
- **MockitoExtension**: For service layer tests
- **Plain JUnit 5**: For exception handlers, JWT providers, etc.

## Operational Gotchas

- **Virtual threads**: `spring.threads.virtual.enabled=true` - avoid `synchronized`, use `ReentrantLock`
- **DDL**: `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema)
- **File storage**: Animated GIFs flattened to a single frame; SVG rejected (XSS risk)
- **Prod profile**: Requires env vars for DB, JWT, CORS, mail, storage – no defaults
- **Max page size**: 50 enforced via `spring.data.web.pageable.max-page-size`

## Security

- JWT in `Authorization: Bearer <token>` header with HMAC-SHA
- `CustomUserDetailsService` accepts username **or** email (tries username first)
- Image serving (`GET /api/v1/images/**`) is public

## Docker Compose

```yaml
services:
  postgres:
    image: postgres:18-alpine
    environment:
      - POSTGRES_DB=morsel
      - POSTGRES_PASSWORD=secret
      - POSTGRES_USER=morsel
    ports:
      - "5432:5432"
```
