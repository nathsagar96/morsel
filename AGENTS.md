# Morsel — Spring Boot 4.1.0 / Java 25

## Quick start

```bash
./mvnw spotless:apply          # format with Palantir JavaFormat
./mvnw spotless:check          # verify formatting
./mvnw test                    # all tests (Testcontainers spins up postgres:18-alpine)
./mvnw test -Dtest=ClassName   # single test class
./mvnw spring-boot:run         # requires `docker compose up -d` for Postgres
```

## Stack

| Concern       | Choice                                         |
|---------------|------------------------------------------------|
| Web           | Spring Web MVC                                 |
| DB            | PostgreSQL + Flyway + Spring Data JPA          |
| Security      | Spring Security                                |
| Validation    | `@Valid` + `jakarta.validation`                |
| Observability | Actuator + Micrometer Prometheus               |
| Format        | Spotless + Palantir JavaFormat                 |
| Codegen       | Lombok + `spring-boot-configuration-processor` |
| Tests         | Testcontainers (PostgreSQL), MockMvc           |

## Architecture conventions

- **Constructor injection** (`@RequiredArgsConstructor`), no `@Autowired` on fields
- **Java Records** for DTOs (request/response), not `@Data`
- **Thin controllers** → service layer → repository (no repo access from controllers)
- **ProblemDetail** (RFC 7807) for error responses (`spring.mvc.problemdetails.enabled=true`)
- **Sealed exception hierarchy** extending a base `ApplicationException`

## Development

- Postgres: `docker compose up -d` (port 5432, user/pass: `morsel`/`secret`, DB: `morsel`)
- Flyway migrations live in `src/main/resources/db/migration/`
- Testcontainers config is in `TestcontainersConfiguration` — do NOT use H2 in tests
- Run locally with Testcontainers instead of Docker: use `MorselApplication` (test main class)
- Lombok + config processor annotation processing is configured in `pom.xml`

## Operational

- Virtual threads: `spring.threads.virtual.enabled=true` (Java 21+ platform thread pinning with `synchronized` — prefer
  `ReentrantLock`)
- Active profile: `spring.application.name=morsel` (default)
- DDL: `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns the schema)
- `spring.jpa.open-in-view=false` to avoid OSIV antipattern
