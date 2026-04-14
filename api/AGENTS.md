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
- `Requester.Anonymous` — guest, carries `anonymousId: String` (cookie or IP fallback)

## Cursor Pagination

Use `CursorPageResponse<T>` for cursor-based list endpoints.
