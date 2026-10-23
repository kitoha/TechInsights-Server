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
