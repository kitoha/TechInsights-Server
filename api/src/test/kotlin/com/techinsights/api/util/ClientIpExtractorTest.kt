package com.techinsights.api.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest

class ClientIpExtractorTest : FunSpec({

  test("X-Forwarded-For 헤더가 있을 때 첫 번째 IP 반환") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns "203.0.113.1"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "203.0.113.1"
  }

  test("X-Forwarded-For에 여러 IP가 있을 때 첫 번째 IP 반환") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns "203.0.113.1, 198.51.100.178, 192.0.2.1"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "203.0.113.1"
  }

  test("X-Forwarded-For에 공백이 있어도 trim 처리") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns "  203.0.113.1  , 198.51.100.178"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "203.0.113.1"
  }

  test("X-Forwarded-For가 null이면 X-Real-IP 사용") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns null
    every { request.getHeader("X-Real-IP") } returns "198.51.100.178"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "198.51.100.178"
  }

  test("X-Forwarded-For가 빈 문자열이면 X-Real-IP 사용") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns ""
    every { request.getHeader("X-Real-IP") } returns "198.51.100.178"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "198.51.100.178"
  }

  test("X-Forwarded-For가 공백만 있으면 X-Real-IP 사용") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns "   "
    every { request.getHeader("X-Real-IP") } returns "198.51.100.178"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "198.51.100.178"
  }

  test("모든 헤더가 없으면 remoteAddr 사용") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns null
    every { request.getHeader("X-Real-IP") } returns null
    every { request.remoteAddr } returns "127.0.0.1"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "127.0.0.1"
  }

  test("X-Real-IP가 빈 문자열이면 remoteAddr 사용") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns null
    every { request.getHeader("X-Real-IP") } returns ""
    every { request.remoteAddr } returns "127.0.0.1"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "127.0.0.1"
  }

  test("실제 Cloudflare 환경 시뮬레이션 - X-Forwarded-For") {
    // given (Cloudflare는 X-Forwarded-For 사용)
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns "104.28.20.100, 172.68.1.1"
    every { request.getHeader("X-Real-IP") } returns "172.68.1.1"
    every { request.remoteAddr } returns "172.68.1.1"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "104.28.20.100" // 실제 클라이언트 IP
  }

  test("직접 접속 시나리오 - 프록시 없음") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns null
    every { request.getHeader("X-Real-IP") } returns null
    every { request.remoteAddr } returns "192.168.1.100"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "192.168.1.100"
  }

  test("IPv6 주소 처리") {
    // given
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns "2001:0db8:85a3:0000:0000:8a2e:0370:7334"

    // when
    val result = ClientIpExtractor.extract(request)

    // then
    result shouldBe "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
  }
})
