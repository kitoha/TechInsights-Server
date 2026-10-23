package com.techinsights.api.auth

import com.techinsights.api.config.CorsProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class CsrfOriginFilterTest : FunSpec({
    val filter = CsrfOriginFilter(
        CorsProperties(allowedOrigins = listOf("https://www.techinsights.shop", "https://techinsights.shop"))
    )

    fun run(
        method: String,
        origin: String? = null,
        referer: String? = null
    ): Pair<MockHttpServletResponse, FilterChain> {
        val request = MockHttpServletRequest(method, "/api/v1/auth/refresh").apply {
            serverName = "api.techinsights.shop"
            scheme = "https"
            serverPort = 443
            origin?.let { addHeader("Origin", it) }
            referer?.let { addHeader("Referer", it) }
        }
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>()
        every { chain.doFilter(any(), any()) } just Runs
        filter.doFilter(request, response, chain)
        return response to chain
    }

    fun passed(chain: FilterChain) = verify(exactly = 1) { chain.doFilter(any(), any()) }
    fun blocked(chain: FilterChain) = verify(exactly = 0) { chain.doFilter(any(), any()) }

    context("허용된 Origin") {
        test("허용 목록의 Origin 에서 온 POST 는 통과한다") {
            val (response, chain) = run("POST", origin = "https://www.techinsights.shop")
            passed(chain)
            response.status shouldBe 200
        }

        test("대소문자/후행 슬래시 차이는 같은 Origin 으로 본다") {
            val (_, chain) = run("POST", origin = "HTTPS://WWW.TECHINSIGHTS.SHOP/")
            passed(chain)
        }

        test("요청 자신과 같은 Origin(same-origin) 은 통과한다") {
            val (_, chain) = run("POST", origin = "https://api.techinsights.shop")
            passed(chain)
        }
    }

    context("허용되지 않은 Origin") {
        listOf("POST", "PUT", "PATCH", "DELETE").forEach { method ->
            test("허용 목록에 없는 Origin 의 $method 는 403 problem+json 으로 차단한다") {
                val (response, chain) = run(method, origin = "https://evil.example.com")
                blocked(chain)
                response.status shouldBe 403
                response.contentType shouldContain "application/problem+json"
                response.contentAsString shouldContain "CSRF_ORIGIN_NOT_ALLOWED"
            }
        }

        test("같은 사이트의 다른 서브도메인 Origin 도 허용 목록에 없으면 차단한다") {
            val (_, chain) = run("POST", origin = "https://evil.techinsights.shop")
            blocked(chain)
        }

        test("Origin: null (샌드박스 iframe, 리다이렉트 체인) 은 차단한다") {
            val (_, chain) = run("POST", origin = "null")
            blocked(chain)
        }

        test("스킴이나 포트가 다른 Origin 은 차단한다") {
            blocked(run("POST", origin = "http://www.techinsights.shop").second)
            blocked(run("POST", origin = "https://www.techinsights.shop:8443").second)
        }
    }

    context("Referer 대체 검사 (Origin 없음)") {
        test("허용된 Referer 는 통과한다") {
            passed(run("POST", referer = "https://www.techinsights.shop/posts/1").second)
        }

        test("허용되지 않은 Referer 는 차단한다") {
            blocked(run("POST", referer = "https://evil.example.com/page").second)
        }

        test("해석할 수 없는 Referer 는 차단한다") {
            blocked(run("POST", referer = "not a url").second)
        }

        test("Origin 이 있으면 Referer 는 보지 않는다") {
            passed(run("POST", origin = "https://www.techinsights.shop", referer = "https://evil.example.com/").second)
        }
    }

    context("브라우저가 아닌 클라이언트 / 안전한 메서드") {
        test("Origin 과 Referer 가 모두 없는 요청은 통과한다 (브라우저의 ambient 쿠키를 타는 요청이 아님)") {
            passed(run("POST").second)
        }

        listOf("GET", "HEAD", "OPTIONS").forEach { method ->
            test("$method 는 Origin 과 무관하게 통과한다 (CORS 가 별도로 처리)") {
                passed(run(method, origin = "https://evil.example.com").second)
            }
        }
    }
})
