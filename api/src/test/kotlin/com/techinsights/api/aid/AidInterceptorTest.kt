package com.techinsights.api.aid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AidInterceptorTest : FunSpec({
  val manager = mockk<AidManager>()
  val interceptor = AidInterceptor(manager)

  beforeTest { clearMocks(manager) }

  test("aid를 확보하면 같은 요청에서 읽을 수 있도록 request attribute에 저장한다") {
    val request = MockHttpServletRequest("POST", "/api/v1/posts/1/view")
    val response = MockHttpServletResponse()
    every { manager.ensure(request, response) } returns "new-aid"

    interceptor.preHandle(request, response, Any()) shouldBe true

    request.getAttribute(AidInterceptor.AID_ATTRIBUTE) shouldBe "new-aid"
  }

  test("봇 요청은 aid를 발급하지 않는다") {
    val request = MockHttpServletRequest("GET", "/api/v1/posts").apply { addHeader("User-Agent", "Googlebot/2.1") }
    val response = MockHttpServletResponse()

    interceptor.preHandle(request, response, Any()) shouldBe true

    request.getAttribute(AidInterceptor.AID_ATTRIBUTE) shouldBe null
    verify(exactly = 0) { manager.ensure(any(), any()) }
  }
})
