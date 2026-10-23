# api/AGENTS.md

Web layer: REST controllers, security, auth. Depends on `domain` for all business logic.

## Test Commands

```bash
./gradlew :api:test
./gradlew :api:test --tests "com.techinsights.api.post.PostControllerTest"
```

Tests use **Kotest `FunSpec`** (lambda body style) + **Mockk** + standalone **MockMvc** (no Spring context).

```kotlin
class MyControllerTest : FunSpec({
    val myService = mockk<MyService>()
    val mockMvc = MockMvcBuilders.standaloneSetup(MyController(myService)).build()

    test("should return 200") { ... }
})
```

## Auth & Requester

`Requester` is a sealed class resolved automatically as a controller method parameter — do not construct it manually.

- `Requester.Authenticated` — logged-in user, carries `userId: Long`
- `Requester.Anonymous` — guest, carries `anonymousId: String` (the `aid` cookie; `AidInterceptor` exposes the freshly issued aid on the first request). `identifier` is the only identity for read history, recommendations, likes and views. `ip` is for logging / rate limiting only and comes from nginx's single `X-Real-IP`.

## Cursor Pagination

Use `CursorPageResponse<T>` for cursor-based list endpoints.

## Error Contract

Every error response is RFC 7807 `application/problem+json`: `type` (`about:blank`), `title`, `status`, `detail`, `instance` (request path), plus `code` (stable machine-readable error code — switch on this), `timestamp`, and `errors` (field → message map, validation errors only).

- Domain/web errors: `GlobalExceptionHandler` → `ProblemDetails.response(...)`. Never return a bespoke error body.
- 401 / 403 from Spring Security: `ProblemDetailAuthenticationEntryPoint` / `ProblemDetailAccessDeniedHandler`. 401 `code` is `UNAUTHORIZED` (no token), `EXPIRED_TOKEN`, `INVALID_TOKEN` or `TOKEN_TAMPERED` (reason recorded by `JwtAuthenticationFilter`); 403 `code` is `FORBIDDEN`.
- nginx maintenance 503 uses the same shape (`code: SERVICE_UNAVAILABLE`).

## Like / Bookmark

Use the idempotent endpoints: `PUT …/like` sets, `DELETE …/like` unsets (same for `…/bookmark` on posts and github repos); both return the resulting state and are safe to retry. The toggle-style `POST …/like` and `POST …/bookmark` are `@Deprecated` — kept only for existing clients (a double click likes then unlikes); do not use them in new code.

## Paging

All list endpoints share one cap, `MAX_PAGE_SIZE` (100, `config/PageLimits.kt`): `Pageable` params are clamped by a `PageableHandlerMethodArgumentResolverCustomizer`, manual `size` params use `@Min(1) @Max(MAX_PAGE_SIZE)` (400 `INVALID_PARAMETER` above the cap). New list endpoints must do the same.
