package com.techinsights.api.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import com.fasterxml.jackson.databind.json.JsonMapper

class ProblemDetailSecurityHandlersTest : FunSpec({
    val mapper = JsonMapper.builder().build()
    val entryPoint = ProblemDetailAuthenticationEntryPoint()

    fun unauthorized(code: String?): Map<*, *> {
        val request = MockHttpServletRequest("GET", "/api/v1/users/me")
        code?.let { request.setAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE_ATTRIBUTE, it) }
        val response = MockHttpServletResponse()

        entryPoint.commence(request, response, BadCredentialsException("x"))

        response.status shouldBe 401
        response.contentType shouldStartWith "application/problem+json"
        return mapper.readValue(response.contentAsString, Map::class.java)
    }

    test("토큰이 없으면 401 problem+json, code=UNAUTHORIZED") {
        val body = unauthorized(null)

        body["status"] shouldBe 401
        body["title"] shouldBe "Unauthorized"
        body["code"] shouldBe "UNAUTHORIZED"
        body["instance"] shouldBe "/api/v1/users/me"
        (body["timestamp"] != null) shouldBe true
    }

    test("토큰이 만료되면 code=EXPIRED_TOKEN") {
        unauthorized("EXPIRED_TOKEN")["code"] shouldBe "EXPIRED_TOKEN"
    }

    test("토큰이 변조되면 해당 사유 코드를 그대로 내려준다") {
        unauthorized("TOKEN_TAMPERED")["code"] shouldBe "TOKEN_TAMPERED"
    }

    test("권한이 없으면 403 problem+json, code=FORBIDDEN") {
        val request = MockHttpServletRequest("DELETE", "/api/v1/companies/1")
        val response = MockHttpServletResponse()

        ProblemDetailAccessDeniedHandler().handle(request, response, AccessDeniedException("denied"))

        response.status shouldBe 403
        response.contentType shouldStartWith "application/problem+json"
        val body = mapper.readValue(response.contentAsString, Map::class.java)
        body["status"] shouldBe 403
        body["code"] shouldBe "FORBIDDEN"
    }
})
