# Copilot Instructions

## Architecture

**Stack**: Spring Boot 3.5.10 (Java 21) + Angular 20.3 + MySQL 8.0 + Redis 7.0

```
lms-backend/src/main/java/com/ibizabroker/lms/
├── controller/      # REST endpoints (@RestController, @PreAuthorize)
├── service/         # Business logic (@Transactional, @RequiredArgsConstructor)
├── dao/             # Spring Data JPA repositories with @EntityGraph
├── dto/             # Request/response objects with Jakarta validation
├── entity/          # JPA entities with @BatchSize and @Version
├── configuration/   # Security, CORS, Redis, HTTP client configs
├── exceptions/      # Custom exceptions (NotFoundException, ValidationException)
├── util/            # JwtUtil, helper classes
├── validation/      # Custom validators
├── projection/      # DTO projections for complex queries
└── specification/   # JPA Criteria API specs for dynamic queries

lms-frontend/src/app/
├── services/        # HTTP clients with interceptors (auth, loading, error)
├── auth/            # Guards, interceptors, JWT handling
├── admin/           # Admin-only features (dashboard, reports, scanner)
├── chatbot/         # RAG-powered chatbot UI
├── gamification/    # Points, badges, challenges
├── models/          # TypeScript interfaces
├── shared/          # Reusable components
└── [feature]/       # Feature modules (books-list, borrow-book, etc.)
```

## Security Patterns

- **Route protection** ([WebSecurityConfiguration.java](lms-backend/src/main/java/com/ibizabroker/lms/configuration/WebSecurityConfiguration.java)):
  - `/api/auth/**`, `/api/public/**` → no auth (login, register, forgot password)
  - `/api/user/**` → `hasAnyRole("USER", "ADMIN")` (authenticated users)
  - `/api/admin/**` → `hasRole("ADMIN")` (admin-only endpoints)
  - CORS: Configured via `allowed.origins` env variable (comma-separated URLs)
- **Method-level security**: Use `@PreAuthorize` (NOT `@Secured`) — see existing controllers
  - Admin-only: `@PreAuthorize("hasRole('ADMIN')")` (NO `ROLE_` prefix in annotation)
  - User-only: `@PreAuthorize("hasRole('USER')")`
  - Any authenticated: `@PreAuthorize("isAuthenticated()")`
  - Mixed roles: `@PreAuthorize("hasAnyRole('USER','ADMIN')")`
  - **Critical**: Spring Security automatically adds `ROLE_` prefix internally; DB stores roles WITH prefix (`ROLE_USER`, `ROLE_ADMIN`)
- **JWT flow**:
  1. Login → `JwtService.generateToken()` → returns JWT + refresh token
  2. Frontend stores token → `AuthInterceptor` adds `Authorization: Bearer <token>` header
  3. Backend → `JwtRequestFilter` validates token → sets `SecurityContext`
  4. Skip JWT for public APIs: use `IS_PUBLIC_API` context in Angular (see `api.service.ts`)

- **Refresh token via HttpOnly cookie (recommended)**:
  - Backend: In auth controller set `Set-Cookie: refreshToken=<token>; HttpOnly; Secure; SameSite=Lax; Path=/api/auth/refresh; Max-Age=<ttl>` (adjust Secure for HTTPS). Remove refresh token from JSON body.
  - CORS: In [WebSecurityConfiguration.java](lms-backend/src/main/java/com/ibizabroker/lms/configuration/WebSecurityConfiguration.java) ensure `c.setAllowCredentials(true)` and allowed origins from `allowed.origins`. Keep `Authorization`, `Content-Type`, `X-Requested-With` in allowed headers.
  - Frontend: Call refresh endpoint with `withCredentials: true`; keep access token in memory/localStorage but never read refresh token from JS (cookie is HttpOnly).
  - Logout: clear cookie via `Set-Cookie: refreshToken=; HttpOnly; Secure; SameSite=Lax; Path=/api/auth/refresh; Max-Age=0`.

## Code Conventions

### Backend (Java/Spring Boot)

- **Services**:
  - Constructor injection with `@RequiredArgsConstructor` (Lombok)
  - Mark read-only methods `@Transactional(readOnly = true)` for performance
  - Class-level `@Transactional` for write operations (see [BookService.java](lms-backend/src/main/java/com/ibizabroker/lms/service/BookService.java))
