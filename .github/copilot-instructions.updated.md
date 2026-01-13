# Copilot Instructions (concise)

Purpose: Give an AI coding agent the minimum, high-value facts to be productive in this repo.

## Architecture (big picture)

- Backend: Spring Boot 3.x (Java 21) monolith layout in `lms-backend/src/main/java/com/ibizabroker/lms/` with the common layering: `controller/` → `service/` → `dao/` (Spring Data JPA) → `entity/`.
- Frontend: Angular 14 SPA in `lms-frontend/src/app/` (feature modules; `auth/` for JWT & guards; `services/` for HTTP clients).
- Infra: MySQL primary DB + Redis for caching/rate-limiting. A `docker-compose.yml` is available for full-stack local runs.

## Quick start (commands)

- Backend dev: `cd lms-backend && mvn spring-boot:run -Dspring.profiles.active=dev` (uses `application-dev.properties`).
- Frontend dev: `cd lms-frontend && npm install && npm start` (dev server on :4200).
- Full stack: `docker-compose up --build` (backend:8081, frontend:80, MySQL:3308, Redis:6379).

## Key files & where to look first

- Security: `configuration/WebSecurityConfiguration.java`, `util/JwtUtil.java`, `security/JwtRequestFilter`.
- Error handling: `controller/ApiExceptionHandler.java` — controllers return DTOs, not entities.
- Chatbot/RAG: `controller/ChatbotController.java` → `service/RagService.java` (requires `GEMINI_API_KEY`).
- N+1 pattern: `entity/Books.java`, `dao/BooksRepository.java` (look for `@EntityGraph` / `@BatchSize`).
- Frontend integration: `src/environments/environment.ts` (`apiBaseUrl`) and `auth/auth.interceptor.ts`.

## Project conventions (must-follow)

- Constructor injection with Lombok: use `@RequiredArgsConstructor` on services/components.
- Mark pure-read methods `@Transactional(readOnly = true)`.
- Use `@PreAuthorize` for method-level security; prefer the patterns already present (watch for `ROLE_` prefixes).
- Controllers return DTOs (in `dto/`) and use Jakarta validation annotations (`@NotEmpty`, `@Min`, etc.).

## Integrations & env vars

- `DB_URL` default: `jdbc:mysql://localhost:3307/lms_db` (MySQL). Tests/integration expect a running DB.
- `REDIS_HOST`/`REDIS_PORT` used for caching/rate-limiting.
- `GEMINI_API_KEY` required to enable chatbot features in `RagService`.
- Mail: `MAIL_USERNAME`/`MAIL_PASSWORD` used for password reset flows.

## Testing & debugging

- Run backend tests: `cd lms-backend && mvn test` (some tests require DB).
- To debug auth: replicate failing request, inspect `JwtRequestFilter` + `ApiExceptionHandler` logs; verify role strings.

## Editing guidance for agents

- Adding endpoints: follow controller → service → dao → dto flow and add validation in DTOs.
- Avoid returning JPA entities from controllers; map to DTOs to prevent lazy-loading leaks.
- For performance fixes, prefer repository-side fetch tuning (`@EntityGraph`, pagination, projection).

## Files to open for onboarding

- [lms-backend/src/main/java/com/ibizabroker/lms/configuration/WebSecurityConfiguration.java](lms-backend/src/main/java/com/ibizabroker/lms/configuration/WebSecurityConfiguration.java)
- [lms-backend/src/main/java/com/ibizabroker/lms/controller/ApiExceptionHandler.java](lms-backend/src/main/java/com/ibizabroker/lms/controller/ApiExceptionHandler.java)
- [lms-backend/src/main/java/com/ibizabroker/lms/controller/ChatbotController.java](lms-backend/src/main/java/com/ibizabroker/lms/controller/ChatbotController.java)
- [lms-frontend/src/app/auth/auth.interceptor.ts](lms-frontend/src/app/auth/auth.interceptor.ts)

If any area needs more examples (DTO shape, typical service method, sample test), tell me which and I will add 1–2 concrete snippets.
