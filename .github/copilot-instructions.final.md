# Copilot Instructions (concise)

Purpose: Give an AI coding agent the minimum, high-value facts to be productive in this repo.

## Architecture (big picture)

- Backend: Spring Boot 3.x (Java 21) monolith layout in `lms-backend/src/main/java/com/ibizabroker/lms/` with the common layering: `controller/` → `service/` → `dao/` (Spring Data JPA) → `entity/`.
- Frontend: Angular 14 SPA in `lms-frontend/src/app/` (feature modules; `auth/` for JWT & guards; `services/` for HTTP clients).
- Infra: MySQL primary DB + Redis for caching/rate-limiting. A `docker-compose.yml` is available for full-stack local runs.

## Quick start (commands)

- Backend dev:

```bash
cd lms-backend && mvn spring-boot:run -Dspring.profiles.active=dev
```

- Frontend dev:

```bash
cd lms-frontend && npm install && npm start
```

- Full stack:

```bash
docker-compose up --build
```

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

## Chatbot integration (practical details)

- Chat flows are implemented in `RagService` and called by `ChatbotController`. The AI key is read from environment/config; set `GEMINI_API_KEY` in `application-dev.properties` or your shell.
- To test the RAG endpoint locally (once backend runs):

```bash
curl -X POST "http://localhost:8081/api/chatbot/query" \
  -H "Content-Type: application/json" \
  -d '{"query":"How do I borrow a book?"}'
```

- If the chatbot fails, check logs from `RagService` for outbound requests and ensure `GEMINI_API_KEY` is present and valid. Also verify network/firewall and that any blocking proxies are bypassed.

## DTO & service examples (copyable patterns)

- Typical DTO (validation + mapping):

```java
// lms-backend/src/main/java/com/ibizabroker/lms/dto/BookRequest.java
public record BookRequest(
    @NotBlank String title,
    @NotBlank String author,
    @Min(1) int pages
) {}
```

- Typical service method pattern (transactional + mapping):

```java
@Service
@RequiredArgsConstructor
public class BookService {
  private final BooksRepository repo;

  @Transactional(readOnly = true)
  public BookResponse findById(Long id) {
    var book = repo.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
    return BookResponse.fromEntity(book);
  }
}
```

## Testing & debugging

- Run backend tests: `cd lms-backend && mvn test` (some tests require DB).
- **Common: MySQL connection refused** — tests expect MySQL on port 3307 (see `DB_URL`). Quick fix:
  - Start MySQL: `docker run -d --name mysql-lms -p 3307:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=lms_db mysql:8`
  - Or skip tests: `mvn clean install -DskipTests`
- **H2 test profile**: `src/test/resources/application.properties` uses H2 in-memory DB, but some queries (e.g., `DATEDIFF`) are MySQL-specific and will fail. Either fix queries or run integration tests against MySQL.
- To debug auth: reproduce failing request, inspect `JwtRequestFilter` + `ApiExceptionHandler` logs; verify role strings.

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