- **Controllers return DTOs**, NEVER entities—prevents lazy-loading exceptions and data leaks
- **Validation**:
  - Use Jakarta annotations in DTOs: `@NotEmpty`, `@NotNull`, `@Min`, `@Max`, `@Email`, `@Size`
  - Add `@Valid` in controller method parameters to trigger validation
  - Errors caught by [ApiExceptionHandler](lms-backend/src/main/java/com/ibizabroker/lms/controller/ApiExceptionHandler.java) → returns 400 with field errors
- **Error handling**:
  - Throw custom exceptions: `NotFoundException`, `ValidationException`, `IllegalStateException`
  - `ApiExceptionHandler` converts to JSON with `ApiResponse<T>` format:
    ```json
    { "success": false, "message": "...", "errorCode": "...", "data": null }
    ```
- **N+1 query fix pattern** (CRITICAL for performance):
  - **Entities**: Add `@BatchSize(size = 20)` on `@ManyToMany`/`@OneToMany` collections
  - **Repositories**: Use `@EntityGraph(attributePaths = {"authors", "categories"})` to fetch related entities in single query
  - Example: [Books.java](lms-backend/src/main/java/com/ibizabroker/lms/entity/Books.java) + [BooksRepository.java](lms-backend/src/main/java/com/ibizabroker/lms/dao/BooksRepository.java)
- **Optimistic locking**: Use `@Version` in entities for concurrent update protection (e.g., `numberOfCopiesAvailable` in Books)
- **Vietnamese locale**: Some error messages and DTOs use Vietnamese—maintain consistency or ask user for preference

### Frontend (Angular)

- **Services**: Inject `HttpClient` + `UserAuthService` for API calls
- **Interceptors** (executed in order):
  1. `LoadingInterceptor` → shows/hides loading spinner automatically
  2. `AuthInterceptor` → adds JWT token to requests (skips public APIs)
  3. `ErrorInterceptor` → catches errors, shows toastr, redirects on 401/403
- **API calls**: Use `ApiService.getApiUrl('endpoint')` for centralized URL management
- **Route guards**: `AuthGuard` checks JWT token; `AdminGuard` checks `ROLE_ADMIN`

## Build & Run

```bash
# Backend (requires MySQL on 3306/3307, optional Redis on 6379)
cd lms-backend && mvn spring-boot:run -Dspring.profiles.active=dev

# Frontend (dev server on :4200)
cd lms-frontend && npm install && npm start

# Full stack via Docker (recommended for first-time setup)
docker-compose up --build  # backend:8081, frontend:80, MySQL:3308, Redis:6379

# Simple Docker setup (no Redis, basic config)
docker-compose -f docker-compose.simple.yml up --build

# Tests
cd lms-backend && mvn test  # Requires running MySQL
cd lms-frontend && npm test  # Runs Karma/Jasmine tests
```

## Environment Variables

| Variable                 | Default                              | Purpose                                      |
| ------------------------ | ------------------------------------ | -------------------------------------------- |
| `DB_URL`                 | `jdbc:mysql://localhost:3306/lms_db` | MySQL connection string                      |
| `DB_USERNAME/PASSWORD`   | root/123456                          | MySQL credentials                            |
| `APP_JWT_SECRET`         | dev fallback                         | JWT signing key (32+ bytes, CHANGE IN PROD)  |
| `GEMINI_API_KEY`         | none                                 | Google Gemini API for RAG chatbot (required) |
| `REDIS_HOST/PORT`        | localhost:6379                       | Rate limiting & caching                      |
| `MAIL_USERNAME/PASSWORD` | none                                 | SMTP credentials for password reset emails   |
| `PINECONE_API_KEY`       | none                                 | Vector database for chatbot semantic search  |
| `PINECONE_INDEX_URL`     | none                                 | Pinecone index endpoint                      |
| `SPRING_PROFILES_ACTIVE` | none                                 | Set to `dev` for development mode            |
| `allowed.origins`        | http://localhost:4200                | CORS allowed origins (comma-separated)       |

**Configuration files**:

- `application.properties` - Base config (prod-safe defaults)
- `application-dev.properties` - Dev overrides (verbose logging, local DB)
- `.env.example` - Template for local env variables (git-ignored)

## Key Files & Patterns

