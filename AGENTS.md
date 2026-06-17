# Morsel — Spring Boot 4.1.0 / Java 25

## Commands

```bash
./mvnw spotless:apply          # format with Palantir JavaFormat (run before committing)
./mvnw spotless:check          # verify formatting in CI
./mvnw test                    # all tests (Testcontainers spins up postgres:18-alpine)
./mvnw test -Dtest=ClassName   # single test class
./mvnw spring-boot:run         # auto-starts Docker Compose via spring-boot-docker-compose
./mvnw verify                  # includes spotless:check
```

## Stack

| Concern       | Choice                                         |
|---------------|------------------------------------------------|
| Web           | Spring Web MVC                                 |
| DB            | PostgreSQL + Flyway + Spring Data JPA          |
| Security      | Spring Security + JWT (jjwt 0.12.6, HMAC-SHA)  |
| Validation    | `@Valid` + `jakarta.validation`                |
| Observability | Actuator + Micrometer Prometheus               |
| Format        | Spotless + Palantir JavaFormat                 |
| Codegen       | Lombok + `spring-boot-configuration-processor` |
| Tests         | Testcontainers, MockMvc, Mockito               |

## Architecture

Single-module Maven project under `com.morsel`:

```
com.morsel
├── Application.java                 # @SpringBootApplication + @ConfigurationPropertiesScan
├── config/                          # SecurityConfig, JpaConfig, JwtProperties (record)
├── controller/                      # AuthController, RecipeController
├── dto/request/                     # SignUpRequest, LoginRequest, CreateRecipeRequest, UpdateRecipeRequest (records)
├── dto/response/                    # AuthResponse, RecipeResponse (records with static of() factory)
├── exception/                       # sealed: ApplicationException ← DuplicateResourceException, ForbiddenException, ResourceNotFoundException
├── mapper/                          # UserMapper, RecipeMapper (@Component)
├── model/                           # User, Recipe, Ingredient, Comment, Rating + Role enum
├── repository/                      # JpaRepository interfaces
├── security/                        # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal (record implements UserDetails)
└── service/                         # UserService, CustomUserDetailsService, RecipeService
```

**API endpoints** (7 total):

| Action                    | Path                          | Auth           |
|---------------------------|-------------------------------|----------------|
| Sign up                   | `POST /api/v1/auth/signup`    | permitAll      |
| Sign in                   | `POST /api/v1/auth/signin`    | permitAll      |
| Create recipe             | `POST /api/v1/recipes`        | authenticated  |
| List recipes (paginated)  | `GET /api/v1/recipes`         | authenticated  |
| Get recipe by id          | `GET /api/v1/recipes/{id}`    | authenticated  |
| Update recipe (owner)     | `PUT /api/v1/recipes/{id}`    | owner check    |
| Delete recipe (admin)     | `DELETE /api/v1/recipes/{id}` | admin only     |

**Entity model**:

- `User` is the root aggregate with cascade ALL + orphanRemoval on its `@OneToMany` collections (recipes, comments, ratings)
- `Recipe` also cascades to comments/ratings (dual ownership)
- `Recipe` ↔ `Ingredient` is a bidirectional `@ManyToMany` via `recipe_ingredients` join table
- All `@ManyToOne` are `FetchType.LAZY`; OSIV is disabled (`open-in-view=false`)
- Lazy collections require `@Transactional` or `@EntityGraph` to avoid `LazyInitializationException`
- `RecipeRepository` uses `@EntityGraph(attributePaths = {"author", "ingredients"})` on `findAll` and `findWithDetailsById`

## Conventions

- **Constructor injection** (`@RequiredArgsConstructor`), no `@Autowired` on fields
- **Java Records** for DTOs and `@ConfigurationProperties` (not `@Data`); entities use Lombok `@Builder`
- **Static factory methods** (`of(...)`) on response records
- **Sealed exception hierarchy** extending `sealed abstract class ApplicationException`
- **ProblemDetail** (RFC 7807) for error responses (`spring.mvc.problemdetails.enabled=true`)
- Package names are singular (`model`, `repository`, `service`)
- Test method names: `snake_case` with `@DisplayName` on class and each method
- **Separate DTOs for create vs update** — never reuse a single request type for both

## Security / JWT

- `POST /api/v1/auth/**` is `permitAll`; **all other endpoints require authentication**
- CSRF disabled, CORS wide open (`*`), sessions STATELESS, `@EnableMethodSecurity` enabled
- JWT in `Authorization: Bearer <token>` header; HMAC-SHA via jjwt
- `app.jwt.secret` / env `JWT_SECRET` (base64), default dev key in `application.yaml`
- `app.jwt.expiration-ms` / env `JWT_EXPIRATION_MS` (default 24h)
- `CustomUserDetailsService.loadUserByUsername()` accepts username **or** email (tries username first)

## Tests (74 total)

Three styles, all under `src/test/java/com/morsel/`:

| Style                                 | Used for                                                | Key setup                                                                                        |
|---------------------------------------|---------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| Plain JUnit 5                         | GlobalExceptionHandler, JwtTokenProvider, UserPrincipal | —                                                                                                |
| `@ExtendWith(MockitoExtension.class)` | UserService, RecipeService, CustomUserDetailsService    | `@Mock`, `@InjectMocks`                                                                          |
| `@DataJpaTest` + Testcontainers       | UserRepository                                          | `@AutoConfigureTestDatabase(replace = NONE)`, `@Import(TestcontainersConfiguration.class)`        |
| `@WebMvcTest` + Testcontainers        | AuthController, RecipeController                        | `excludeAutoConfiguration = {SecurityAutoConfiguration, UserDetailsServiceAutoConfiguration}`, `addFilters = false` + manual `SecurityContextHolder` setup in `@BeforeEach` |

- **WebMvcTest auth setup**: Inject a `UsernamePasswordAuthenticationToken` with a `UserPrincipal` into `SecurityContextHolder` before each test; clear in `@AfterEach`
- **TestcontainersConfiguration** (`@TestConfiguration`, `@ServiceConnection`) provides the PostgreSQL container — no H2 allowed
- `Application.java` doubles as the test main class (`spring-boot-testcontainers`)

## Operational

- **Virtual threads**: `spring.threads.virtual.enabled=true` — avoid `synchronized` (platform thread pinning); prefer `ReentrantLock`
- **DDL**: `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema)
- **Flyway migrations**: `src/main/resources/db/migration/`
- **Postgres**: `docker compose up -d` (port 5432, user/pass: `morsel`/`secret`, DB: `morsel`) — or just run with Maven, `spring-boot-docker-compose` auto-starts it
- `@EnableJpaAuditing` in `JpaConfig` (for `@CreationTimestamp`/`@UpdateTimestamp`)
- **`@EntityGraph`** is required on any repository method that serves response serialization (avoids N+1 with OSIV disabled)
- **Ingredient lookup** validates all requested IDs exist — throws `ResourceNotFoundException` listing missing IDs
- **Update does not call `save()`** — entity is managed inside `@Transactional`, dirty flushing happens at commit
