```markdown
# Copilot Instructions — repo-specific quick facts

Purpose: give an AI coding agent the minimal, high-value facts to be productive in this repository.

## Big picture

- Backend: Spring Boot (Java 21) monolith in `lms-backend/src/main/java/com/ibizabroker/lms/` with layered layout: `controller/` → `service/` → `dao/` (Spring Data JPA) → `entity/`.
- Frontend: Angular 14 SPA in `lms-frontend/src/app/` (feature modules; `auth/` handles JWT + guards; `services/` contains HTTP clients).
- Infra: MySQL primary DB + Redis for cache/rate-limiting. `docker-compose.yml` boots a full stack (useful for local integration tests).

## Quick start (exact commands)

- Backend (dev profile):
  - Unix/mac: `./mvnw spring-boot:run -Dspring.profiles.active=dev`
  - Windows: `mvnw.cmd spring-boot:run -Dspring.profiles.active=dev` or run `start-backend.bat` in repo root.
- Frontend (dev): `cd lms-frontend && pnpm install && pnpm start` (project includes `pnpm` lockfile; `npm install` also works).
- Full stack (Docker): `docker-compose up --build` — expected ports: backend 8081, frontend 80, MySQL 3308, Redis 6379.

## Critical patterns & examples (search these first)

- Security & auth: `configuration/WebSecurityConfiguration.java`, `security/JwtRequestFilter` and `util/JwtUtil.java` — tokens are `Authorization: Bearer <token>` and role strings may appear with/without `ROLE_` prefix.
- Error handling: `controller/ApiExceptionHandler.java` — controllers return DTOs (see `dto/`) not JPA entities.
- Chatbot / RAG: `controller/ChatbotController.java` → `service/RagService.java` — requires `GEMINI_API_KEY` to enable chatbot features.
- N+1 / performance: inspect `entity/Books.java` and `dao/BooksRepository.java` for `@EntityGraph` / `@BatchSize` examples used to fix N+1.

## Project conventions (concrete rules)

- Constructor injection with Lombok: use `@RequiredArgsConstructor` on services/controllers.
- Mark read-only service methods `@Transactional(readOnly = true)`.
- Use `@PreAuthorize` for method-level security (avoid `@Secured`); prefer existing role checks in controllers.
- DTO-first controllers: validate request DTOs with Jakarta annotations (`@NotEmpty`, `@Min`, etc.) and map entities → DTOs in service layer.

## Environment variables & external deps

- `DB_URL` default: `jdbc:mysql://localhost:3307/lms_db` (MySQL). Tests and some flows expect a running DB instance.
- `REDIS_HOST` / `REDIS_PORT` used for caching and rate-limiting.
- `GEMINI_API_KEY` must be set to enable RAG/chatbot features used by `RagService`.
- `MAIL_USERNAME` / `MAIL_PASSWORD` used by password reset flows.

## Build, test & debug notes

- Run backend tests: `cd lms-backend && ./mvnw test` (integration-like tests may require DB).
- To debug auth: reproduce request, inspect `JwtRequestFilter` traces and `ApiExceptionHandler` output; verify role name formatting.
- Use `application-dev.properties` for local overrides; activate with `SPRING_PROFILES_ACTIVE=dev`.

## Where an agent should look first (onboarding files)

- [lms-backend/src/main/java/com/ibizabroker/lms/configuration/WebSecurityConfiguration.java](lms-backend/src/main/java/com/ibizabroker/lms/configuration/WebSecurityConfiguration.java)
- [lms-backend/src/main/java/com/ibizabroker/lms/controller/ApiExceptionHandler.java](lms-backend/src/main/java/com/ibizabroker/lms/controller/ApiExceptionHandler.java)
- [lms-backend/src/main/java/com/ibizabroker/lms/controller/ChatbotController.java](lms-backend/src/main/java/com/ibizabroker/lms/controller/ChatbotController.java)
- [lms-backend/src/main/java/com/ibizabroker/lms/service/RagService.java](lms-backend/src/main/java/com/ibizabroker/lms/service/RagService.java)
- [lms-backend/src/main/java/com/ibizabroker/lms/entity/Books.java](lms-backend/src/main/java/com/ibizabroker/lms/entity/Books.java)
- [lms-frontend/src/app/auth/auth.interceptor.ts](lms-frontend/src/app/auth/auth.interceptor.ts)

## Editing guidance for agents (do this, not that)

- Add endpoint: create controller method → service method → repository change (if needed) → new request/response DTOs in `dto/`.
- Never return entities from controllers; always map to DTOs to avoid lazy-loading and leakage.
- For perf issues prefer repository-side fixes (`@EntityGraph`, projections) before heavy client-side joins.

If you want, I can expand with 1–2 concrete DTO examples, a sample service method, or common test stubs.
```