| Purpose                    | File                                                                | Notes                                                 |
| -------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------- |
| Security config            | `configuration/WebSecurityConfiguration.java`                       | CORS, JWT filter chain, auth provider                 |
| JWT generation/validation  | `service/JwtService.java`, `util/JwtUtil.java`                      | Token generation, validation, user extraction         |
| Global error handler       | `controller/ApiExceptionHandler.java`                               | Converts exceptions to JSON responses                 |
| Chatbot flow (RAG)         | `controller/ChatbotController.java` → `service/RagService.java`     | Vector search + Gemini API, streaming with SSE        |
| Entity example (N+1 fixed) | `entity/Books.java`, `dao/BooksRepository.java`                     | @BatchSize + @EntityGraph pattern                     |
| DTO with validation        | `dto/BookCreateDto.java`, `dto/BookUpdateDto.java`                  | Jakarta validation annotations                        |
| Frontend API base          | `environments/environment.ts` (`apiBaseUrl`)                        | Single source of truth for API URLs                   |
| Frontend JWT interceptor   | `auth/auth.interceptor.ts`                                          | Adds Bearer token, skips public APIs                  |
| Frontend error handling    | `auth/error.interceptor.ts`                                         | Global error handling, auto-redirect on auth failures |
| Frontend loading state     | `services/loading.service.ts`, `auth/loading.interceptor.ts`        | Automatic loading spinner for HTTP requests           |
| Gamification logic         | `service/GamificationService.java`, `gamification/` (frontend)      | Points, badges, challenges                            |
| Admin dashboard            | `controller/AdminDashboardController.java`, `admin/dashboard/` (UI) | System stats, charts, analytics                       |

## Current Risks / Cons (fix soon)

- **CirculationService god class**: Handles borrow/return/renew/fine/inventory in one service → hard to test/maintain; plan to split per domain.
- **N+1 risk in projections/DTOs**: When listing books with authors/categories, ensure `@EntityGraph` or join fetch is applied; otherwise Hibernate will loop per row.
- **Refresh token in JSON body**: `JwtResponse` returns refresh token; prefer HttpOnly cookie to avoid XSS token theft.
- **Transactional boundaries**: Complex flows (return book → clear fines → award points → log) must be under coherent `@Transactional`; decide rollback rules for partial failures.
- **DB search indexes**: Add MySQL indexes on books.name/books.isbn/published_year to speed search/sort.

## Roadmap (actionable)

1. **Refactor & clean**: Split CirculationService into LoanService (borrow/return), FineService (fees), ReservationService; standardize API responses to `ApiResponse<T>`; adopt MapStruct for entity→DTO mapping.
2. **Security & performance**: Move refresh token to HttpOnly cookie; audit MySQL indexes on search-heavy columns (title, author, isbn); keep `@EntityGraph`/`@BatchSize` on list endpoints.
3. **Advanced features**: Add AI recommendations (borrow history + book vectors); integrate VNPay/MoMo sandbox for online fine payments.

## Common Tasks

- **Add new endpoint**:
  1. Create DTO in `dto/` with validation annotations
  2. Add service method in `service/` (use `@Transactional(readOnly = true)` for reads)
  3. Create controller method with `@PreAuthorize` for security
  4. Return `ResponseEntity<ApiResponse<T>>` with DTO (never entity)
- **Add admin-only endpoint**: Use `@PreAuthorize("hasRole('ADMIN')")` at method or class level
- **Add new entity**:
  1. Create entity in `entity/` (consider `@Version` for locking, `@BatchSize` for collections)
  2. Create repository in `dao/` (use `@EntityGraph` to avoid N+1)
  3. Create DTOs for request/response
  4. Add service layer, then controller
- **Run tests**: `cd lms-backend && mvn test` (requires running MySQL)
- **Debug auth issues**:
  - Check `JwtRequestFilter` logs for token validation failures
  - Verify token format: `Authorization: Bearer <jwt>`
  - Check role names in DB (must be `ROLE_USER`, `ROLE_ADMIN`)
  - Verify `@PreAuthorize` uses NO `ROLE_` prefix
- **Add chatbot context**: Update `RagService.retrieveContext()` to include new data sources
- **Performance issues**:
  - Check for N+1 queries in logs (set `spring.jpa.show-sql=true`)
  - Add `@EntityGraph` or `@BatchSize` to affected entities/repositories
  - Consider caching with `@Cacheable` for frequently accessed data
