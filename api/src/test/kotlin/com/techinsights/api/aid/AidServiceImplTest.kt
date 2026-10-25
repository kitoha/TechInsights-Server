package com.techinsights.api.aid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class AidServiceImplTest : FunSpec({
  val service = AidServiceImpl(DefaultCookiePolicy())
  val props = AidProperties()

  fun requestWithAid(value: String) = MockHttpServletRequest().apply { setCookies(Cookie("aid", value)) }

  test("서버가 발급한 형식(UUID v4)의 aid 는 그대로 읽는다") {
    val aid = UUID.randomUUID().toString()

    service.read(requestWithAid(aid), props) shouldBe aid
  }

  listOf(
    "U_42",
    "U_42-8f14e45f-ceea-467a-9575-1e6c2a9a3f10",
    "aid-123",
    "8f14e45fceea467a95751e6c2a9a3f10",
    " 8f14e45f-ceea-467a-9575-1e6c2a9a3f10",
    "8f14e45f-ceea-467a-9575-1e6c2a9a3f10\nSet-Cookie: x=y",
    "8f14e45f-ceea-467a-9575-1e6c2a9a3f10".repeat(100),
    "'; DROP TABLE post_view; --"
  ).forEach { forged ->
    test("발급 형식이 아닌 aid 는 없는 것으로 취급한다: ${forged.take(30).replace("\n", "\\n")}") {
      service.read(requestWithAid(forged), props) shouldBe null
    }
  }

  test("쿠키가 없거나 비어 있으면 null") {
    service.read(MockHttpServletRequest(), props) shouldBe null
    service.read(requestWithAid(""), props) shouldBe null
  }

  test("형식이 잘못된 aid 쿠키가 오면 AidManager 가 새 aid 를 발급해 쿠키를 다시 쓴다") {
    val generator = mockk<AidGenerator>()
    val fresh = UUID.randomUUID().toString()
    every { generator.generate() } returns fresh
    val manager = AidManager(generator, service, props)
    val response = MockHttpServletResponse()

    val aid = manager.ensure(requestWithAid("U_42"), response)

    aid shouldBe fresh
    response.getHeader("Set-Cookie")!!.startsWith("aid=$fresh") shouldBe true
  }
})
