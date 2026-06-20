# Graph Report - morsel  (2026-06-21)

## Corpus Check
- 133 files · ~21,713 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1120 nodes · 2526 edges · 72 communities (35 shown, 37 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 360 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e1f3fa77`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_JWT Auth & Security|JWT Auth & Security]]
- [[_COMMUNITY_Security Configuration|Security Configuration]]
- [[_COMMUNITY_Exception Handling|Exception Handling]]
- [[_COMMUNITY_User Controller Tests|User Controller Tests]]
- [[_COMMUNITY_Recipe Mapping|Recipe Mapping]]
- [[_COMMUNITY_Image Storage|Image Storage]]
- [[_COMMUNITY_Favorites Controller|Favorites Controller]]
- [[_COMMUNITY_User Repository|User Repository]]
- [[_COMMUNITY_Ratings Repository|Ratings Repository]]
- [[_COMMUNITY_User Controller|User Controller]]
- [[_COMMUNITY_Account Security Exceptions|Account Security Exceptions]]
- [[_COMMUNITY_Comment Service Tests|Comment Service Tests]]
- [[_COMMUNITY_Graphify Knowledge Graph|Graphify Knowledge Graph]]
- [[_COMMUNITY_Refresh Token Management|Refresh Token Management]]
- [[_COMMUNITY_Comment Repository|Comment Repository]]
- [[_COMMUNITY_Recipe Controller Tests|Recipe Controller Tests]]
- [[_COMMUNITY_Recipe Controller|Recipe Controller]]
- [[_COMMUNITY_Recipe Repository Tests|Recipe Repository Tests]]
- [[_COMMUNITY_Auth Controller Tests|Auth Controller Tests]]
- [[_COMMUNITY_User Mapper & Profile|User Mapper & Profile]]
- [[_COMMUNITY_Image Controller|Image Controller]]
- [[_COMMUNITY_Auth Controller|Auth Controller]]
- [[_COMMUNITY_Comment Controller|Comment Controller]]
- [[_COMMUNITY_Comment Controller Tests|Comment Controller Tests]]
- [[_COMMUNITY_Rating Controller Tests|Rating Controller Tests]]
- [[_COMMUNITY_Comment Mapping|Comment Mapping]]
- [[_COMMUNITY_Rating Controller|Rating Controller]]
- [[_COMMUNITY_OpenAPI Config|OpenAPI Config]]
- [[_COMMUNITY_Graphify Project Config|Graphify Project Config]]
- [[_COMMUNITY_Testcontainers Config|Testcontainers Config]]
- [[_COMMUNITY_Rating Mapping|Rating Mapping]]
- [[_COMMUNITY_Auth Response DTO|Auth Response DTO]]
- [[_COMMUNITY_Application Entrypoint|Application Entrypoint]]
- [[_COMMUNITY_Rating Response DTO|Rating Response DTO]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_API Paths Constants|API Paths Constants]]
- [[_COMMUNITY_App Property Keys|App Property Keys]]
- [[_COMMUNITY_Auth Constants|Auth Constants]]
- [[_COMMUNITY_Error Messages|Error Messages]]
- [[_COMMUNITY_OpenCode Plugin Config|OpenCode Plugin Config]]
- [[_COMMUNITY_Package Dependencies|Package Dependencies]]
- [[_COMMUNITY_App Configuration|App Configuration]]
- [[_COMMUNITY_CORS Properties|CORS Properties]]
- [[_COMMUNITY_JPA Configuration|JPA Configuration]]
- [[_COMMUNITY_JWT Properties|JWT Properties]]
- [[_COMMUNITY_Lockout Properties|Lockout Properties]]
- [[_COMMUNITY_Password Reset Event|Password Reset Event]]
- [[_COMMUNITY_Comment Model|Comment Model]]
- [[_COMMUNITY_Ingredient Model|Ingredient Model]]
- [[_COMMUNITY_Password Reset Token Model|Password Reset Token Model]]
- [[_COMMUNITY_Rating Model|Rating Model]]
- [[_COMMUNITY_Recipe Model|Recipe Model]]
- [[_COMMUNITY_Refresh Token Model|Refresh Token Model]]
- [[_COMMUNITY_User Model|User Model]]
- [[_COMMUNITY_Ingredient Repository|Ingredient Repository]]
- [[_COMMUNITY_Comment Request DTO|Comment Request DTO]]
- [[_COMMUNITY_Community 56|Community 56]]
- [[_COMMUNITY_Forgot Password Request DTO|Forgot Password Request DTO]]
- [[_COMMUNITY_Login Request DTO|Login Request DTO]]
- [[_COMMUNITY_Rating Request DTO|Rating Request DTO]]
- [[_COMMUNITY_Refresh Token Request DTO|Refresh Token Request DTO]]
- [[_COMMUNITY_Reset Password Request DTO|Reset Password Request DTO]]
- [[_COMMUNITY_Sign Up Request DTO|Sign Up Request DTO]]
- [[_COMMUNITY_User Status Request DTO|User Status Request DTO]]
- [[_COMMUNITY_Morsel Project Root|Morsel Project Root]]
- [[_COMMUNITY_Community 69|Community 69]]
- [[_COMMUNITY_Community 70|Community 70]]
- [[_COMMUNITY_Community 71|Community 71]]
- [[_COMMUNITY_Community 72|Community 72]]
- [[_COMMUNITY_Community 73|Community 73]]
- [[_COMMUNITY_Community 74|Community 74]]

## God Nodes (most connected - your core abstractions)
1. `Role` - 50 edges
2. `RecipeControllerTest` - 23 edges
3. `Test` - 21 edges
4. `DisplayName` - 21 edges
5. `AuthControllerTest` - 20 edges
6. `Test` - 20 edges
7. `DisplayName` - 20 edges
8. `RecipeServiceTest` - 20 edges
9. `GlobalExceptionHandlerTest` - 19 edges
10. `Test` - 19 edges

## Surprising Connections (you probably didn't know these)
- `PostgreSQL 18 Alpine` --references--> `PostgreSQL Datasource`  [INFERRED]
  compose.yaml → src/main/resources/application.yaml
- `LocalFileStorageService` --implements--> `FileStorageService`  [EXTRACTED]
  src/main/java/com/morsel/storage/LocalFileStorageService.java → src/main/java/com/morsel/storage/FileStorageService.java
- `AccountDisabledException` --inherits--> `ApplicationException`  [EXTRACTED]
  src/main/java/com/morsel/exception/AccountDisabledException.java → src/main/java/com/morsel/exception/ApplicationException.java
- `AccountLockedException` --inherits--> `ApplicationException`  [EXTRACTED]
  src/main/java/com/morsel/exception/AccountLockedException.java → src/main/java/com/morsel/exception/ApplicationException.java
- `BadRequestException` --inherits--> `ApplicationException`  [EXTRACTED]
  src/main/java/com/morsel/exception/BadRequestException.java → src/main/java/com/morsel/exception/ApplicationException.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Morsel Infrastructure Stack** — compose_postgres, resources_application_datasource, resources_application_prod_config [INFERRED 0.85]
- **Morsel Security Configuration** — resources_application_jwt_dev, resources_application_prod_jwt, resources_application_lockout_dev, resources_application_prod_lockout, resources_application_prod_cors [INFERRED 0.85]

## Communities (72 total, 37 thin omitted)

### Community 0 - "JWT Auth & Security"
Cohesion: 0.08
Nodes (27): Claims, JwtProperties, RefreshTokenClaims, RatingResponse, UserProfileResponse, JwtTokenProvider, RefreshTokenClaims, JwtTokenProviderTest (+19 more)

### Community 1 - "Security Configuration"
Cohesion: 0.08
Nodes (31): UserController, Event, CorrelationIdFilter, AuditLogger, OncePerRequestFilter, Outcome, PatchMapping, PreAuthorize (+23 more)

### Community 2 - "Exception Handling"
Cohesion: 0.10
Nodes (26): AccessDeniedException, BadCredentialsException, BindException, ConstraintViolationException, DisabledException, Exception, GlobalExceptionHandler, GlobalExceptionHandlerTest (+18 more)

### Community 3 - "User Controller Tests"
Cohesion: 0.25
Nodes (6): PiiSanitizer, UserProfileService, String, String, Transactional, UserProfileResponse

### Community 4 - "Recipe Mapping"
Cohesion: 0.09
Nodes (24): RecipeMapper, RecipeRepository, RecipeServiceTest, CreateRecipeRequest, Ingredient, List, Recipe, RecipeResponse (+16 more)

### Community 5 - "Image Storage"
Cohesion: 0.10
Nodes (18): BufferedImage, StorageProperties, ParameterizedTest, Path, PostConstruct, RuntimeException, MultipartFile, Override (+10 more)

### Community 6 - "Favorites Controller"
Cohesion: 0.10
Nodes (22): FavoriteController, FavoriteControllerTest, FavoriteService, FavoriteServiceTest, DeleteMapping, Long, PostMapping, ResponseStatus (+14 more)

### Community 7 - "User Repository"
Cohesion: 0.16
Nodes (10): UserRepository, UserRepositoryTest, Long, Modifying, Optional, Query, User, BeforeEach (+2 more)

### Community 8 - "Ratings Repository"
Cohesion: 0.10
Nodes (23): Double, Integer, RatingRepository, RatingRepositoryTest, RatingService, RatingServiceTest, EntityGraph, Long (+15 more)

### Community 9 - "User Controller"
Cohesion: 0.09
Nodes (24): Async, PasswordResetEvent, PasswordResetToken, PasswordResetTokenRepository, EmailService, EmailServiceTest, PasswordResetService, PasswordResetServiceTest (+16 more)

### Community 10 - "Account Security Exceptions"
Cohesion: 0.06
Nodes (31): AuthController, AccountDisabledException, AccountLockedException, ApplicationException, BadRequestException, DuplicateResourceException, ForbiddenException, InvalidFileException (+23 more)

### Community 11 - "Comment Service Tests"
Cohesion: 0.14
Nodes (19): CommentServiceTest, RecipeService, CreateRecipeRequest, Ingredient, List, Long, MultipartFile, Page (+11 more)

### Community 12 - "Graphify Knowledge Graph"
Cohesion: 0.13
Nodes (15): PostgreSQL Health Check, Morsel Database, pgdata Persistent Volume, PostgreSQL 18 Alpine, PostgreSQL Datasource, Default Development Configuration, File Upload Limits, JPA Schema Validation (+7 more)

### Community 13 - "Refresh Token Management"
Cohesion: 0.14
Nodes (17): PlatformTransactionManager, RefreshToken, RefreshTokenRepository, RefreshTokenRepository, RefreshTokenService, RefreshTokenServiceTest, Long, Modifying (+9 more)

### Community 14 - "Comment Repository"
Cohesion: 0.13
Nodes (18): CommentRepository, CommentRepositoryTest, CommentService, Comment, EntityGraph, Long, Page, Pageable (+10 more)

### Community 15 - "Recipe Controller Tests"
Cohesion: 0.18
Nodes (5): RecipeControllerTest, AfterEach, BeforeEach, DisplayName, Test

### Community 16 - "Recipe Controller"
Cohesion: 0.18
Nodes (18): RecipeController, CreateRecipeRequest, DeleteMapping, GetMapping, List, Long, MultipartFile, Page (+10 more)

### Community 17 - "Recipe Repository Tests"
Cohesion: 0.17
Nodes (10): RecipeRepositoryTest, RecipeSpecification, List, Long, Recipe, Specification, String, BeforeEach (+2 more)

### Community 18 - "Auth Controller Tests"
Cohesion: 0.24
Nodes (3): AuthControllerTest, DisplayName, Test

### Community 19 - "User Mapper & Profile"
Cohesion: 0.06
Nodes (35): Collection, GrantedAuthority, UserMapper, Role, JwtAuthenticationFilterTest, UserPrincipal, UserPrincipalTest, CustomUserDetailsService (+27 more)

### Community 20 - "Image Controller"
Cohesion: 0.15
Nodes (12): ImageController, ImageControllerTest, ResponseEntity, GetMapping, Resource, String, MultipartFile, Resource (+4 more)

### Community 21 - "Auth Controller"
Cohesion: 0.11
Nodes (20): AfterEach, IngredientController, IngredientControllerTest, DeleteMapping, GetMapping, PostMapping, PutMapping, ResponseStatus (+12 more)

### Community 22 - "Comment Controller"
Cohesion: 0.26
Nodes (11): CommentController, CommentRequest, CommentResponse, GetMapping, Long, Page, Pageable, PostMapping (+3 more)

### Community 23 - "Comment Controller Tests"
Cohesion: 0.31
Nodes (5): CommentControllerTest, AfterEach, BeforeEach, DisplayName, Test

### Community 24 - "Rating Controller Tests"
Cohesion: 0.32
Nodes (5): RatingControllerTest, AfterEach, BeforeEach, DisplayName, Test

### Community 25 - "Comment Mapping"
Cohesion: 0.39
Nodes (6): CommentMapper, Comment, CommentRequest, CommentResponse, Recipe, User

### Community 27 - "OpenAPI Config"
Cohesion: 0.43
Nodes (4): BuildProperties, OpenApiConfig, OpenAPI, Bean

### Community 28 - "Graphify Project Config"
Cohesion: 0.29
Nodes (8): AuthenticationConfiguration, AuthenticationManager, Bean, SecurityConfig, CorsConfigurationSource, HttpSecurity, PasswordEncoder, SecurityFilterChain

### Community 29 - "Testcontainers Config"
Cohesion: 0.53
Nodes (4): TestcontainersConfiguration, PostgreSQLContainer, ServiceConnection, Bean

### Community 30 - "Rating Mapping"
Cohesion: 0.60
Nodes (3): RatingMapper, Rating, RatingResponse

### Community 31 - "Auth Response DTO"
Cohesion: 0.40
Nodes (3): AuthResponse, Long, String

### Community 33 - "Rating Response DTO"
Cohesion: 0.32
Nodes (5): UserControllerTest, AfterEach, BeforeEach, DisplayName, Test

### Community 54 - "Ingredient Repository"
Cohesion: 0.09
Nodes (23): IngredientMapper, Optional, IngredientRepository, IngredientService, IngredientServiceTest, Ingredient, IngredientRequest, IngredientResponse (+15 more)

### Community 56 - "Community 56"
Cohesion: 0.39
Nodes (6): RatingController, Long, PutMapping, RatingRequest, RatingResponse, UserPrincipal

## Knowledge Gaps
- **86 isolated node(s):** `String`, `IngredientRequest`, `IngredientResponse`, `Long`, `String` (+81 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **37 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Role` connect `User Mapper & Profile` to `JWT Auth & Security`, `Rating Response DTO`, `Recipe Mapping`, `Favorites Controller`, `User Repository`, `Ratings Repository`, `Comment Service Tests`, `Comment Repository`, `Recipe Controller Tests`, `Recipe Repository Tests`, `Auth Controller`, `Ingredient Repository`, `Comment Controller Tests`, `Rating Controller Tests`?**
  _High betweenness centrality (0.276) - this node is a cross-community bridge._
- **Why does `HttpStatus` connect `Account Security Exceptions` to `Security Configuration`, `Exception Handling`, `Favorites Controller`, `Recipe Controller`, `Auth Controller`, `Comment Controller`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Why does `BadCredentialsException` connect `Exception Handling` to `JWT Auth & Security`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **What connects `String`, `IngredientRequest`, `IngredientResponse` to the rest of the system?**
  _86 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JWT Auth & Security` be split into smaller, more focused modules?**
  _Cohesion score 0.07919506653683869 - nodes in this community are weakly interconnected._
- **Should `Security Configuration` be split into smaller, more focused modules?**
  _Cohesion score 0.07641196013289037 - nodes in this community are weakly interconnected._
- **Should `Exception Handling` be split into smaller, more focused modules?**
  _Cohesion score 0.10056497175141244 - nodes in this community are weakly interconnected._