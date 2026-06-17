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

| Concern       | Choice                                            |
|---------------|---------------------------------------------------|
| Web           | Spring Web MVC                                    |
| DB            | PostgreSQL + Flyway + Spring Data JPA             |
| Security      | Spring Security + JWT (jjwt 0.12.6, HMAC-SHA)     |
| Validation    | `@Valid` + `jakarta.validation`                   |
| Observability | Actuator + Micrometer Prometheus                  |
| File Storage  | Local filesystem (interface `FileStorageService`) |
| Format        | Spotless + Palantir JavaFormat                    |
| Codegen       | Lombok + `spring-boot-configuration-processor`    |
| Tests         | Testcontainers, MockMvc, Mockito                  |

## Architecture

Single-module Maven project under `com.morsel`:

```
com.morsel
├── Application.java                 # @SpringBootApplication + @ConfigurationPropertiesScan
├── config/                          # SecurityConfig, JpaConfig, JwtProperties, StorageProperties (records)
├── controller/                      # AuthController, RecipeController, ImageController, CommentController, RatingController
├── dto/request/                     # SignUpRequest, LoginRequest, CreateRecipeRequest, UpdateRecipeRequest, CommentRequest, RatingRequest (records)
├── dto/response/                    # AuthResponse, RecipeResponse, CommentResponse, RatingResponse (records with static of() factory)
├── exception/                       # sealed: ApplicationException ← DuplicateResourceException, ForbiddenException, InvalidFileException, ResourceNotFoundException
├── mapper/                          # UserMapper, RecipeMapper, CommentMapper, RatingMapper (@Component)
├── model/                           # User, Recipe, Ingredient, Comment, Rating + Role enum
├── repository/                      # JpaRepository interfaces
├── security/                        # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal (record implements UserDetails)
├── controller/                      # AuthController, RecipeController, ImageController, CommentController, RatingController, UserController, FavoriteController
├── service/                         # UserService, CustomUserDetailsService, RecipeService, CommentService, RatingService, FavoriteService
├── specification/                   # RecipeSpecification (JPA Criteria API static factories)
└── storage/                         # FileStorageService (interface), LocalFileStorageService
```

**API endpoints** (16 total):

| Action                   | Path                                       | Auth          | Query params                               |
|--------------------------|--------------------------------------------|---------------|--------------------------------------------|
| Sign up                  | `POST /api/v1/auth/signup`                 | permitAll     | —                                          |
| Sign in                  | `POST /api/v1/auth/signin`                 | permitAll     | —                                          |
| Create recipe            | `POST /api/v1/recipes`                     | authenticated | —                                          |
| List recipes (paginated) | `GET /api/v1/recipes`                      | authenticated | `keyword`, `ingredients` (comma-separated) |
| Get recipe by id         | `GET /api/v1/recipes/{id}`                 | authenticated | —                                          |
| Update recipe (owner)    | `PUT /api/v1/recipes/{id}`                 | owner check   | —                                          |
| Upload recipe image      | `POST /api/v1/recipes/{id}/image`          | owner check   | —                                          |
| Delete recipe (admin)    | `DELETE /api/v1/recipes/{id}`              | admin only    | —                                          |
| Add comment              | `POST /api/v1/recipes/{recipeId}/comments` | authenticated | —                                          |
| List comments            | `GET /api/v1/recipes/{recipeId}/comments`  | authenticated | —                                          |
| Add/update rating        | `POST /api/v1/recipes/{recipeId}/ratings`  | authenticated | —                                          |
| Serve stored image       | `GET /api/v1/images/{filename}`            | permitAll     | —                                          |
| Add favorite             | `POST /api/v1/recipes/{id}/favorite`       | authenticated | —                                          |
| Remove favorite          | `DELETE /api/v1/recipes/{id}/favorite`     | authenticated | —                                          |
| Get my favorites         | `GET /api/v1/users/me/favorites`           | authenticated | —                                          |
| Get user profile         | `GET /api/v1/users/{username}`             | authenticated | —                                          |

**Entity model**:

- `User` is the root aggregate with cascade ALL + orphanRemoval on its `@OneToMany` collections (recipes, comments,
  ratings)
- `Recipe` also cascades to comments/ratings (dual ownership)
- `Recipe` ↔ `Ingredient` is a bidirectional `@ManyToMany` via `recipe_ingredients` join table
- `User` ↔ `Recipe` is a unidirectional `@ManyToMany` via `user_favorite_recipes` join table (`user.favorites`)
- All `@ManyToOne` are `FetchType.LAZY`; OSIV is disabled (`open-in-view=false`)
- Lazy collections require `@Transactional` or `@EntityGraph` to avoid `LazyInitializationException`
- `RecipeRepository` extends `JpaSpecificationExecutor<Recipe>` with `@EntityGraph` on its `findAll` overrides
- `RecipeSpecification` provides static factories for keyword search (LIKE on `title`/`description`) and ingredient
  filter (JOIN + IN, ANY semantics) via JPA Criteria API

## Conventions

