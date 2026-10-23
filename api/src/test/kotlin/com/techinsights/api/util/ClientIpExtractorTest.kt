package com.techinsights.api.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest

class ClientIpExtractorTest : FunSpec({

  test("nginx가 덮어쓴 X-Real-IP 값을 사용한다") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Real-IP") } returns "203.0.113.1"

    ClientIpExtractor.extract(request) shouldBe "203.0.113.1"
  }

  test("X-Real-IP에 공백이 있어도 trim 처리") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Real-IP") } returns "  203.0.113.1  "

    ClientIpExtractor.extract(request) shouldBe "203.0.113.1"
  }

  test("클라이언트가 보낸 X-Forwarded-For는 신뢰하지 않는다") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns "6.6.6.6, 7.7.7.7"
    every { request.getHeader("X-Real-IP") } returns null
    every { request.remoteAddr } returns "192.168.1.100"

    ClientIpExtractor.extract(request) shouldBe "192.168.1.100"
  }

  test("X-Real-IP가 빈 문자열이면 remoteAddr 사용") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Real-IP") } returns ""
    every { request.remoteAddr } returns "127.0.0.1"

    ClientIpExtractor.extract(request) shouldBe "127.0.0.1"
  }

  test("X-Real-IP가 공백만 있으면 remoteAddr 사용") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Real-IP") } returns "   "
    every { request.remoteAddr } returns "127.0.0.1"

    ClientIpExtractor.extract(request) shouldBe "127.0.0.1"
  }

  test("프록시 헤더가 없으면 remoteAddr 사용") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Real-IP") } returns null
    every { request.remoteAddr } returns "192.168.1.100"

    ClientIpExtractor.extract(request) shouldBe "192.168.1.100"
  }

  test("IPv6 주소 처리") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Real-IP") } returns "2001:0db8:85a3:0000:0000:8a2e:0370:7334"

    ClientIpExtractor.extract(request) shouldBe "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
  }
})