- **Constructor injection** (`@RequiredArgsConstructor`), no `@Autowired` on fields
- **Java Records** for DTOs and `@ConfigurationProperties` (not `@Data`); entities use Lombok `@Builder`
- **Static factory methods** (`of(...)`) on response records
- **Sealed exception hierarchy** extending `sealed abstract class ApplicationException`
- **ProblemDetail** (RFC 7807) for error responses (`spring.mvc.problemdetails.enabled=true`)
- Package names are singular (`model`, `repository`, `service`)
- Test method names: `snake_case` with `@DisplayName` on class and each method
- **Separate DTOs for creation vs update** — never reuse a single request type for both

## Security / JWT

- `POST /api/v1/auth/**` and `GET /api/v1/images/**` are `permitAll`; all other endpoints require authentication
- CSRF disabled, CORS widely open (`*`), sessions STATELESS, `@EnableMethodSecurity` enabled
- JWT in `Authorization: Bearer <token>` header; HMAC-SHA via jjwt
- `app.jwt.secret` / env `JWT_SECRET` (base64), default dev key in `application.yaml`
- `app.jwt.expiration-ms` / env `JWT_EXPIRATION_MS` (default 24h)
- `CustomUserDetailsService.loadUserByUsername()` accepts username **or** email (tries username first)

## File Storage

- Interface `FileStorageService` with `store(MultipartFile)` and `load(String filename)` methods
- `LocalFileStorageService` saves to configurable directory (`app.storage.upload-dir`, default `uploads/`)
- Allowed extensions: jpg, jpeg, png, GIF, webp, svg (validated server-side)
- Filenames are UUIDs with original extension; returned URL path is `/api/v1/images/{uuid}.{ext}`
- Image serving (`GET /api/v1/images/{filename}`) is public (permitAll)
- Multipart limits: 5MB per file, 10MB per request (`spring.servlet.multipart`)

## Tests (112 total)

Four styles, all under `src/test/java/com/morsel/`:

| Style                                 | Used for                                                                               | Key setup                                                                                                                                                                   |
|---------------------------------------|----------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Plain JUnit 5                         | GlobalExceptionHandler, JwtTokenProvider, UserPrincipal, LocalFileStorageService       | —                                                                                                                                                                           |
| `@ExtendWith(MockitoExtension.class)` | UserService, RecipeService, CustomUserDetailsService, CommentService, RatingService    | `@Mock`, `@InjectMocks`                                                                                                                                                     |
| `@DataJpaTest` + Testcontainers       | UserRepository                                                                         | `@AutoConfigureTestDatabase(replace = NONE)`, `@Import(TestcontainersConfiguration.class)`                                                                                  |
| `@WebMvcTest` + Testcontainers        | AuthController, RecipeController, ImageController, CommentController, RatingController | `excludeAutoConfiguration = {SecurityAutoConfiguration, UserDetailsServiceAutoConfiguration}`, `addFilters = false` + manual `SecurityContextHolder` setup in `@BeforeEach` |

- **WebMvcTest auth setup**: Inject a `UsernamePasswordAuthenticationToken` with a `UserPrincipal` into
  `SecurityContextHolder` before each test; clear in `@AfterEach`
- **TestcontainersConfiguration** (`@TestConfiguration`, `@ServiceConnection`) provides the PostgreSQL container — no H2
  allowed
- Testcontainers auto-detects `Application.java` as the `@SpringBootApplication` from the classpath

## Operational

- **Virtual threads**: `spring.threads.virtual.enabled=true` — avoid `synchronized` (platform thread pinning); prefer
  `ReentrantLock`
- **DDL**: `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema)
- **Flyway migrations**: `src/main/resources/db/migration/`
- **Postgres**: `docker compose up -d` (port 5432, user/pass: `morsel`/`secret`, DB: `morsel`) — or just run with Maven,
  `spring-boot-docker-compose` auto-starts it
- `@EnableJpaAuditing` in `JpaConfig` (for `@CreationTimestamp`/`@UpdateTimestamp`)
- **`@EntityGraph`** is required on any repository method that serves response serialization (avoids N+1 with OSIV
  disabled)
- **`RecipeRepository`** extends `JpaSpecificationExecutor` — the overridden `findAll(Specification, Pageable)` must
  also
  carry `@EntityGraph`
- **`Specification.where(null)` is ambiguous** in Spring Data JPA 3.x — both `Specification<T>` and
  `PredicateSpecification<T>` overloads match. Use `(root, query, cb) -> cb.conjunction()` as the match-all starting
  point instead
- **LIKE wildcards must be escaped** in `RecipeSpecification.withKeyword()` — escape `_` and `%` with a custom escape
  character (`!`) to prevent unexpected pattern matching
- **Ingredient lookup** validates all requested IDs exist — throws `ResourceNotFoundException` listing missing IDs
- **Update does not call `save()`** — entity is managed inside `@Transactional`, dirty flushing happens at commit
- **Rating upsert** uses native PostgreSQL `INSERT ... ON CONFLICT DO UPDATE` for atomic one-rating-per-user — no
  find-then-create race
- **Rating aggregates** use `AVG(score)` / `COUNT(*)` queries rather than loading all entities in memory
